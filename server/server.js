#!/usr/bin/env node
/**
 * FreeFinance sync server
 * ------------------------------------------------------------
 * - Zero npm dependencies (Node >= 22.13: node:sqlite, node:crypto, node:http).
 * - Single-owner account; each logged-in client is a "device" with its own number.
 * - Records are stored generically as { tbl, id, data(JSON), deleted, ts, seq }.
 *   `id` is a GLOBAL id: deviceNo * 2^40 + the device's local autoincrement id.
 * - Conflict rule: last write wins by client timestamp `ts`.
 * - Serves the web app from ../web.
 */
'use strict';
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { DatabaseSync } = require('node:sqlite');

const PORT = Number(process.env.FF_PORT || 5006);
const HOST = process.env.FF_HOST || '127.0.0.1';
const DATA_DIR = process.env.FF_DATA_DIR || path.join(__dirname, 'data');
const WEB_DIR = process.env.FF_WEB_DIR || path.join(__dirname, '..', 'web');
const MAX_BODY = 25 * 1024 * 1024;
const PULL_LIMIT = 2000;
const DEVICE_SPAN = 2 ** 40;
const MAX_DEVICES = 8191; // keeps global ids below 2^53 (JS safe integers)

// Tables the Android Room schema syncs, plus "prefs" for shared settings.
const TABLES = new Set([
  'accounts', 'transactions', 'categories', 'budgets', 'debts', 'freelance_projects',
  'milestones', 'exchange_rates', 'receipts_inbox', 'assets', 'planned_payouts',
  'payment_cards', 'prefs',
]);

fs.mkdirSync(DATA_DIR, { recursive: true, mode: 0o700 });
const db = new DatabaseSync(path.join(DATA_DIR, 'freefinance.db'));
db.exec(`
  PRAGMA journal_mode = WAL;
  PRAGMA synchronous = NORMAL;
  CREATE TABLE IF NOT EXISTS meta (k TEXT PRIMARY KEY, v TEXT);
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY, username TEXT UNIQUE NOT NULL, pass_hash TEXT NOT NULL, created_at INTEGER NOT NULL);
  CREATE TABLE IF NOT EXISTS devices (
    device_no INTEGER PRIMARY KEY, name TEXT, kind TEXT, token_hash TEXT,
    created_at INTEGER NOT NULL, last_seen INTEGER, revoked INTEGER NOT NULL DEFAULT 0);
  CREATE TABLE IF NOT EXISTS records (
    tbl TEXT NOT NULL, id INTEGER NOT NULL, data TEXT, deleted INTEGER NOT NULL DEFAULT 0,
    ts INTEGER NOT NULL, seq INTEGER NOT NULL, device_no INTEGER,
    PRIMARY KEY (tbl, id));
  CREATE INDEX IF NOT EXISTS records_seq ON records(seq);
`);

const q = {
  getMeta: db.prepare('SELECT v FROM meta WHERE k = ?'),
  setMeta: db.prepare('INSERT INTO meta(k, v) VALUES(?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v'),
  userCount: db.prepare('SELECT COUNT(*) n FROM users'),
  getUser: db.prepare('SELECT * FROM users WHERE username = ?'),
  firstUser: db.prepare('SELECT * FROM users ORDER BY id LIMIT 1'),
  addUser: db.prepare('INSERT INTO users(username, pass_hash, created_at) VALUES(?, ?, ?)'),
  setPass: db.prepare('UPDATE users SET pass_hash = ? WHERE id = ?'),
  deviceByToken: db.prepare('SELECT * FROM devices WHERE token_hash = ? AND revoked = 0'),
  deviceByNo: db.prepare('SELECT * FROM devices WHERE device_no = ?'),
  maxDevice: db.prepare('SELECT COALESCE(MAX(device_no), 0) m FROM devices'),
  addDevice: db.prepare('INSERT INTO devices(device_no, name, kind, token_hash, created_at, last_seen) VALUES(?, ?, ?, ?, ?, ?)'),
  reuseDevice: db.prepare('UPDATE devices SET name = ?, kind = ?, token_hash = ?, last_seen = ?, revoked = 0 WHERE device_no = ?'),
  touchDevice: db.prepare('UPDATE devices SET last_seen = ? WHERE device_no = ?'),
  listDevices: db.prepare('SELECT device_no, name, kind, created_at, last_seen, revoked FROM devices ORDER BY device_no'),
  revokeDevice: db.prepare('UPDATE devices SET revoked = 1, token_hash = NULL WHERE device_no = ?'),
  getRecord: db.prepare('SELECT ts FROM records WHERE tbl = ? AND id = ?'),
  upsertRecord: db.prepare(`INSERT INTO records(tbl, id, data, deleted, ts, seq, device_no) VALUES(?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(tbl, id) DO UPDATE SET data = excluded.data, deleted = excluded.deleted, ts = excluded.ts,
      seq = excluded.seq, device_no = excluded.device_no`),
  pull: db.prepare('SELECT tbl, id, data, deleted, ts, seq FROM records WHERE seq > ? ORDER BY seq LIMIT ?'),
  liveCount: db.prepare('SELECT COUNT(*) n FROM records WHERE deleted = 0 AND tbl != \'prefs\''),
  maxSeq: db.prepare('SELECT COALESCE(MAX(seq), 0) m FROM records'),
};

