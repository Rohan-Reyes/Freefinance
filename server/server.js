#!/usr/bin/env node
/**
 * FreeFinance sync server
 * ------------------------------------------------------------
 * - Zero npm dependencies (Node >= 22.13: node:sqlite, node:crypto, node:http).
 * - Multiple accounts (users); each user's data is separate. The first account is created with the
 *   one-time setup code and is the admin; others sign up with the admin's invite code.
 * - Each logged-in client is a "device" with its own number (unique across the server).
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
const MAX_FILE = 15 * 1024 * 1024;
const FILES_DIR = path.join(DATA_DIR, 'files');
const PULL_LIMIT = 2000;
const DEVICE_SPAN = 2 ** 40;
const MAX_DEVICES = 8191; // keeps global ids below 2^53 (JS safe integers)
const MAX_USERS = Math.max(1, Number(process.env.FF_MAX_USERS || 50));
const DOWNLOAD_DIR = path.join(DATA_DIR, 'downloads');
const APK_NAME = 'my-finance.apk';
// Per-account storage limits, so one account cannot fill the server's disk.
const MAX_RECORD_BYTES = 256 * 1024;
const MAX_LIVE_RECORDS = Math.max(1000, Number(process.env.FF_MAX_RECORDS || 200000));
const MAX_FILES_PER_USER = Math.max(10, Number(process.env.FF_MAX_FILES || 3000));

// Tables the Android Room schema syncs, plus "prefs" for shared settings.
const TABLES = new Set([
  'accounts', 'transactions', 'categories', 'budgets', 'debts', 'freelance_projects',
  'milestones', 'exchange_rates', 'receipts_inbox', 'assets', 'planned_payouts',
  'payment_cards', 'prefs',
]);

fs.mkdirSync(DATA_DIR, { recursive: true, mode: 0o700 });
fs.mkdirSync(FILES_DIR, { recursive: true, mode: 0o700 });
const db = new DatabaseSync(path.join(DATA_DIR, 'freefinance.db'));
db.exec(`
  PRAGMA journal_mode = WAL;
  PRAGMA synchronous = NORMAL;
  CREATE TABLE IF NOT EXISTS meta (k TEXT PRIMARY KEY, v TEXT);
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY, username TEXT UNIQUE NOT NULL, pass_hash TEXT NOT NULL, created_at INTEGER NOT NULL);
  CREATE TABLE IF NOT EXISTS devices (
    device_no INTEGER PRIMARY KEY, name TEXT, kind TEXT, token_hash TEXT,
    created_at INTEGER NOT NULL, last_seen INTEGER, revoked INTEGER NOT NULL DEFAULT 0, user_id INTEGER);
`);
fs.mkdirSync(DOWNLOAD_DIR, { recursive: true, mode: 0o755 });
migrateToMultiUser();

/**
 * Schema v2: records and photos belong to a user. A v1 database (single owner) is backed up with
 * VACUUM INTO, then every existing record, device and photo is assigned to the first user.
 */
function migrateToMultiUser() {
  const version = Number(db.prepare("SELECT v FROM meta WHERE k = 'schema'").get()?.v || 1);
  if (version >= 2) return;
  const hasOld = !!db.prepare("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'records'").get();
  const deviceCols = db.prepare('PRAGMA table_info(devices)').all().map((c) => c.name);
  if (hasOld) {
    const backup = path.join(DATA_DIR, `backup-before-multiuser-${Date.now()}.db`);
    db.exec(`VACUUM INTO '${backup.replace(/'/g, "''")}'`);
    console.log(`Schema v1 -> v2: backup written to ${backup}`);
  }
  const owner = db.prepare('SELECT id FROM users ORDER BY id LIMIT 1').get()?.id ?? 1;
  db.exec('BEGIN IMMEDIATE');
  try {
    if (!deviceCols.includes('user_id')) db.exec('ALTER TABLE devices ADD COLUMN user_id INTEGER');
    db.prepare('UPDATE devices SET user_id = ? WHERE user_id IS NULL').run(owner);
    db.exec(`CREATE TABLE IF NOT EXISTS records2 (
      user_id INTEGER NOT NULL, tbl TEXT NOT NULL, id INTEGER NOT NULL, data TEXT, deleted INTEGER NOT NULL DEFAULT 0,
      ts INTEGER NOT NULL, seq INTEGER NOT NULL, device_no INTEGER, PRIMARY KEY (user_id, tbl, id))`);
    if (hasOld) {
      db.prepare('INSERT OR REPLACE INTO records2(user_id, tbl, id, data, deleted, ts, seq, device_no) SELECT ?, tbl, id, data, deleted, ts, seq, device_no FROM records').run(owner);
      db.exec('DROP TABLE records');
    }
    db.exec('ALTER TABLE records2 RENAME TO records');
    db.exec('CREATE INDEX IF NOT EXISTS records_user_seq ON records(user_id, seq)');
    db.exec('CREATE TABLE IF NOT EXISTS file_owners (user_id INTEGER NOT NULL, sha TEXT NOT NULL, PRIMARY KEY (user_id, sha))');
    const own = db.prepare('INSERT OR IGNORE INTO file_owners(user_id, sha) VALUES(?, ?)');
    for (const f of fs.readdirSync(FILES_DIR)) if (/^[a-f0-9]{64}$/.test(f)) own.run(owner, f);
    db.prepare("INSERT INTO meta(k, v) VALUES('schema', '2') ON CONFLICT(k) DO UPDATE SET v = excluded.v").run();
    db.exec('COMMIT');
  } catch (e) {
    db.exec('ROLLBACK');
    throw e;
  }
}

