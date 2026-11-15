// Public landing page (signed-out visitors), styled after aten-systems.site.
// Local-first Android app, optional self-hosted cloud sync + this web app.
import { api } from './store.js?v=8';
import { esc } from './ui.js?v=8';

const MAIN_SITE = 'https://aten-systems.site';
const CONTACT = {
  inquiry: `${MAIN_SITE}/start`,
  security: 'security@aten.systems',
  legal: 'legal@aten.systems',
};

const I = {
  sync: '<path d="M12 4V1L8 5l4 4V6a6 6 0 0 1 5.65 8.03l1.46 1.46A8 8 0 0 0 12 4zm-5.65 5.97L4.89 8.51A8 8 0 0 0 12 20v3l4-4-4-4v3a6 6 0 0 1-5.65-8.03z"/>',
  card: '<path d="M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 14H4v-6h16zm0-10H4V6h16z"/>',
  chart: '<path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z"/>',
  bills: '<path d="M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 16H5V9h14zM7 11h5v5H7z"/>',
  lock: '<path d="M18 8h-1V6A5 5 0 0 0 7 6v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zM9 6a3 3 0 0 1 6 0v2H9zm9 14H6V10h12zm-6-3a2 2 0 1 0 0-4 2 2 0 0 0 0 4z"/>',
  receipt: '<path d="M18 17H6v-2h12zm0-4H6v-2h12zm0-4H6V7h12zM3 22l1.5-1.5L6 22l1.5-1.5L9 22l1.5-1.5L12 22l1.5-1.5L15 22l1.5-1.5L18 22l1.5-1.5L21 22V2l-1.5 1.5L18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2 4.5 3.5 3 2z"/>',
  phone: '<path d="M16 1H8a3 3 0 0 0-3 3v16a3 3 0 0 0 3 3h8a3 3 0 0 0 3-3V4a3 3 0 0 0-3-3zm-4 21a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm5-4H7V4h10z"/>',
  server: '<path d="M4 3h16a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm0 10h16a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-6a1 1 0 0 1 1-1zm3-7.5A1.5 1.5 0 1 0 7 8.5 1.5 1.5 0 0 0 7 5.5zm0 10A1.5 1.5 0 1 0 7 18.5a1.5 1.5 0 0 0 0-3z"/>',
  web: '<path d="M20 3H4a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h6v2H8v2h8v-2h-2v-2h6a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm0 13H4V5h16z"/>',
  android: '<path d="M17.6 9.48l1.84-3.18a.38.38 0 0 0-.66-.38l-1.86 3.22a11.4 11.4 0 0 0-9.84 0L5.22 5.92a.38.38 0 0 0-.66.38L6.4 9.48A10.8 10.8 0 0 0 1 18h22a10.8 10.8 0 0 0-5.4-8.52zM7 15.25a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5zm10 0a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5z"/>',
  download: '<path d="M19 9h-4V3H9v6H5l7 7zM5 18v2h14v-2z"/>',
  mail: '<path d="M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 4-8 5-8-5V6l8 5 8-5z"/>',
  arrow: '<path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/>',
};
const svg = (k, cls = '') => `<svg class="ico ${cls}" viewBox="0 0 24 24" aria-hidden="true">${I[k]}</svg>`;
const mb = (n) => `${(n / 1048576).toFixed(1)} MB`;

const FEATURES = [
  ['phone', 'Works offline, first', 'Everything is stored on your phone. No account, no internet needed to track money.'],
  ['card', 'Your banks and cards', 'GCash, Maya, MariBank, GoTyme, BDO, BPI and more, each with its own look.'],
  ['chart', 'Where the money goes', 'Spending by category, a 30-day cash-flow forecast and monthly trends.'],
  ['bills', 'Bills and receivables', 'Recurring bills, money owed to you and planned payouts, with due dates.'],
  ['receipt', 'Receipts', 'Snap receipts and attach proof photos to transactions.'],
  ['lock', 'Private by design', 'App lock with fingerprint or PIN. Sync, if you turn it on, goes to your own server.'],
];