// ---------- helpers ----------
const sha256 = (s) => crypto.createHash('sha256').update(s).digest('hex');
function hashPassword(pw) {
  const salt = crypto.randomBytes(16);
  const key = crypto.scryptSync(pw, salt, 64, { N: 16384, r: 8, p: 1 });
  return `scrypt$${salt.toString('hex')}$${key.toString('hex')}`;
}
function verifyPassword(pw, stored) {
  const [alg, saltHex, keyHex] = String(stored).split('$');
  if (alg !== 'scrypt') return false;
  const key = crypto.scryptSync(pw, Buffer.from(saltHex, 'hex'), 64, { N: 16384, r: 8, p: 1 });
  return crypto.timingSafeEqual(key, Buffer.from(keyHex, 'hex'));
}
let seqCounter = q.maxSeq.get().m;

function setupCode() {
  if (q.userCount.get().n > 0) return null;
  let code = q.getMeta.get('setup_code')?.v;
  if (!code) {
    code = crypto.randomBytes(6).toString('hex').toUpperCase().match(/.{4}/g).join('-');
    q.setMeta.run('setup_code', code);
  }
  fs.writeFileSync(path.join(DATA_DIR, 'SETUP_CODE.txt'), code + '\n', { mode: 0o600 });
  return code;
}

// Very small in-memory login throttle: 10 failures / 15 min per client IP.
const failures = new Map();
function clientIp(req) {
  return String(req.headers['cf-connecting-ip'] || (req.headers['x-forwarded-for'] || '').split(',')[0] || req.socket.remoteAddress || '').trim();
}
function throttled(ip) {
  const f = failures.get(ip);
  if (!f) return false;
  if (Date.now() - f.first > 15 * 60e3) { failures.delete(ip); return false; }
  return f.n >= 10;
}
function noteFailure(ip) {
  const f = failures.get(ip);
  if (!f || Date.now() - f.first > 15 * 60e3) failures.set(ip, { n: 1, first: Date.now() });
  else f.n++;
}

function send(res, code, obj, headers = {}) {
  const body = JSON.stringify(obj);
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', ...headers });
  res.end(body);
}
class HttpError extends Error { constructor(code, msg) { super(msg); this.code = code; } }

function readJson(req) {
  return new Promise((resolve, reject) => {
    let size = 0; const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > MAX_BODY) { reject(new HttpError(413, 'Payload too large')); req.destroy(); return; }
      chunks.push(c);
    });
    req.on('end', () => {
      if (!chunks.length) return resolve({});
      try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))); }
      catch { reject(new HttpError(400, 'Invalid JSON')); }
    });
    req.on('error', reject);
  });
}

function auth(req) {
  const m = /^Bearer\s+(\S+)$/i.exec(req.headers.authorization || '');
  if (!m) throw new HttpError(401, 'Not signed in');
  const dev = q.deviceByToken.get(sha256(m[1]));
  if (!dev) throw new HttpError(401, 'Session expired or revoked');
  q.touchDevice.run(Date.now(), dev.device_no);
  return dev;
}

