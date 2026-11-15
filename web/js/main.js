import { store, api } from './store.js';
import { PAGES, NAV } from './pages.js';
import { esc, toast, viewPrefs, fmtDateTime, applyTheme } from './ui.js';
import { lock, showLockScreen } from './security.js';
import { hydratePhotos, viewPhoto } from './images.js';

const app = document.getElementById('app');
applyTheme();

// ---------------------------------------------------------------- auth screens
async function authScreen() {
  let status = { needsSetup: false };
  try { status = await api('/api/status'); } catch (e) { /* offline: still show login */ }
  const setup = status.needsSetup;
  app.innerHTML = `<div class="auth">
    <form class="auth-card" novalidate>
      <div class="brand big"><span class="logo">₱</span><div><strong>My Finance</strong><small>Web · synced with your phone</small></div></div>
      <h1>${setup ? 'Create your account' : 'Sign in'}</h1>
      <p class="muted">${setup ? 'First-time setup. Enter the one-time setup code from your server (file <code>server/data/SETUP_CODE.txt</code>).' : 'Use the same account as the Android app\'s Server Sync.'}</p>
      ${setup ? '<label class="field"><span>Setup code</span><input name="setupCode" autocomplete="one-time-code" required placeholder="XXXX-XXXX-XXXX"></label>' : ''}
      <label class="field"><span>Username</span><input name="username" autocomplete="username" required></label>
      <label class="field"><span>Password${setup ? ' (10+ characters)' : ''}</span><input name="password" type="password" autocomplete="${setup ? 'new-password' : 'current-password'}" required></label>
      ${setup ? '<label class="field"><span>Confirm password</span><input name="password2" type="password" autocomplete="new-password" required></label>' : ''}
      <p class="form-error" hidden></p>
      <button class="btn primary block" type="submit">${setup ? 'Create account' : 'Sign in'}</button>
    </form></div>`;
  const form = app.querySelector('form'), err = app.querySelector('.form-error');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(form));
    err.hidden = true;
    if (setup && f.password !== f.password2) { err.textContent = 'Passwords do not match'; err.hidden = false; return; }
    const btn = form.querySelector('button'); btn.disabled = true;
    try {
      const prev = JSON.parse(localStorage.getItem('ff_last_device') || 'null');
      const body = { username: f.username, password: f.password, deviceName: browserName(), kind: 'web',
        ...(prev && prev.username === f.username ? { deviceNo: prev.deviceNo } : {}), ...(setup ? { setupCode: f.setupCode } : {}) };
      const res = await api(setup ? '/api/setup' : '/api/login', { method: 'POST', body });
      store.setAuth({ token: res.token, deviceNo: res.deviceNo, username: res.username });
      localStorage.setItem('ff_last_device', JSON.stringify({ username: res.username, deviceNo: res.deviceNo }));
      await start();
    } catch (ex) {
      err.textContent = ex.message; err.hidden = false; btn.disabled = false;
    }
  });
}
function browserName() {
  const ua = navigator.userAgent;
  const b = /Edg\//.test(ua) ? 'Edge' : /Chrome\//.test(ua) ? 'Chrome' : /Firefox\//.test(ua) ? 'Firefox' : /Safari\//.test(ua) ? 'Safari' : 'Browser';
  const os = /Android/.test(ua) ? 'Android' : /iPhone|iPad/.test(ua) ? 'iOS' : /Windows/.test(ua) ? 'Windows' : /Mac OS/.test(ua) ? 'macOS' : /Linux/.test(ua) ? 'Linux' : '';
  return `${b}${os ? ' on ' + os : ''}`;
}