const q = {
  getMeta: db.prepare('SELECT v FROM meta WHERE k = ?'),
  setMeta: db.prepare('INSERT INTO meta(k, v) VALUES(?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v'),
  userCount: db.prepare('SELECT COUNT(*) n FROM users'),
  getUser: db.prepare('SELECT * FROM users WHERE username = ? COLLATE NOCASE'),
  userById: db.prepare('SELECT * FROM users WHERE id = ?'),
  firstUser: db.prepare('SELECT * FROM users ORDER BY id LIMIT 1'),
  addUser: db.prepare('INSERT INTO users(username, pass_hash, created_at) VALUES(?, ?, ?)'),
  setPass: db.prepare('UPDATE users SET pass_hash = ? WHERE id = ?'),
  deviceByToken: db.prepare('SELECT * FROM devices WHERE token_hash = ? AND revoked = 0'),
  deviceByNo: db.prepare('SELECT * FROM devices WHERE device_no = ?'),
  maxDevice: db.prepare('SELECT COALESCE(MAX(device_no), 0) m FROM devices'),
  addDevice: db.prepare('INSERT INTO devices(device_no, name, kind, token_hash, created_at, last_seen, user_id) VALUES(?, ?, ?, ?, ?, ?, ?)'),
  reuseDevice: db.prepare('UPDATE devices SET name = ?, kind = ?, token_hash = ?, last_seen = ?, revoked = 0 WHERE device_no = ?'),
  touchDevice: db.prepare('UPDATE devices SET last_seen = ? WHERE device_no = ?'),
  listDevices: db.prepare('SELECT device_no, name, kind, created_at, last_seen, revoked FROM devices WHERE user_id = ? ORDER BY device_no'),
  revokeDevice: db.prepare('UPDATE devices SET revoked = 1, token_hash = NULL WHERE device_no = ? AND user_id = ?'),
  getRecord: db.prepare('SELECT ts FROM records WHERE user_id = ? AND tbl = ? AND id = ?'),
  upsertRecord: db.prepare(`INSERT INTO records(user_id, tbl, id, data, deleted, ts, seq, device_no) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(user_id, tbl, id) DO UPDATE SET data = excluded.data, deleted = excluded.deleted, ts = excluded.ts,
      seq = excluded.seq, device_no = excluded.device_no`),
  pull: db.prepare('SELECT tbl, id, data, deleted, ts, seq FROM records WHERE user_id = ? AND seq > ? ORDER BY seq LIMIT ?'),
  liveCount: db.prepare('SELECT COUNT(*) n FROM records WHERE user_id = ? AND deleted = 0 AND tbl != \'prefs\''),
  maxSeq: db.prepare('SELECT COALESCE(MAX(seq), 0) m FROM records'),
  ownsFile: db.prepare('SELECT 1 FROM file_owners WHERE user_id = ? AND sha = ?'),
  fileCount: db.prepare('SELECT COUNT(*) n FROM file_owners WHERE user_id = ?'),
  revokeOthers: db.prepare('UPDATE devices SET revoked = 1, token_hash = NULL WHERE user_id = ? AND device_no != ?'),
  addFileOwner: db.prepare('INSERT OR IGNORE INTO file_owners(user_id, sha) VALUES(?, ?)'),
};