function issueDevice(body) {
  const token = crypto.randomBytes(32).toString('base64url');
  const now = Date.now();
  const name = String(body.deviceName || 'Device').slice(0, 80);
  const kind = ['android', 'web'].includes(body.kind) ? body.kind : 'other';
  let no = Number(body.deviceNo);
  const existing = Number.isInteger(no) && no > 0 ? q.deviceByNo.get(no) : null;
  if (existing) {
    q.reuseDevice.run(name, kind, sha256(token), now, no);
  } else {
    no = q.maxDevice.get().m + 1;
    if (no > MAX_DEVICES) throw new HttpError(409, 'Device limit reached');
    q.addDevice.run(no, name, kind, sha256(token), now, now);
  }
  return { token, deviceNo: no, deviceSpan: DEVICE_SPAN };
}

function validChange(c) {
  return c && TABLES.has(c.tbl) && Number.isSafeInteger(c.id) && c.id > 0 &&
    Number.isSafeInteger(c.ts) && c.ts > 0 &&
    (c.deleted ? true : (c.data && typeof c.data === 'object' && !Array.isArray(c.data)));
}

// ---------- API ----------
async function api(req, res, url) {
  const p = url.pathname;
  const ip = clientIp(req);

  if (p === '/api/health') return send(res, 200, { ok: true });

  if (p === '/api/status' && req.method === 'GET') {
    return send(res, 200, { needsSetup: q.userCount.get().n === 0, serverTime: Date.now(), deviceSpan: DEVICE_SPAN });
  }

  if (p === '/api/setup' && req.method === 'POST') {
    if (q.userCount.get().n > 0) throw new HttpError(409, 'Already set up');
    if (throttled(ip)) throw new HttpError(429, 'Too many attempts, try again later');
    const b = await readJson(req);
    const code = setupCode();
    if (String(b.setupCode || '').trim().toUpperCase() !== code) { noteFailure(ip); throw new HttpError(403, 'Wrong setup code'); }
    const username = String(b.username || '').trim();
    const password = String(b.password || '');
    if (!/^[A-Za-z0-9._@-]{3,64}$/.test(username)) throw new HttpError(400, 'Username: 3–64 letters, digits or . _ @ -');
    if (password.length < 10) throw new HttpError(400, 'Password must be at least 10 characters');
    q.addUser.run(username, hashPassword(password), Date.now());
    q.setMeta.run('setup_code', '');
    fs.rmSync(path.join(DATA_DIR, 'SETUP_CODE.txt'), { force: true });
    return send(res, 200, { username, ...issueDevice(b), serverTime: Date.now() });
  }

  if (p === '/api/login' && req.method === 'POST') {
    if (throttled(ip)) throw new HttpError(429, 'Too many attempts, try again later');
    const b = await readJson(req);
    const user = q.getUser.get(String(b.username || '').trim());
    if (!user || !verifyPassword(String(b.password || ''), user.pass_hash)) {
      noteFailure(ip);
      throw new HttpError(401, 'Wrong username or password');
    }
    failures.delete(ip);
    return send(res, 200, { username: user.username, ...issueDevice(b), serverTime: Date.now() });
  }

  // ---- everything below requires a device token ----
  const dev = auth(req);

  if (p === '/api/me' && req.method === 'GET') {
    return send(res, 200, {
      username: q.firstUser.get()?.username, deviceNo: dev.device_no, deviceSpan: DEVICE_SPAN,
      serverTime: Date.now(), cursor: seqCounter, liveRecords: q.liveCount.get().n,
    });
  }

  if (p === '/api/sync/pull' && req.method === 'GET') {
    const since = Math.max(0, Number(url.searchParams.get('since') || 0) | 0);
    const rows = q.pull.all(since, PULL_LIMIT + 1);
    const more = rows.length > PULL_LIMIT;
    if (more) rows.pop();
    const records = rows.map((r) => ({
      tbl: r.tbl, id: r.id, ts: r.ts, deleted: !!r.deleted, data: r.deleted ? null : JSON.parse(r.data),
    }));
    const cursor = rows.length ? rows[rows.length - 1].seq : since;
    return send(res, 200, { records, cursor, hasMore: more, serverTime: Date.now() });
  }

  if (p === '/api/sync/push' && req.method === 'POST') {
    const b = await readJson(req);
    const changes = Array.isArray(b.changes) ? b.changes : [];
    if (changes.length > 20000) throw new HttpError(413, 'Too many changes in one request');
    let applied = 0; const rejected = []; const invalid = [];
    db.exec('BEGIN IMMEDIATE');
    try {
      for (const c of changes) {
        if (!validChange(c)) { invalid.push({ tbl: c?.tbl, id: c?.id }); continue; }
        const cur = q.getRecord.get(c.tbl, c.id);
        if (cur && cur.ts > c.ts) { rejected.push({ tbl: c.tbl, id: c.id }); continue; } // newer version already stored
        seqCounter += 1;
        q.upsertRecord.run(c.tbl, c.id, c.deleted ? null : JSON.stringify(c.data), c.deleted ? 1 : 0, c.ts, seqCounter, dev.device_no);
        applied++;
      }
      db.exec('COMMIT');
    } catch (e) { db.exec('ROLLBACK'); seqCounter = q.maxSeq.get().m; throw e; }
    return send(res, 200, { applied, rejected, invalid, cursor: seqCounter, serverTime: Date.now() });
  }

  if (p === '/api/devices' && req.method === 'GET') {
    return send(res, 200, { devices: q.listDevices.all(), current: dev.device_no });
  }
  const rm = /^\/api\/devices\/(\d+)$/.exec(p);
  if (rm && req.method === 'DELETE') {
    q.revokeDevice.run(Number(rm[1]));
    return send(res, 200, { ok: true });
  }

  if (p === '/api/password' && req.method === 'POST') {
    const b = await readJson(req);
    const user = q.firstUser.get();
    if (!verifyPassword(String(b.currentPassword || ''), user.pass_hash)) throw new HttpError(403, 'Current password is wrong');
    if (String(b.newPassword || '').length < 10) throw new HttpError(400, 'Password must be at least 10 characters');
    q.setPass.run(hashPassword(String(b.newPassword)), user.id);
    return send(res, 200, { ok: true });
  }

  throw new HttpError(404, 'Not found');
}

