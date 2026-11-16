// App lock (4-digit PIN) — mirrors the Android BiometricLockScreen behaviour, per browser.
// The PIN is never stored: only a salted PBKDF2 hash, in this browser's localStorage.
import { viewPrefs, esc, toast } from './ui.js?v=14';

const KEY = 'ff_lock';
const read = () => { try { return JSON.parse(localStorage.getItem(KEY) || 'null'); } catch { return null; } };
const write = (v) => { if (v) localStorage.setItem(KEY, JSON.stringify(v)); else localStorage.removeItem(KEY); };
const TIMEOUTS = { IMMEDIATE: 1500, AFTER_1_MIN: 60000, AFTER_5_MIN: 300000 };
export const TIMEOUT_OPTIONS = [['IMMEDIATE', 'Immediately when I leave the tab'], ['AFTER_1_MIN', 'After 1 minute away'], ['AFTER_5_MIN', 'After 5 minutes away']];
const IDLE_LOCK_MS = 10 * 60000; // browsers stay open for days: also lock after 10 min without activity

async function hashPin(pin, saltHex) {
  const salt = saltHex ? Uint8Array.from(saltHex.match(/../g).map((h) => parseInt(h, 16))) : crypto.getRandomValues(new Uint8Array(16));
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(pin), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits({ name: 'PBKDF2', hash: 'SHA-256', salt, iterations: 210000 }, key, 256);
  const hex = (b) => [...new Uint8Array(b)].map((x) => x.toString(16).padStart(2, '0')).join('');
  return { salt: hex(salt), hash: hex(bits) };
}

export const lock = {
  locked: false,
  hiddenAt: 0,
  lastActivity: Date.now(),
  onChange: () => {},

  get config() { return read(); },
  get enabled() { return !!read()?.hash; },
  get requireForCards() { return !!read()?.cards; },
  get timeoutMode() { return read()?.timeout || 'IMMEDIATE'; },

  async setPin(pin) {
    if (!/^\d{4}$/.test(pin)) throw new Error('PIN must be exactly 4 digits');
    const h = await hashPin(pin);
    write({ ...(read() || { timeout: 'IMMEDIATE', cards: 1 }), ...h, fails: 0, until: 0 });
  },
  disable() { write(null); this.locked = false; this.onChange(); },
  setOption(k, v) { const c = read(); if (c) write({ ...c, [k]: v }); },

  /** Returns true if correct. Applies cooldowns; 10 wrong in a row wipes this browser's session. */
  async verify(pin) {
    const c = read();
    if (!c) return true;
    if (c.until && Date.now() < c.until) throw new Error(`Too many attempts — wait ${Math.ceil((c.until - Date.now()) / 1000)}s`);
    const { hash } = await hashPin(pin, c.salt);
    if (hash === c.hash) { write({ ...c, fails: 0, until: 0 }); return true; }
    const fails = (c.fails || 0) + 1;
    if (fails >= 10) {
      write(null);
      localStorage.removeItem('ff_auth');
      indexedDB.deleteDatabase('freefinance');
      toast('Too many wrong PINs — signed out of this browser', 'err');
      setTimeout(() => location.reload(), 1200);
      throw new Error('Signed out');
    }
    write({ ...c, fails, until: fails >= 5 ? Date.now() + 30000 * (fails - 4) : 0 });
    return false;
  },

  lockNow() { if (this.enabled) { this.locked = true; this.onChange(); } },

  init(onChange) {
    this.onChange = onChange;
    this.locked = this.enabled; // a fresh page load always starts locked
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) this.hiddenAt = Date.now();
      else if (this.enabled && this.hiddenAt && Date.now() - this.hiddenAt >= (TIMEOUTS[this.timeoutMode] ?? 1500)) this.lockNow();
    });
    const bump = () => { this.lastActivity = Date.now(); };
    for (const ev of ['pointerdown', 'keydown', 'wheel', 'touchstart']) document.addEventListener(ev, bump, { passive: true });
    setInterval(() => { if (this.enabled && !this.locked && Date.now() - this.lastActivity > IDLE_LOCK_MS) this.lockNow(); }, 15000);
  },
};