// ---------- helpers ----------
const sha256 = (s) => crypto.createHash('sha256').update(s).digest('hex');
// scrypt runs on the libuv thread pool (async) so password checks never block other requests.
const scrypt = (pw, salt) => new Promise((resolve, reject) =>
  crypto.scrypt(pw, salt, 64, { N: 16384, r: 8, p: 1 }, (err, key) => (err ? reject(err) : resolve(key))));
async function hashPassword(pw) {
  const salt = crypto.randomBytes(16);
  const key = await scrypt(pw, salt);
  return `scrypt$${salt.toString('hex')}$${key.toString('hex')}`;
}
// Used when the username does not exist, so a failed login takes the same time either way.
const DUMMY_HASH = `scrypt$${'00'.repeat(16)}$${'00'.repeat(64)}`;
async function verifyPassword(pw, stored) {
  const [alg, saltHex, keyHex] = String(stored || DUMMY_HASH).split('$');
  if (alg !== 'scrypt' || !saltHex || !keyHex) return false;
  const key = await scrypt(pw, Buffer.from(saltHex, 'hex'));
  const want = Buffer.from(keyHex, 'hex');
  return want.length === key.length && crypto.timingSafeEqual(key, want);
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

// Small in-memory throttle: 10 failures / 15 min per client IP, and per username (keys "u:" + name).
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
  if (failures.size > 1000) {
    const cutoff = Date.now() - 15 * 60e3;
    for (const [k, v] of failures) if (v.first <= cutoff) failures.delete(k);
  }
  const f = failures.get(ip);
  if (!f || Date.now() - f.first > 15 * 60e3) failures.set(ip, { n: 1, first: Date.now() });
  else f.n++;
}