// ---------- static web app ----------
const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon',
  '.webmanifest': 'application/manifest+json', '.woff2': 'font/woff2', '.ttf': 'font/ttf',
};
const SECURITY_HEADERS = {
  'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'no-referrer',
  'X-Frame-Options': 'DENY',
  'Content-Security-Policy': "default-src 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; script-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'",
};
function serveStatic(req, res, url) {
  let rel = decodeURIComponent(url.pathname);
  if (rel.endsWith('/')) rel += 'index.html';
  let file = path.normalize(path.join(WEB_DIR, rel));
  if (!file.startsWith(WEB_DIR + path.sep)) { res.writeHead(403); return res.end(); }
  if (!fs.existsSync(file) || !fs.statSync(file).isFile()) file = path.join(WEB_DIR, 'index.html'); // SPA fallback
  const ext = path.extname(file);
  res.writeHead(200, {
    'Content-Type': MIME[ext] || 'application/octet-stream',
    'Cache-Control': ext === '.html' || file.endsWith('sw.js') ? 'no-cache' : 'public, max-age=300',
    ...SECURITY_HEADERS,
  });
  fs.createReadStream(file).pipe(res);
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://local');
  try {
    if (url.pathname.startsWith('/api/')) return await api(req, res, url);
    if (req.method !== 'GET' && req.method !== 'HEAD') throw new HttpError(405, 'Method not allowed');
    return serveStatic(req, res, url);
  } catch (e) {
    const code = e instanceof HttpError ? e.code : 500;
    if (code === 500) console.error(e);
    if (!res.headersSent) send(res, code, { error: code === 500 ? 'Server error' : e.message });
    else res.end();
  }
});

const code = setupCode();
server.listen(PORT, HOST, () => {
  console.log(`FreeFinance sync server on http://${HOST}:${PORT} (data: ${DATA_DIR})`);
  if (code) console.log(`First-run setup code: ${code}  (also in ${path.join(DATA_DIR, 'SETUP_CODE.txt')})`);
});
for (const sig of ['SIGTERM', 'SIGINT']) process.on(sig, () => { server.close(); db.close(); process.exit(0); });