// ---------- PIN pad UI (used for the lock screen and for card reveal) ----------
function padHTML(title, sub) {
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', '⌫'];
  return `<div class="pin-box"><span class="logo">₱</span><h2>${esc(title)}</h2><p class="muted">${esc(sub)}</p>
    <div class="pin-dots">${'<i></i>'.repeat(4)}</div><p class="pin-msg"></p>
    <div class="pin-pad">${keys.map((k) => (k ? `<button type="button" data-k="${k}">${k}</button>` : '<span></span>')).join('')}</div>
    <div class="pin-extra"></div></div>`;
}
/** Shows a PIN pad; resolves true when the correct PIN is entered, false if cancelled. */
export function askPin({ title = 'Enter PIN', sub = 'Enter your 4-digit PIN', overlay = false, cancellable = true, extra = '' } = {}) {
  return new Promise((resolve) => {
    const wrap = document.createElement('div');
    wrap.className = overlay ? 'lock-screen' : 'modal-wrap';
    wrap.innerHTML = padHTML(title, sub);
    const box = wrap.querySelector('.pin-box'), dots = [...wrap.querySelectorAll('.pin-dots i')], msg = wrap.querySelector('.pin-msg');
    if (cancellable) wrap.querySelector('.pin-extra').innerHTML = '<button type="button" class="btn ghost small" data-cancel>Cancel</button>';
    if (extra) wrap.querySelector('.pin-extra').insertAdjacentHTML('beforeend', extra);
    let pin = '', busy = false;
    const paint = () => dots.forEach((d, i) => d.classList.toggle('on', i < pin.length));
    const done = (v) => { document.removeEventListener('keydown', onKey); wrap.remove(); resolve(v); };
    const press = async (k) => {
      if (busy) return;
      if (k === '⌫') { pin = pin.slice(0, -1); paint(); return; }
      if (!/\d/.test(k) || pin.length >= 4) return;
      pin += k; paint();
      if (pin.length < 4) return;
      busy = true;
      try {
        if (await lock.verify(pin)) return done(true);
        msg.textContent = 'Wrong PIN'; box.classList.add('shake'); setTimeout(() => box.classList.remove('shake'), 400);
      } catch (e) { msg.textContent = e.message; }
      pin = ''; paint(); busy = false;
    };
    const onKey = (e) => {
      if (/^\d$/.test(e.key)) press(e.key);
      else if (e.key === 'Backspace') press('⌫');
      else if (e.key === 'Escape' && cancellable) done(false);
    };
    document.addEventListener('keydown', onKey);
    wrap.addEventListener('click', (e) => {
      const k = e.target.closest('[data-k]'); if (k) return press(k.dataset.k);
      if (e.target.closest('[data-cancel]')) done(false);
      if (e.target.closest('[data-forgot]')) done('forgot');
    });
    document.body.appendChild(wrap);
  });
}

/** Full-screen lock; resolves when unlocked. */
export async function showLockScreen(onForgot) {
  for (;;) {
    const r = await askPin({ title: 'My Finance is locked', sub: 'Enter your 4-digit PIN to unlock', overlay: true, cancellable: false,
      extra: '<button type="button" class="btn ghost small" data-forgot>Forgot PIN? Sign out</button>' });
    if (r === true) { lock.locked = false; return; }
    if (r === 'forgot') { await onForgot(); return; }
  }
}

/** Asks for a new PIN twice. Returns the PIN or null. */
export function choosePin() {
  return new Promise((resolve) => {
    const wrap = document.createElement('div');
    wrap.className = 'modal-wrap';
    wrap.innerHTML = padHTML('Set a 4-digit PIN', 'Choose a PIN for this browser');
    wrap.querySelector('.pin-extra').innerHTML = '<button type="button" class="btn ghost small" data-cancel>Cancel</button>';
    const title = wrap.querySelector('h2'), sub = wrap.querySelector('.muted'), msg = wrap.querySelector('.pin-msg');
    const dots = [...wrap.querySelectorAll('.pin-dots i')];
    let pin = '', first = null;
    const paint = () => dots.forEach((d, i) => d.classList.toggle('on', i < pin.length));
    const done = (v) => { document.removeEventListener('keydown', onKey); wrap.remove(); resolve(v); };
    const press = (k) => {
      if (k === '⌫') { pin = pin.slice(0, -1); return paint(); }
      if (!/\d/.test(k) || pin.length >= 4) return;
      pin += k; paint();
      if (pin.length < 4) return;
      if (first == null) { first = pin; pin = ''; title.textContent = 'Confirm your PIN'; sub.textContent = 'Enter the same 4 digits again'; msg.textContent = ''; return setTimeout(paint, 150); }
      if (pin === first) return done(pin);
      msg.textContent = "PINs didn't match — start again"; first = null; pin = ''; title.textContent = 'Set a 4-digit PIN'; setTimeout(paint, 150);
    };
    const onKey = (e) => { if (/^\d$/.test(e.key)) press(e.key); else if (e.key === 'Backspace') press('⌫'); else if (e.key === 'Escape') done(null); };
    document.addEventListener('keydown', onKey);
    wrap.addEventListener('click', (e) => { const k = e.target.closest('[data-k]'); if (k) press(k.dataset.k); if (e.target.closest('[data-cancel]')) done(null); });
    document.body.appendChild(wrap);
  });
}

// Never let the privacy mask be bypassed while locked.
export const isMasked = () => lock.locked || viewPrefs.get('hideMoney', false);
