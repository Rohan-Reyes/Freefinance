import { store, api } from './store.js?v=19';
import { PAGES, NAV, NAV_GROUPS } from './pages.js?v=19';
import { esc, toast, viewPrefs, fmtDateTime, applyTheme } from './ui.js?v=19';
import { lock, showLockScreen } from './security.js?v=19';
import { hydratePhotos, viewPhoto } from './images.js?v=19';
import { renderLanding } from './landing.js?v=19';
import { svg } from './icons.js?v=19';

const app = document.getElementById('app');
applyTheme();

// ---------------------------------------------------------------- auth screens
async function authScreen() {
  await renderLanding(app, {
    browserName,
    onSignedIn: async (res) => {
      store.setAuth({ token: res.token, deviceNo: res.deviceNo, username: res.username });
      localStorage.setItem('ff_last_device', JSON.stringify({ username: res.username, deviceNo: res.deviceNo }));
      await start();
    },
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
const MOBILE_TABS = ['dashboard', 'records', 'accounts', 'budgets'];
function shell() {
  const navLink = ([k, ico, label]) => `<a href="#/${k}" data-nav="${k}"><span class="ni">${ico}</span><span>${label}</span></a>`;
  app.innerHTML = `<div class="layout">
    <aside class="side">
      <a class="brand" href="#/dashboard"><span class="logo"></span><div><strong>My Finance</strong><small id="who"></small></div></a>
      <nav id="nav">${NAV_GROUPS.map(([title, items]) => `<div class="nav-group"><span class="nav-title">${title}</span>${items.map(navLink).join('')}</div>`).join('')}</nav>
      <div class="side-foot">
        <button class="btn ghost small lock-btn" id="lockBtn" data-action-global="lock" hidden>${svg('lock')}Lock now</button>
        <button class="sync-pill" id="syncPill" data-action-global="sync" title="Sync now"></button>
      </div>
    </aside>
    <header class="topbar"><a class="brand" href="#/dashboard"><span class="logo"></span></a><strong id="topTitle"></strong>
      <button class="sync-dot" id="syncDot" data-action-global="sync" aria-label="Sync now"></button></header>
    <main id="main" tabindex="-1"></main>
    <nav class="bottom-nav">${NAV.filter(([k]) => MOBILE_TABS.includes(k)).map(([k, ico, label]) => `<a href="#/${k}" data-nav="${k}"><span>${ico}</span><small>${label}</small></a>`).join('')}
      <button data-action-global="more" id="moreBtn" aria-label="All sections"><span>${svg('apps')}</span><small>More</small></button></nav>
    <button class="fab" data-action-global="add" aria-label="Add transaction">${svg('add')}</button>
    <div class="sheet-wrap" id="moreSheet" hidden><div class="sheet" role="dialog" aria-label="All sections">
      <div class="sheet-grab"></div>${NAV_GROUPS.map(([title, items]) => `<span class="nav-title">${title}</span><div class="sheet-grid">${items.map(([k, ico, label]) => `<a href="#/${k}" data-nav="${k}">${ico}<small>${label}</small></a>`).join('')}</div>`).join('')}
    </div></div>
  </div>`;
  document.getElementById('who').textContent = store.auth.username;
}
function toggleMore(open) {
  const sh = document.getElementById('moreSheet'); if (!sh) return;
  sh.hidden = !open;
  document.getElementById('moreBtn')?.classList.toggle('on', open);
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
  const tt = document.getElementById('topTitle'); if (tt) tt.textContent = page.title;
  document.getElementById('moreBtn')?.classList.toggle('on', !MOBILE_TABS.includes(r.navKey));
  if (filterKey) { const el = main.querySelector(`[data-filter="${filterKey}"]`); if (el) { el.focus(); try { el.setSelectionRange(caret, caret); } catch { /* not a text input */ } } }
  renderSyncPill();
}
function renderSyncPill() {
  const pill = document.getElementById('syncPill'); if (!pill) return;
  const s = store.status, pend = store.pending.size;
  const state = s.syncing ? 'busy' : s.error ? 'err' : 'ok';
  pill.className = `sync-pill ${state}`;
  pill.innerHTML = s.syncing ? `${svg('sync')}<span>Syncing…</span>` : s.error ? `${svg('warn')}<span>${esc(s.online ? 'Sync error' : 'Offline')}${pend ? ` · ${pend} pending` : ''}</span>`
    : `${svg('check')}<span>Synced${s.lastSync ? ' ' + new Date(s.lastSync).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }) : ''}${pend ? ` · ${pend} pending` : ''}</span>`;
  pill.title = s.error ? s.error : s.lastSync ? `Last sync ${fmtDateTime(s.lastSync)}. Click to sync now.` : 'Click to sync now';
  const dot = document.getElementById('syncDot');
  if (dot) { dot.className = `sync-dot ${state}`; dot.innerHTML = s.error ? svg('warn') : svg('sync'); dot.title = pill.title; }
}

// ---------------------------------------------------------------- events (delegated; CSP forbids inline handlers)
document.addEventListener('click', async (e) => {
  const ph = e.target.closest('[data-photo]');
  if (ph) { e.preventDefault(); e.stopPropagation(); viewPhoto(ph.dataset.photo, ph.dataset.title || 'Photo'); return; }
  const g = e.target.closest('[data-action-global]');
  if (g) {
    if (g.dataset.actionGlobal === 'sync') store.sync().then(() => toast('Synced')).catch((x) => toast(x.message, 'err'));
    if (g.dataset.actionGlobal === 'add') { toggleMore(false); (await import('./pages.js?v=19')).openTxForm(null); }
    if (g.dataset.actionGlobal === 'lock') lock.lockNow();
    if (g.dataset.actionGlobal === 'more') toggleMore(document.getElementById('moreSheet')?.hidden);
    return;
  }
  if (e.target.closest('#moreSheet')) { if (e.target.id === 'moreSheet' || e.target.closest('a')) toggleMore(false); return; }
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
  if (e.key === 'Escape') toggleMore(false);
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
  import('./logic.js?v=19').then((L) => { L.refreshLiveRates(); setInterval(() => L.refreshLiveRates(), 3600 * 1000); });
  // Re-render only when data changed; sync status updates just the sync indicators.
  let renderedVersion = -1, renderedHash = '';
  const schedule = (force = false) => {
    if (queued) return; queued = true;
    requestAnimationFrame(() => {
      queued = false;
      if (!force && store.version === renderedVersion && location.hash === renderedHash) return;
      renderedVersion = store.version; renderedHash = location.hash;
      applyTheme(); render();
    });
  };
  store.subscribe(() => schedule());
  store.onStatus(() => { if (!store.auth) return render(); renderSyncPill(); if (route().name === 'settings') schedule(true); });
  window.addEventListener('hashchange', () => { toggleMore(false); renderedHash = location.hash; render(); document.getElementById('main')?.scrollTo?.(0, 0); window.scrollTo(0, 0); });
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