// Demo data for the phone mock-up (illustrative only).
const DEMO = {
  accounts: { label: 'Accounts', total: 45959.17, rows: [['G', '#0057E7', '#fff', 'GCash', 'E-wallet', 2140], ['M', '#F26A0A', '#fff', 'MariBank', 'Savings', 41173], ['m', '#0E1A13', '#00D26A', 'Maya', 'E-wallet', 500], ['₱', '#1A4D9C', '#fff', 'Cash', 'Wallet', 2146.17]] },
  cards: { label: 'Cards', total: 2, rows: null },
  bills: { label: 'Bills', total: 1450, rows: [['N', '#E50914', '#fff', 'Netflix', 'Due in 3 days', -549], ['P', '#F5A020', '#07111F', 'PLDT Fibr', 'Due Nov 20', -1699], ['A', '#34D399', '#07111F', 'Allowance', 'Tomorrow', 150]] },
};
const peso = (n) => `${n < 0 ? '−' : ''}₱${Math.abs(n).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

function phoneScreen(key) {
  const d = DEMO[key];
  if (key === 'cards') {
    return `<div class="lp-screen-title">Cards</div>
      <div class="lp-card" style="background:linear-gradient(135deg,#2D7BFF,#0057E7 55%,#003BB0)"><b>GCash</b><span>••••  ••••  ••••  2466</span><i class="lp-visa">VISA</i></div>
      <div class="lp-card" style="background:linear-gradient(135deg,#FF8A1E,#F26A0A 55%,#D9480F)"><b>MariBank</b><span>••••  ••••  ••••  0915</span><i class="lp-mc"><em></em><em></em></i></div>`;
  }
  return `<div class="lp-screen-title">${key === 'bills' ? 'Upcoming bills' : 'Net worth'}</div>
    <div class="lp-total" data-count="${d.total}">${peso(0)}</div>
    ${d.rows.map(([m, bg, fg, name, sub, v], i) => `<div class="lp-row" style="--i:${i}"><span class="lp-mark" style="background:${bg};color:${fg}">${esc(m)}</span><span class="lp-name">${esc(name)}<small>${esc(sub)}</small></span><b class="${v < 0 ? 'neg' : ''}">${peso(v)}</b></div>`).join('')}`;
}

export async function renderLanding(app, { onSignedIn, browserName }) {
  let status = { needsSetup: false, signupOpen: false, app: null };
  try { status = await api('/api/status'); } catch { /* offline: still show the page */ }
  const a = status.app;
  const dlBtn = (cls = '') => a
    ? `<a class="lp-btn primary ${cls}" href="${esc(a.url)}" download>${svg('download')} Download for Android</a>`
    : `<span class="lp-btn ${cls} disabled">Android app coming soon</span>`;
  const meta = a ? `<small class="lp-meta">${a.versionName ? 'v' + esc(a.versionName) + ' · ' : ''}${mb(a.size)} · Android ${esc(a.minAndroid)}+ · free</small>` : '';
  document.documentElement.classList.add('lp-active');

  app.innerHTML = `<div class="lp">
    <header class="lp-nav">
      <a class="lp-brand" href="#lp-top"><span class="logo">₱</span><span>My Finance<small>by ATEN Systems</small></span></a>
      <nav><a href="#lp-local">Local-first</a><a href="#lp-features">Features</a><a href="#lp-download">Download</a><a href="#lp-contact">Contact</a>
        <a class="lp-btn small" href="#lp-signin">Sign in</a></nav>
    </header>

    <section class="lp-hero" id="lp-top">
      <div class="lp-copy">
        <span class="lp-eyebrow">Local-first finance app · optional cloud sync</span>
        <h1>Your money stays <span class="lp-accent">on your phone.</span> Sync only if you want to.</h1>
        <p class="lp-lead">My Finance is a local-first Android app for accounts, cards, bills and spending. It works fully offline. Turn on cloud sync to use it on the web and on more devices too, through your own server.</p>
        <div class="lp-cta">${dlBtn('big')}<a class="lp-btn big ghost" href="#lp-signin">${status.signupOpen ? 'Create a cloud account' : 'Sign in to the web app'} ${svg('arrow')}</a></div>
        ${meta}
      </div>
      <div class="lp-stage">
        <div class="lp-phone" id="lpPhone">
          <div class="lp-notch"></div>
          <div class="lp-tabs" role="tablist">${Object.entries(DEMO).map(([k, d], i) => `<button role="tab" class="${i ? '' : 'on'}" data-screen="${k}">${d.label}</button>`).join('')}</div>
          <div class="lp-screen" id="lpScreen">${phoneScreen('accounts')}</div>
        </div>
        <p class="lp-hint">Demo data · tap the tabs</p>
      </div>
    </section>

    <section class="lp-local reveal" id="lp-local">
      <div class="lp-local-copy">
        <span class="lp-eyebrow">How it works</span>
        <h2>Local-first, cloud optional</h2>
        <p id="lpModeText"></p>
        <label class="lp-switch"><input type="checkbox" id="lpCloud"><span class="lp-track"><span class="lp-thumb"></span></span><b>Cloud sync</b></label>
      </div>
      <div class="lp-diagram" id="lpDiagram">
        <div class="lp-node phone">${svg('phone')}<b>Your phone</b><small>All data, always</small></div>
        <div class="lp-link l1"><i></i></div>
        <div class="lp-node server">${svg('server')}<b>Your server</b><small>budgets.aten-systems.site</small></div>
        <div class="lp-link l2"><i></i></div>
        <div class="lp-node web">${svg('web')}<b>Web &amp; other devices</b><small>Same data, in sync</small></div>
      </div>
    </section>

    <section class="lp-features" id="lp-features">
      ${FEATURES.map(([k, t, d], i) => `<article class="lp-feat reveal" style="--d:${i * 70}ms">${svg(k, 'feat')}<h3>${t}</h3><p>${d}</p></article>`).join('')}
    </section>

    <section class="lp-get reveal" id="lp-download">
      <div>
        <span class="lp-eyebrow">Android</span>
        <h2>Get the app</h2>
        <p>The same link always has the latest version. To update, download it again and install over the app; your data stays on your phone.</p>
        <ol class="lp-steps">
          <li><b>1</b><span>Tap <strong>Download for Android</strong> on your phone.</span></li>
          <li><b>2</b><span>Open the file. If Android asks, allow your browser to install unknown apps.</span></li>
          <li><b>3</b><span>Optional: Settings → Data &amp; sync → Server sync to turn on cloud sync.</span></li>
        </ol>
        <div class="lp-cta">${dlBtn()}</div>${meta}
        ${a?.sha256 ? `<p class="lp-hash">SHA-256 ${esc(a.sha256)}</p>` : ''}
      </div>
    </section>

    <section class="lp-faq reveal">
      <h2>Questions</h2>
      <details><summary>Do I need an account?</summary><p>No. The Android app works fully offline with no account. An account is only needed for cloud sync and the web app.</p></details>
      <details><summary>Where is my data stored?</summary><p>On your phone. With cloud sync on, a copy is kept on the sync server so your other devices and the web app stay up to date. Each account's data is kept separate.</p></details>
      <details><summary>How do I get a cloud account?</summary><p>Accounts are invite-only. Ask for an invite code using the contact options below, then create your account here.</p></details>
      <details><summary>Is it free?</summary><p>Yes, the app is free to download and use.</p></details>
    </section>

    <section class="lp-account" id="lp-signin">
      <form class="lp-form" novalidate>
        <div class="tabs" role="tablist"></div>
        <h2 class="form-title"></h2>
        <p class="form-sub"></p>
        <div class="fields"></div>
        <p class="form-error" hidden></p>
        <button class="lp-btn primary block" type="submit"></button>
      </form>
    </section>

    <section class="lp-contact reveal" id="lp-contact">
      <div>
        <span class="lp-eyebrow">Contact</span>
        <h2>Built by ATEN Systems</h2>
        <p>Questions, invite requests or a custom build? Get in touch with the team behind My Finance.</p>
      </div>
      <div class="lp-contact-grid">
        <a class="lp-contact-card" href="${CONTACT.inquiry}" target="_blank" rel="noopener">${svg('arrow')}<b>Send an inquiry</b><small>aten-systems.site/start</small></a>
        <a class="lp-contact-card" href="${MAIN_SITE}" target="_blank" rel="noopener">${svg('web')}<b>Visit ATEN Systems</b><small>aten-systems.site</small></a>
        <a class="lp-contact-card" href="mailto:${CONTACT.security}">${svg('mail')}<b>Privacy &amp; security</b><small>${CONTACT.security}</small></a>
      </div>
    </section>

    <footer class="lp-foot">
      <span>© ${new Date().getFullYear()} <a href="${MAIN_SITE}" target="_blank" rel="noopener">ATEN Systems</a> · My Finance</span>
      <span><a href="${MAIN_SITE}/privacy" target="_blank" rel="noopener">Privacy</a><a href="${MAIN_SITE}/terms" target="_blank" rel="noopener">Terms</a><a href="mailto:${CONTACT.legal}">${CONTACT.legal}</a></span>
    </footer>
  </div>`;

  wireInteractions(app);
  wireForm(app, status, { onSignedIn, browserName });
}

function wireInteractions(root) {
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const screen = root.querySelector('#lpScreen');
  const tabs = [...root.querySelectorAll('.lp-tabs button')];
  const countUp = () => {
    const el = screen.querySelector('[data-count]'); if (!el) return;
    const target = Number(el.dataset.count), t0 = performance.now(), dur = reduce ? 0 : 900;
    const tick = (t) => { const p = dur ? Math.min(1, (t - t0) / dur) : 1; el.textContent = Number.isInteger(target) && target < 10 ? String(Math.round(target * p)) : peso(target * (1 - Math.pow(1 - p, 3))); if (p < 1) requestAnimationFrame(tick); };
    requestAnimationFrame(tick);
  };
  let current = 'accounts', timer = null;
  const show = (key, user = false) => {
    current = key;
    tabs.forEach((b) => b.classList.toggle('on', b.dataset.screen === key));
    screen.classList.remove('in'); void screen.offsetWidth;
    screen.innerHTML = phoneScreen(key); screen.classList.add('in'); countUp();
    if (user) { clearInterval(timer); timer = null; }
  };
  tabs.forEach((b) => b.addEventListener('click', () => show(b.dataset.screen, true)));
  countUp();
  if (!reduce) {
    const keys = Object.keys(DEMO);
    timer = setInterval(() => show(keys[(keys.indexOf(current) + 1) % keys.length]), 4200);
    // gentle 3D tilt that follows the pointer
    const stage = root.querySelector('.lp-stage'), phone = root.querySelector('#lpPhone');
    stage.addEventListener('pointermove', (e) => {
      const r = stage.getBoundingClientRect();
      const x = (e.clientX - r.left) / r.width - 0.5, y = (e.clientY - r.top) / r.height - 0.5;
      phone.style.transform = `rotateY(${x * 14}deg) rotateX(${-y * 10}deg)`;
    });
    stage.addEventListener('pointerleave', () => { phone.style.transform = ''; });
  }
  // local-first switch
  const sw = root.querySelector('#lpCloud'), diagram = root.querySelector('#lpDiagram'), text = root.querySelector('#lpModeText');
  const mode = () => {
    diagram.classList.toggle('cloud', sw.checked);
    text.textContent = sw.checked
      ? 'Cloud sync on: your phone still keeps everything, and also syncs with your own server, so the web app and your other devices stay up to date within seconds.'
      : 'Cloud sync off: everything lives on your phone. It works with no internet and no account, and nothing leaves the device unless you export it.';
  };
  sw.addEventListener('change', mode); mode();
  // reveal on scroll
  if (!('IntersectionObserver' in window)) { root.querySelectorAll('.reveal').forEach((el) => el.classList.add('shown')); return; }
  const io = new IntersectionObserver((entries) => entries.forEach((en) => { if (en.isIntersecting) { en.target.classList.add('shown'); io.unobserve(en.target); } }), { threshold: 0.15 });
  root.querySelectorAll('.reveal').forEach((el) => (reduce ? el.classList.add('shown') : io.observe(el)));
}

function wireForm(app, status, { onSignedIn, browserName }) {
  const form = app.querySelector('form'), err = form.querySelector('.form-error');
  const modes = status.needsSetup ? ['setup'] : status.signupOpen ? ['signin', 'signup'] : ['signin'];
  let mode = modes[0];
  const field = (name, label, type = 'text', ac = '') => `<label class="lp-field"><span>${label}</span><input name="${name}" type="${type}" ${ac ? `autocomplete="${ac}"` : ''} required></label>`;
  const draw = () => {
    form.querySelector('.tabs').innerHTML = modes.length > 1
      ? modes.map((m) => `<button type="button" class="tab ${m === mode ? 'on' : ''}" data-mode="${m}">${m === 'signin' ? 'Sign in' : 'Create account'}</button>`).join('') : '';
    const t = { setup: ['Set up your server', 'First-time setup: enter the one-time code from server/data/SETUP_CODE.txt.', 'Create account'],
      signin: ['Sign in to the web app', 'Use the same account as Server sync in the Android app.', 'Sign in'],
      signup: ['Create a cloud account', 'Accounts are invite-only. Enter the invite code you were given.', 'Create account'] }[mode];
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
      document.documentElement.classList.remove('lp-active');
      history.replaceState(null, '', '#/dashboard'); // landing anchors are not app routes
      await onSignedIn(res);
    } catch (ex) {
      err.textContent = ex.message; err.hidden = false; btn.disabled = false;
    }
  });
}