function send(res, code, obj, headers = {}) {
  const body = JSON.stringify(obj);
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', ...headers });
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

function issueDevice(body, userId) {
  const token = crypto.randomBytes(32).toString('base64url');
  const now = Date.now();
  const name = String(body.deviceName || 'Device').slice(0, 80);
  const kind = ['android', 'web'].includes(body.kind) ? body.kind : 'other';
  let no = Number(body.deviceNo);
  // A device number is only reused by the same user (it decides the id range of their rows).
  const existing = Number.isInteger(no) && no > 0 ? q.deviceByNo.get(no) : null;
  if (existing && existing.user_id === userId) {
    q.reuseDevice.run(name, kind, sha256(token), now, no);
  } else {
    no = q.maxDevice.get().m + 1;
    if (no > MAX_DEVICES) throw new HttpError(409, 'Device limit reached');
    q.addDevice.run(no, name, kind, sha256(token), now, now, userId);
  }
  return { token, deviceNo: no, deviceSpan: DEVICE_SPAN };
}

function validChange(c) {
  return c && TABLES.has(c.tbl) && Number.isSafeInteger(c.id) && c.id > 0 &&
    Number.isSafeInteger(c.ts) && c.ts > 0 &&
    (c.deleted ? true : (c.data && typeof c.data === 'object' && !Array.isArray(c.data)));
}

function imageType(b) {
  if (b.length >= 3 && b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff) return 'image/jpeg';
  if (b.length >= 8 && b.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) return 'image/png';
  if (b.length >= 12 && b.subarray(0, 4).toString('latin1') === 'RIFF' && b.subarray(8, 12).toString('latin1') === 'WEBP') return 'image/webp';
  if (b.length >= 6 && b.subarray(0, 3).toString('latin1') === 'GIF') return 'image/gif';
  if (b.length >= 12 && b.subarray(4, 8).toString('latin1') === 'ftyp' && /hei|mif|avif/.test(b.subarray(8, 12).toString('latin1'))) return 'image/heic';
  return null;
}
function readRaw(req, limit) {
  return new Promise((resolve, reject) => {
    let size = 0; const chunks = [];
    req.on('data', (c) => { size += c.length; if (size > limit) { reject(new HttpError(413, 'Image too large (max 15 MB)')); req.destroy(); return; } chunks.push(c); });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

// ---------- invites & app downloads ----------
const ENV_INVITE = String(process.env.FF_INVITE_CODE || '').trim();
const inviteCode = () => ENV_INVITE || q.getMeta.get('invite_code')?.v || '';
const newInviteCode = () => crypto.randomBytes(6).toString('base64url').toUpperCase().replace(/[^A-Z0-9]/g, 'X').slice(0, 8);
const USERNAME_RE = /^[A-Za-z0-9._@-]{3,64}$/;
function appInfo() {
  const apk = path.join(DOWNLOAD_DIR, APK_NAME);
  if (!fs.existsSync(apk)) return null;
  let meta = {};
  try { meta = JSON.parse(fs.readFileSync(path.join(DOWNLOAD_DIR, 'version.json'), 'utf8')); } catch { /* optional */ }
  const st = fs.statSync(apk);
  return { url: '/download/' + APK_NAME, size: st.size, updatedAt: st.mtimeMs, versionName: meta.versionName || null,
    versionCode: Number(meta.versionCode) || null, sha256: meta.sha256 || null, minAndroid: meta.minAndroid || '7.0' };
}

// ---------- API ----------
async function api(req, res, url) {
  const p = url.pathname;
  const ip = clientIp(req);

  if (p === '/api/health') return send(res, 200, { ok: true });

  if (p === '/api/status' && req.method === 'GET') {
    const users = q.userCount.get().n;
    return send(res, 200, { needsSetup: users === 0, signupOpen: users > 0 && users < MAX_USERS && !!inviteCode(),
      serverTime: Date.now(), deviceSpan: DEVICE_SPAN, app: appInfo() });
  }

  if (p === '/api/app-version' && req.method === 'GET') {
    return send(res, 200, { app: appInfo() });
  }

  if (p === '/api/signup' && req.method === 'POST') {
    if (throttled(ip)) throw new HttpError(429, 'Too many attempts, try again later');
    const b = await readJson(req);
    const code = inviteCode();
    if (!code || q.userCount.get().n === 0) throw new HttpError(403, 'Sign-up is closed');
    const given = String(b.inviteCode || '').trim().toUpperCase();
    if (given.length !== code.length || !crypto.timingSafeEqual(Buffer.from(given), Buffer.from(code.toUpperCase()))) {
      noteFailure(ip); throw new HttpError(403, 'Invalid invite code');
    }
    if (q.userCount.get().n >= MAX_USERS) throw new HttpError(403, 'This server is full');
    const username = String(b.username || '').trim();
    const password = String(b.password || '');
    if (!USERNAME_RE.test(username)) throw new HttpError(400, 'Username: 3–64 letters, digits or . _ @ -');
    if (password.length < 10) throw new HttpError(400, 'Password must be at least 10 characters');
    if (q.getUser.get(username)) throw new HttpError(409, 'That username is taken');
    const hash = await hashPassword(password);
    if (q.getUser.get(username)) throw new HttpError(409, 'That username is taken');
    if (q.userCount.get().n >= MAX_USERS) throw new HttpError(403, 'This server is full');
    const r = q.addUser.run(username, hash, Date.now());
    return send(res, 200, { username, ...issueDevice(b, Number(r.lastInsertRowid)), serverTime: Date.now() });
  }

  if (p === '/api/setup' && req.method === 'POST') {
    if (q.userCount.get().n > 0) throw new HttpError(409, 'Already set up');
    if (throttled(ip)) throw new HttpError(429, 'Too many attempts, try again later');
    const b = await readJson(req);
    const code = setupCode();
    const givenSetup = Buffer.from(String(b.setupCode || '').trim().toUpperCase());
    if (!code || givenSetup.length !== code.length || !crypto.timingSafeEqual(givenSetup, Buffer.from(code))) { noteFailure(ip); throw new HttpError(403, 'Wrong setup code'); }
    const username = String(b.username || '').trim();
    const password = String(b.password || '');
    if (!USERNAME_RE.test(username)) throw new HttpError(400, 'Username: 3–64 letters, digits or . _ @ -');
    if (password.length < 10) throw new HttpError(400, 'Password must be at least 10 characters');
    const hash = await hashPassword(password);
    if (q.userCount.get().n > 0) throw new HttpError(409, 'Already set up');
    const r = q.addUser.run(username, hash, Date.now());
    q.setMeta.run('setup_code', '');
    fs.rmSync(path.join(DATA_DIR, 'SETUP_CODE.txt'), { force: true });
    return send(res, 200, { username, ...issueDevice(b, Number(r.lastInsertRowid)), serverTime: Date.now() });
  }

  if (p === '/api/login' && req.method === 'POST') {
    if (throttled(ip)) throw new HttpError(429, 'Too many attempts, try again later');
    const b = await readJson(req);
    const uname = String(b.username || '').trim();
    const ukey = 'u:' + uname.toLowerCase();
    if (throttled(ukey)) throw new HttpError(429, 'Too many attempts for this account, try again later');
    const user = q.getUser.get(uname);
    const ok = await verifyPassword(String(b.password || ''), user?.pass_hash);
    if (!user || !ok) {
      noteFailure(ip); noteFailure(ukey);
      throw new HttpError(401, 'Wrong username or password');
    }
    failures.delete(ip); failures.delete(ukey);
    return send(res, 200, { username: user.username, ...issueDevice(b, user.id), serverTime: Date.now() });
  }

  // ---- everything below requires a device token ----
  const dev = auth(req);
  const uid = dev.user_id;
  const me = q.userById.get(uid);
  if (!me) throw new HttpError(401, 'Account no longer exists');
  // The server owner is the first account (the one created with the one-time setup code).
  const isAdmin = me.id === q.firstUser.get()?.id;

  if (p === '/api/me' && req.method === 'GET') {
    return send(res, 200, {
      username: me.username, admin: isAdmin, deviceNo: dev.device_no, deviceSpan: DEVICE_SPAN,
      serverTime: Date.now(), cursor: seqCounter, liveRecords: q.liveCount.get(uid).n,
    });
  }

  if (p === '/api/sync/pull' && req.method === 'GET') {
    const sinceRaw = Math.floor(Number(url.searchParams.get('since') || 0));
    const since = Number.isSafeInteger(sinceRaw) && sinceRaw > 0 ? sinceRaw : 0;
    const rows = q.pull.all(uid, since, PULL_LIMIT + 1);
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
    let live = q.liveCount.get(uid).n;
    db.exec('BEGIN IMMEDIATE');
    try {
      for (const c of changes) {
        if (!validChange(c)) { invalid.push({ tbl: c?.tbl, id: c?.id }); continue; }
        const json = c.deleted ? null : JSON.stringify(c.data);
        if (json && json.length > MAX_RECORD_BYTES) { invalid.push({ tbl: c.tbl, id: c.id }); continue; }
        const cur = q.getRecord.get(uid, c.tbl, c.id);
        if (cur && cur.ts > c.ts) { rejected.push({ tbl: c.tbl, id: c.id }); continue; } // newer version already stored
        if (!cur && !c.deleted && c.tbl !== 'prefs' && ++live > MAX_LIVE_RECORDS) throw new HttpError(507, 'Storage limit reached for this account');
        seqCounter += 1;
        q.upsertRecord.run(uid, c.tbl, c.id, json, c.deleted ? 1 : 0, c.ts, seqCounter, dev.device_no);
        applied++;
      }
      db.exec('COMMIT');
    } catch (e) { db.exec('ROLLBACK'); seqCounter = q.maxSeq.get().m; throw e; }
    return send(res, 200, { applied, rejected, invalid, cursor: seqCounter, serverTime: Date.now() });
  }

  // ---- photo store (receipts / transaction proofs), content-addressed by SHA-256 ----
  const fm = /^\/api\/files\/([a-f0-9]{64})$/.exec(p);
  if (fm) {
    const file = path.join(FILES_DIR, fm[1]);
    if (req.method === 'HEAD' || req.method === 'GET') {
      if (!fs.existsSync(file) || !q.ownsFile.get(uid, fm[1])) { res.writeHead(404, { 'Cache-Control': 'no-store' }); return res.end(); }
      const head = Buffer.alloc(12); const fd = fs.openSync(file, 'r'); fs.readSync(fd, head, 0, 12, 0); fs.closeSync(fd);
      res.writeHead(200, { 'Content-Type': imageType(head) || 'application/octet-stream', 'Content-Length': fs.statSync(file).size,
        'Cache-Control': 'private, max-age=31536000, immutable', 'X-Content-Type-Options': 'nosniff' });
      if (req.method === 'HEAD') return res.end();
      return fs.createReadStream(file).pipe(res);
    }
    if (req.method === 'PUT') {
      const body = await readRaw(req, MAX_FILE);
      if (!imageType(body)) throw new HttpError(415, 'Only JPEG, PNG, WebP, GIF or HEIC images are accepted');
      if (sha256(body) !== fm[1]) throw new HttpError(400, 'Content does not match its SHA-256 name');
      if (!q.ownsFile.get(uid, fm[1]) && q.fileCount.get(uid).n >= MAX_FILES_PER_USER) throw new HttpError(507, 'Photo limit reached for this account');
      if (!fs.existsSync(file)) { fs.writeFileSync(file + '.tmp', body, { mode: 0o600 }); fs.renameSync(file + '.tmp', file); }
      q.addFileOwner.run(uid, fm[1]);
      return send(res, 200, { ok: true, size: body.length });
    }
  }

  if (p === '/api/devices' && req.method === 'GET') {
    return send(res, 200, { devices: q.listDevices.all(uid), current: dev.device_no });
  }
  const rm = /^\/api\/devices\/(\d+)$/.exec(p);
  if (rm && req.method === 'DELETE') {
    q.revokeDevice.run(Number(rm[1]), uid);
    return send(res, 200, { ok: true });
  }

  if (p === '/api/password' && req.method === 'POST') {
    const ukey = 'u:' + me.username.toLowerCase();
    if (throttled(ukey)) throw new HttpError(429, 'Too many attempts, try again later');
    const b = await readJson(req);
    if (!(await verifyPassword(String(b.currentPassword || ''), me.pass_hash))) { noteFailure(ukey); throw new HttpError(403, 'Current password is wrong'); }
    if (String(b.newPassword || '').length < 10) throw new HttpError(400, 'Password must be at least 10 characters');
    q.setPass.run(await hashPassword(String(b.newPassword)), me.id);
    // A new password signs out every other device; they sign in again with the new password.
    const signedOut = q.revokeOthers.run(me.id, dev.device_no).changes;
    return send(res, 200, { ok: true, signedOut });
  }

  // ---- admin: invite code for sign-ups ----
  if (p === '/api/admin/invite') {
    if (!isAdmin) throw new HttpError(403, 'Only the server owner can manage invites');
    if (req.method === 'POST') {
      const b = await readJson(req);
      if (ENV_INVITE) throw new HttpError(409, 'The invite code is set by FF_INVITE_CODE on the server');
      q.setMeta.run('invite_code', b.disable ? '' : newInviteCode());
    }
    return send(res, 200, { inviteCode: inviteCode() || null, fromEnv: !!ENV_INVITE, users: q.userCount.get().n, maxUsers: MAX_USERS });
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
  'Strict-Transport-Security': 'max-age=15552000',
  'Referrer-Policy': 'no-referrer',
  'X-Frame-Options': 'DENY',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
  'Content-Security-Policy': "default-src 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; script-src 'self'; connect-src 'self' https://open.er-api.com; base-uri 'none'; frame-ancestors 'none'",
};
function serveDownload(req, res, url) {
  const apk = path.join(DOWNLOAD_DIR, APK_NAME);
  if (url.pathname !== '/download/' + APK_NAME || !fs.existsSync(apk)) { res.writeHead(404, SECURITY_HEADERS); return res.end('Not found'); }
  res.writeHead(200, {
    ...SECURITY_HEADERS,
    'Content-Type': 'application/vnd.android.package-archive',
    'Content-Length': fs.statSync(apk).size,
    'Content-Disposition': `attachment; filename="${APK_NAME}"`,
    'Cache-Control': 'no-cache',
  });
  if (req.method === 'HEAD') return res.end();
  fs.createReadStream(apk).pipe(res);
}
function serveStatic(req, res, url) {
  let rel;
  try { rel = decodeURIComponent(url.pathname); } catch { throw new HttpError(400, 'Bad URL'); }
  if (rel.endsWith('/')) rel += 'index.html';
  let file = path.normalize(path.join(WEB_DIR, rel));
  if (!file.startsWith(WEB_DIR + path.sep)) { res.writeHead(403); return res.end(); }
  // Only web assets are served, never helper scripts or dotfiles that live next to them.
  const servable = MIME[path.extname(file)] && !path.basename(file).startsWith('.');
  if (!servable || !fs.existsSync(file) || !fs.statSync(file).isFile()) file = path.join(WEB_DIR, 'index.html'); // SPA fallback
  const ext = path.extname(file);
  // Always revalidate (cheap 304s via ETag) so a deploy reaches browsers immediately, even behind
  // Cloudflare, which otherwise stretches short max-age values to hours.
  const st = fs.statSync(file);
  const etag = `W/"${st.size.toString(16)}-${Math.floor(st.mtimeMs).toString(16)}"`;
  const headers = { 'Content-Type': MIME[ext] || 'application/octet-stream', 'Cache-Control': 'no-cache', ETag: etag, ...SECURITY_HEADERS };
  if (req.headers['if-none-match'] === etag) { res.writeHead(304, headers); return res.end(); }
  res.writeHead(200, headers);
  if (req.method === 'HEAD') return res.end();
  fs.createReadStream(file).pipe(res);
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://local');
  try {
    if (url.pathname.startsWith('/api/')) return await api(req, res, url);
    if (url.pathname.startsWith('/download/')) return serveDownload(req, res, url);
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
