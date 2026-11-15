// Public landing page for signed-out visitors: what the app does, Android download, sign in / create account.
import { api } from './store.js';
import { esc } from './ui.js';

const I = {
  sync: '<path d="M12 4V1L8 5l4 4V6a6 6 0 0 1 5.65 8.03l1.46 1.46A8 8 0 0 0 12 4zm-5.65 5.97L4.89 8.51A8 8 0 0 0 12 20v3l4-4-4-4v3a6 6 0 0 1-5.65-8.03z"/>',
  card: '<path d="M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 14H4v-6h16zm0-10H4V6h16z"/>',
  chart: '<path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z"/>',
  bills: '<path d="M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 16H5V9h14zM7 11h5v5H7z"/>',
  lock: '<path d="M18 8h-1V6A5 5 0 0 0 7 6v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zM9 6a3 3 0 0 1 6 0v2H9zm9 14H6V10h12zm-6-3a2 2 0 1 0 0-4 2 2 0 0 0 0 4z"/>',
  receipt: '<path d="M18 17H6v-2h12zm0-4H6v-2h12zm0-4H6V7h12zM3 22l1.5-1.5L6 22l1.5-1.5L9 22l1.5-1.5L12 22l1.5-1.5L15 22l1.5-1.5L18 22l1.5-1.5L21 22V2l-1.5 1.5L18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2 4.5 3.5 3 2z"/>',
  android: '<path d="M17.6 9.48l1.84-3.18a.38.38 0 0 0-.66-.38l-1.86 3.22a11.4 11.4 0 0 0-9.84 0L5.22 5.92a.38.38 0 0 0-.66.38L6.4 9.48A10.8 10.8 0 0 0 1 18h22a10.8 10.8 0 0 0-5.4-8.52zM7 15.25a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5zm10 0a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5z"/>',
  download: '<path d="M19 9h-4V3H9v6H5l7 7zM5 18v2h14v-2z"/>',
};
const svg = (k, cls = '') => `<svg class="ico ${cls}" viewBox="0 0 24 24" aria-hidden="true">${I[k]}</svg>`;
const mb = (n) => `${(n / 1048576).toFixed(1)} MB`;

const FEATURES = [
  ['sync', 'Phone and web, in sync', 'Every change on your phone shows up here within seconds, and the other way round.'],
  ['card', 'Your banks and cards', 'GCash, Maya, MariBank, GoTyme, BDO, BPI and more, each in its own look.'],
  ['chart', 'Where the money goes', 'Spending by category, cash-flow forecast and monthly trends.'],
  ['bills', 'Bills and receivables', 'Recurring bills, debts owed to you and planned payouts with due dates.'],
  ['receipt', 'Receipts', 'Snap receipts and attach proof photos to transactions.'],
  ['lock', 'Private by default', 'Self-hosted. App lock with fingerprint or PIN, and per-account data.'],
];