// ---------------------------------------------------------------- shell
function route() {
  const parts = (location.hash.replace(/^#\/?/, '') || 'dashboard').split('/');
  let name = parts[0], params = parts.slice(1);
  if (name === 'accounts' && params.length) { name = 'account'; }
  return { name: PAGES[name] ? name : 'dashboard', params, navKey: name === 'account' ? 'accounts' : name };
}
function shell() {
  app.innerHTML = `<div class="layout">
    <aside class="side">
      <div class="brand"><span class="logo">₱</span><div><strong>My Finance</strong><small id="who"></small></div></div>
      <nav id="nav">${NAV.map(([k, ico, label]) => `<a href="#/${k}" data-nav="${k}"><span class="ni">${ico}</span>${label}</a>`).join('')}</nav>
      <button class="btn ghost small lock-btn" id="lockBtn" data-action-global="lock" hidden>🔒 Lock now</button>
      <div class="sync-pill" id="syncPill" data-action-global="sync" title="Sync now"></div>
    </aside>
    <main id="main" tabindex="-1"></main>
    <nav class="bottom-nav">${NAV.filter(([k]) => ['dashboard', 'records', 'accounts', 'subscriptions', 'settings'].includes(k)).map(([k, ico, label]) => `<a href="#/${k}" data-nav="${k}"><span>${ico}</span><small>${label}</small></a>`).join('')}
      <button class="fab" data-action-global="add" aria-label="Add transaction">＋</button></nav>
  </div>`;
  document.getElementById('who').textContent = store.auth.username;
}
function render() {
  if (!store.auth) return authScreen();
  const main = document.getElementById('main');
  if (!main) return;
  const lb = document.getElementById('lockBtn'); if (lb) lb.hidden = !lock.enabled;
  if (lock.locked) { main.innerHTML = ''; document.title = 'Locked · My Finance'; return; }
  const r = route(), page = PAGES[r.name];
  // Keep focus/caret inside the records search box while typing.
  const active = document.activeElement, filterKey = active?.dataset?.filter, caret = active?.selectionStart;
  main.innerHTML = page.render(r.params);
  hydratePhotos(main);
  document.title = `${page.title} · My Finance`;
  document.querySelectorAll('[data-nav]').forEach((a) => a.classList.toggle('on', a.dataset.nav === r.navKey));
  if (filterKey) { const el = main.querySelector(`[data-filter="${filterKey}"]`); if (el) { el.focus(); try { el.setSelectionRange(caret, caret); } catch { /* not a text input */ } } }
  renderSyncPill();
}
function renderSyncPill() {
  const pill = document.getElementById('syncPill'); if (!pill) return;
  const s = store.status, pend = store.pending.size;
  pill.className = `sync-pill ${s.syncing ? 'busy' : s.error ? 'err' : 'ok'}`;
  pill.innerHTML = s.syncing ? '⟳ Syncing…' : s.error ? `⚠ ${esc(s.online ? 'Sync error' : 'Offline')}${pend ? ` · ${pend} pending` : ''}`
    : `✓ Synced${s.lastSync ? ' ' + new Date(s.lastSync).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }) : ''}${pend ? ` · ${pend} pending` : ''}`;
  pill.title = s.error ? s.error : s.lastSync ? `Last sync ${fmtDateTime(s.lastSync)} — click to sync now` : 'Click to sync now';
}

// ---------------------------------------------------------------- events (delegated; CSP forbids inline handlers)
document.addEventListener('click', async (e) => {
  const ph = e.target.closest('[data-photo]');
  if (ph) { e.preventDefault(); e.stopPropagation(); viewPhoto(ph.dataset.photo, ph.dataset.title || 'Photo'); return; }
  const g = e.target.closest('[data-action-global]');
  if (g) {
    if (g.dataset.actionGlobal === 'sync') store.sync().then(() => toast('Synced')).catch((x) => toast(x.message, 'err'));
    if (g.dataset.actionGlobal === 'add') (await import('./pages.js')).openTxForm(null);
    if (g.dataset.actionGlobal === 'lock') lock.lockNow();
    return;
  }
  const el = e.target.closest('#main [data-action]');
  if (!el || el.matches('input[type=checkbox]')) return;
  const r = route(), fn = PAGES[r.name].actions?.[el.dataset.action];
  if (fn) { e.preventDefault(); try { await fn(el, r.params); } catch (x) { toast(x.message, 'err'); } }
});
document.addEventListener('change', (e) => {
  const r = route();
  const el = e.target;
  if (el.matches('#main input[type=checkbox][data-action]')) PAGES[r.name].actions?.[el.dataset.action]?.(el, r.params);
  if (el.dataset?.actionChange) PAGES[r.name].changes?.[el.dataset.actionChange]?.(el);
  if (el.dataset?.filter && el.tagName === 'SELECT') { PAGES[r.name].onFilter?.(el.dataset.filter, el.value); render(); }
  if (el.dataset?.filter === 'month') { PAGES[r.name].onFilter?.('month', el.value); render(); }
});
// Drag & drop reordering on the Customize page
let dragId = null;
document.addEventListener('dragstart', (e) => { const r = e.target.closest?.('.cust-row'); if (!r) return; dragId = r.dataset.id; r.classList.add('dragging'); e.dataTransfer.effectAllowed = 'move'; });
document.addEventListener('dragover', (e) => { const r = e.target.closest?.('.cust-row'); if (!r || !dragId) return; e.preventDefault(); document.querySelectorAll('.cust-row.over').forEach((x) => x.classList.remove('over')); r.classList.add('over'); });
document.addEventListener('drop', (e) => { const r = e.target.closest?.('.cust-row'); if (!r || !dragId) return; e.preventDefault(); if (r.dataset.id !== dragId) PAGES.customize.drop(dragId, r.dataset.id); dragId = null; });
document.addEventListener('dragend', () => { dragId = null; document.querySelectorAll('.cust-row').forEach((x) => x.classList.remove('dragging', 'over')); });
let filterTimer;
document.addEventListener('input', (e) => {
  const el = e.target;
  if (el.dataset?.filter === 'q') {
    clearTimeout(filterTimer);
    filterTimer = setTimeout(() => { PAGES[route().name].onFilter?.('q', el.value); render(); }, 180);
  }
});
document.addEventListener('keydown', (e) => {
  if ((e.key === 'Enter' || e.key === ' ') && e.target.matches?.('.tx-row')) { e.preventDefault(); e.target.click(); }
});

// ---------------------------------------------------------------- boot
let syncTimer = null;
let lockShown = false;
async function enforceLock() {
  if (!lock.locked || lockShown) return;
  lockShown = true;
  document.querySelectorAll('.modal-wrap').forEach((m) => m.remove()); // don't leave forms/card details open
  render();
  await showLockScreen(() => store.signOut());
  lockShown = false;
  render();
}
async function start() {
  shell();
  lock.init(enforceLock);
  await store.load();
  applyTheme();
  let queued = false;
  store.subscribe(() => { if (queued) return; queued = true; requestAnimationFrame(() => { queued = false; applyTheme(); render(); }); });
  window.addEventListener('hashchange', () => { render(); document.getElementById('main')?.scrollTo?.(0, 0); window.scrollTo(0, 0); });
  render();
  enforceLock();
  const doSync = () => store.sync().catch((e) => { if (e.status === 401) { toast('Session ended — please sign in again', 'err'); setTimeout(() => location.reload(), 1200); } });
  doSync();
  clearInterval(syncTimer);
  syncTimer = setInterval(() => { if (!document.hidden) doSync(); }, 20000);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) doSync(); });
  window.addEventListener('online', doSync);
  window.addEventListener('beforeunload', () => store.save());
}

if (store.auth) start(); else authScreen();
if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