export async function renderLanding(app, { onSignedIn, browserName }) {
  let status = { needsSetup: false, signupOpen: false, app: null };
  try { status = await api('/api/status'); } catch { /* offline: still show the page */ }
  const a = status.app;
  const isAndroid = /Android/i.test(navigator.userAgent);
  const dlBtn = a
    ? `<a class="btn primary big" href="${esc(a.url)}" download>${svg('download')} Download for Android</a>`
    : '<span class="btn big disabled">Android app coming soon</span>';
  const meta = a ? `<small class="muted">${a.versionName ? 'Version ' + esc(a.versionName) + ' · ' : ''}${mb(a.size)} · Android ${esc(a.minAndroid)}+</small>` : '';

  app.innerHTML = `<div class="landing">
    <header class="ld-nav">
      <div class="brand"><span class="logo">₱</span><strong>My Finance</strong></div>
      <nav><a href="#features">Features</a><a href="#get-app">Get the app</a><a class="btn small" href="#account">Sign in</a></nav>
    </header>

    <section class="ld-hero">
      <div class="ld-copy">
        <h1>Your money, on your phone and the web.</h1>
        <p class="lead">Track accounts, cards, bills and spending in one place. Use the Android app on the go and this website on your computer. Both stay in sync.</p>
        <div class="row">${dlBtn}<a class="btn big" href="#account">${status.signupOpen ? 'Create an account' : 'Sign in'}</a></div>
        ${meta}
      </div>
      <div class="ld-visual" aria-hidden="true">
        <div class="ld-phone">
          <div class="ld-bal"><small>Net worth</small><strong>₱45,959.17</strong></div>
          ${[['G', '#0057E7', 'GCash', '₱2,140.00'], ['M', '#F26A0A', 'MariBank', '₱41,173.00'], ['m', '#0E1A13', 'Maya', '₱500.00']].map(([m, c, n, v]) =>
            `<div class="ld-acc"><span class="brand-mark" style="width:30px;height:30px;background:${c};color:${n === 'Maya' ? '#00D26A' : '#fff'};font-size:15px">${m}</span><span>${n}</span><b>${v}</b></div>`).join('')}
        </div>
      </div>
    </section>

    <section id="features" class="ld-features">
      ${FEATURES.map(([k, t, d]) => `<div class="ld-feat">${svg(k, 'feat')}<h3>${t}</h3><p>${d}</p></div>`).join('')}
    </section>

    <section id="get-app" class="ld-get">
      <div>
        <h2>${svg('android')} Get the Android app</h2>
        <p>Download the APK and open it on your phone. The same link always has the latest version, so use it to update too: install over the old app and your data stays.</p>
        <ol class="ld-steps">
          <li>Tap <strong>Download for Android</strong> on your phone${isAndroid ? '' : ' (or scan this page from your phone)'}.</li>
          <li>Open the file. If Android asks, allow your browser to <em>install unknown apps</em>.</li>
          <li>Open My Finance → Settings → Data &amp; sync → Server sync, and sign in with the same account.</li>
        </ol>
        <div class="row">${dlBtn}</div>
        ${meta}${a?.sha256 ? `<p class="hash"><small class="muted">SHA-256 ${esc(a.sha256)}</small></p>` : ''}
      </div>
    </section>

    <section id="account" class="ld-account">
      <form class="auth-card" novalidate>
        <div class="tabs" role="tablist"></div>
        <h2 class="form-title"></h2>
        <p class="muted form-sub"></p>
        <div class="fields"></div>
        <p class="form-error" hidden></p>
        <button class="btn primary block" type="submit"></button>
      </form>
    </section>

    <footer class="ld-foot"><span>My Finance · self-hosted</span><a href="#account">Sign in</a></footer>
  </div>`;

  const form = app.querySelector('form'), err = form.querySelector('.form-error');
  const modes = status.needsSetup ? ['setup'] : status.signupOpen ? ['signin', 'signup'] : ['signin'];
  let mode = modes[0];
  const field = (name, label, type = 'text', ac = '') => `<label class="field"><span>${label}</span><input name="${name}" type="${type}" ${ac ? `autocomplete="${ac}"` : ''} required></label>`;
  const draw = () => {
    form.querySelector('.tabs').innerHTML = modes.length > 1
      ? modes.map((m) => `<button type="button" class="tab ${m === mode ? 'on' : ''}" data-mode="${m}">${m === 'signin' ? 'Sign in' : 'Create account'}</button>`).join('') : '';
    const t = { setup: ['Set up your server', 'First-time setup: enter the one-time code from server/data/SETUP_CODE.txt.', 'Create account'],
      signin: ['Sign in', 'Use the same account as the Android app.', 'Sign in'],
      signup: ['Create an account', 'You need an invite code from the server owner.', 'Create account'] }[mode];
    form.querySelector('.form-title').textContent = t[0];
    form.querySelector('.form-sub').textContent = t[1];
    form.querySelector('button[type=submit]').textContent = t[2];
    form.querySelector('.fields').innerHTML =
      (mode === 'setup' ? field('setupCode', 'Setup code', 'text', 'one-time-code') : '')
      + (mode === 'signup' ? field('inviteCode', 'Invite code', 'text', 'off') : '')
      + field('username', 'Username', 'text', 'username')
      + field('password', mode === 'signin' ? 'Password' : 'Password (10+ characters)', 'password', mode === 'signin' ? 'current-password' : 'new-password')
      + (mode !== 'signin' ? field('password2', 'Confirm password', 'password', 'new-password') : '');
    err.hidden = true;
  };
  draw();
  form.addEventListener('click', (e) => { const b = e.target.closest('[data-mode]'); if (b) { mode = b.dataset.mode; draw(); } });
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(form));
    err.hidden = true;
    if (mode !== 'signin' && f.password !== f.password2) { err.textContent = 'Passwords do not match'; err.hidden = false; return; }
    const btn = form.querySelector('button[type=submit]'); btn.disabled = true;
    try {
      const prev = JSON.parse(localStorage.getItem('ff_last_device') || 'null');
      const body = { username: f.username, password: f.password, deviceName: browserName(), kind: 'web',
        ...(prev && prev.username === f.username ? { deviceNo: prev.deviceNo } : {}),
        ...(mode === 'setup' ? { setupCode: f.setupCode } : {}), ...(mode === 'signup' ? { inviteCode: f.inviteCode } : {}) };
      const res = await api({ setup: '/api/setup', signin: '/api/login', signup: '/api/signup' }[mode], { method: 'POST', body });
      await onSignedIn(res);
    } catch (ex) {
      err.textContent = ex.message; err.hidden = false; btn.disabled = false;
    }
  });
}
