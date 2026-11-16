// Public landing page (signed-out visitors), styled after aten-systems.site.
// Personal finance manager on Android and the web; local-first with optional sync.
import { api } from './store.js?v=15';
import { esc } from './ui.js?v=15';

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
  download: '<path d="M19 9h-4V3H9v6H5l7 7zM5 18v2h14v-2z"/>',
  mail: '<path d="M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 4-8 5-8-5V6l8 5 8-5z"/>',
  arrow: '<path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/>',
  pie: '<path d="M11 2v20c-5.07-.5-9-4.79-9-10s3.93-9.5 9-10zm2.03 0v8.99H22c-.47-4.74-4.24-8.52-8.97-8.99zm0 11.01V22c4.74-.47 8.5-4.25 8.97-8.99z"/>',
  wallet: '<path d="M21 18v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v1h-9a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2zm-9-2h10V8H12zm4-2.5a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z"/>',
  people: '<path d="M16 11a3 3 0 1 0-3-3 3 3 0 0 0 3 3zm-8 0a3 3 0 1 0-3-3 3 3 0 0 0 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05A4.2 4.2 0 0 1 17 16.5V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>',
  fx: '<path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z"/>',
  check: '<path d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>',
};
const svg = (k, cls = '') => `<svg class="ico ${cls}" viewBox="0 0 24 24" aria-hidden="true">${I[k]}</svg>`;
const mb = (n) => `${(n / 1048576).toFixed(1)} MB`;
const peso = (n) => `${n < 0 ? '−' : ''}₱${Math.abs(n).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const peso0 = (n) => `₱${Math.round(n).toLocaleString('en-PH')}`;

const FEATURES = [
  ['wallet', 'Accounts & net worth'], ['pie', 'Budgets'], ['bills', 'Bills & subscriptions'], ['card', 'Cards & credit'],
  ['people', 'Debts & receivables'], ['chart', 'Reports & forecast'], ['receipt', 'Receipts'], ['fx', 'Multi-currency'],
];

// Demo data for the phone mock-up (illustrative only).
const DEMO = {
  accounts: { label: 'Accounts', total: 116394, rows: [['G', '#0057E7', '#fff', 'GCash', 'E-wallet', 4698], ['M', '#F26A0A', '#fff', 'MariBank', 'Savings', 108500], ['m', '#0E1A13', '#00D26A', 'Maya', 'E-wallet', 1452], ['₱', '#1A4D9C', '#fff', 'Cash', 'Wallet', 1744]] },
  budgets: { label: 'Budgets', total: 5820, rows: null },
  bills: { label: 'Bills', total: 2248, rows: [['N', '#E50914', '#fff', 'Netflix', 'Due in 3 days', -549], ['P', '#F5A020', '#07111F', 'PLDT Fibr', 'Due Nov 20', -1699], ['S', '#34D399', '#07111F', 'Salary', 'In 2 days', 22500]] },
};

function phoneScreen(key) {
  const d = DEMO[key];
  if (key === 'budgets') {
    const rows = [['Food & Dining', 7400, 10000], ['Transport', 1200, 3000], ['Shopping', 2600, 2500], ['Bills', 1911, 4500]];
    return `<div class="lp-screen-title">Budget left this month</div>
      <div class="lp-total" data-count="${d.total}">${peso(0)}</div>
      ${rows.map(([n, spent, lim], i) => { const r = spent / lim; return `<div class="lp-brow" style="--i:${i}"><span>${n}<small>${peso0(spent)} of ${peso0(lim)}</small></span><i><em style="--w:${Math.min(100, r * 100)}%;--c:${r > 1 ? '#FB7185' : r > 0.8 ? '#FBBF24' : '#34D399'}"></em></i></div>`; }).join('')}`;
  }
  return `<div class="lp-screen-title">${key === 'bills' ? 'Due in the next 7 days' : 'Net worth'}</div>
    <div class="lp-total" data-count="${d.total}">${peso(0)}</div>
    ${d.rows.map(([m, bg, fg, name, sub, v], i) => `<div class="lp-row" style="--i:${i}"><span class="lp-mark" style="background:${bg};color:${fg}">${esc(m)}</span><span class="lp-name">${esc(name)}<small>${esc(sub)}</small></span><b class="${v < 0 ? 'neg' : ''}">${peso(v)}</b></div>`).join('')}`;
}

const browserMock = () => `<div class="lp-browser">
  <div class="lp-bbar"><i></i><i></i><i></i><span>budgets.aten-systems.site</span></div>
  <div class="lp-bbody">
    <aside><b>My Finance</b>${['Dashboard', 'Accounts', 'Records', 'Budgets', 'Bills', 'Cards', 'Analytics'].map((n, i) => `<span class="${i ? '' : 'on'}">${n}</span>`).join('')}</aside>
    <div class="lp-bmain">
      <div class="lp-kpis"><div><small>Net worth</small><b>₱116,394</b></div><div><small>Spent this month</small><b class="neg">₱24,180</b></div><div><small>Budget left</small><b class="pos">₱5,820</b></div></div>
      <div class="lp-chart"><small>Monthly spending</small>
        <svg viewBox="0 0 300 90" preserveAspectRatio="none"><path class="area" d="M0,70 C40,60 60,72 90,55 S150,30 180,40 S250,15 300,22 L300,90 L0,90Z"/><path class="line" d="M0,70 C40,60 60,72 90,55 S150,30 180,40 S250,15 300,22"/></svg>
      </div>
      <div class="lp-blist">${[['Food & Dining', 41], ['Shopping', 25], ['Bills & Utilities', 18]].map(([n, v]) => `<div><span>${n}</span><i><em style="--w:${v}%"></em></i><b>${v}%</b></div>`).join('')}</div>
    </div>
  </div>
</div>`;

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
      <nav><a href="#lp-features">Features</a><a href="#lp-plan">Planner</a><a href="#lp-download">Download</a><a href="#lp-contact">Contact</a>
        <a class="lp-btn small" href="#lp-signin" data-auth-mode="signin">Sign in</a>
        <a class="lp-btn small primary" href="#lp-signup" data-auth-mode="signup">Sign up</a></nav>
    </header>

    <section class="lp-hero" id="lp-top">
      <div class="lp-copy">
        <span class="lp-eyebrow">Personal finance manager · Android &amp; web</span>
        <h1>Manage your money, <span class="lp-accent">on your phone</span> or on the web.</h1>
        <p class="lp-lead">Accounts, budgets, bills and cards in one place. Offline on Android, synced to the web when you want.</p>
        <div class="lp-cta">${dlBtn('big')}<a class="lp-btn big ghost" href="#lp-signin">${svg('web')} Open the web app</a></div>
        ${meta}
        <ul class="lp-ticks"><li>${svg('check')} Free</li><li>${svg('check')} Works offline</li><li>${svg('check')} Sync is optional</li></ul>
      </div>
      <div class="lp-stage">
        <div class="lp-devices" role="tablist" aria-label="Preview">
          <button type="button" role="tab" class="on" data-device="phone">${svg('phone')} Phone</button>
          <button type="button" role="tab" data-device="web">${svg('web')} Web</button>
        </div>
        <div class="lp-device-wrap" id="lpDevice" data-device="phone">
          <div class="lp-phone" id="lpPhone">
            <div class="lp-notch"></div>
            <div class="lp-tabs" role="tablist">${Object.entries(DEMO).map(([k, d], i) => `<button type="button" role="tab" class="${i ? '' : 'on'}" data-screen="${k}">${d.label}</button>`).join('')}</div>
            <div class="lp-screen" id="lpScreen">${phoneScreen('accounts')}</div>
          </div>
          ${browserMock()}
        </div>

      </div>
    </section>

    <section class="lp-features" id="lp-features">
      <header class="lp-sec-head reveal"><h2>Everything for your money</h2></header>
      ${FEATURES.map(([k, t], i) => `<article class="lp-feat reveal" style="--d:${(i % 4) * 60}ms">${svg(k, 'feat')}<h3>${t}</h3></article>`).join('')}
    </section>

    <section class="lp-two" id="lp-two">
      <article class="reveal">
        <div class="lp-two-art phone">${svg('phone')}</div><h3>Phone</h3>
        <ul>${[['lock', 'Offline & locked'], ['receipt', 'Snap receipts'], ['chart', 'Home-screen widgets']].map(([k, t]) => `<li>${svg(k)}${t}</li>`).join('')}</ul>
        ${dlBtn()}
      </article>
      <article class="reveal" style="--d:90ms">
        <div class="lp-two-art web">${svg('web')}</div><h3>Web</h3>
        <ul>${[['sync', 'Synced with phone'], ['pie', 'Big-screen reports'], ['download', 'CSV & JSON export']].map(([k, t]) => `<li>${svg(k)}${t}</li>`).join('')}</ul>
        <a class="lp-btn" href="#lp-signin">Open web app ${svg('arrow')}</a>
      </article>
    </section>

    <section class="lp-plan reveal" id="lp-plan">
      <div class="lp-plan-copy">
        <span class="lp-eyebrow">Try it</span>
        <h2>Plan your month</h2>
        <label class="lp-field"><span>Monthly income</span><input id="lpIncome" type="number" inputmode="numeric" min="0" step="500" value="35000"></label>
        <div class="lp-presets" role="radiogroup" aria-label="Split">
          ${[['50/30/20', 50, 30, 20], ['60/20/20', 60, 20, 20], ['70/20/10', 70, 20, 10]].map(([n, a1, b1, c1], i) => `<button type="button" role="radio" class="${i ? '' : 'on'}" data-split="${a1},${b1},${c1}">${n}</button>`).join('')}
        </div>
      </div>
      <div class="lp-plan-out" id="lpPlanOut" aria-live="polite"></div>
    </section>

    <section class="lp-local reveal" id="lp-local">
      <div class="lp-local-copy">
        <h2>Local-first. Sync if you want.</h2>
        <p id="lpModeText"></p>
        <label class="lp-switch"><input type="checkbox" id="lpCloud"><span class="lp-track"><span class="lp-thumb"></span></span><b>Sync phone and web</b></label>
      </div>
      <div class="lp-diagram" id="lpDiagram">
        <div class="lp-node phone">${svg('phone')}<b>Phone</b></div>
        <div class="lp-link l1"><i></i></div>
        <div class="lp-node server">${svg('server')}<b>Server</b></div>
        <div class="lp-link l2"><i></i></div>
        <div class="lp-node web">${svg('web')}<b>Web</b></div>
      </div>
    </section>

    <section class="lp-get reveal" id="lp-download">
      <div>
        <span class="lp-eyebrow">Android</span>
        <h2>Get the app</h2>
        <ol class="lp-steps">
          <li><b>${svg('download')}</b><span>Download</span></li>
          <li><b>${svg('phone')}</b><span>Open &amp; install</span></li>
          <li><b>${svg('sync')}</b><span>Optional: sign in to sync</span></li>
        </ol>
        <div class="lp-cta">${dlBtn()}</div>${meta}
      </div>
    </section>

    <dialog class="lp-modal" id="lp-signin" aria-labelledby="lpAuthTitle">
      <form class="lp-form" novalidate>
        <button type="button" class="lp-close" aria-label="Close">×</button>
        <div class="tabs" role="tablist"></div>
        <h2 class="form-title" id="lpAuthTitle"></h2>
        <p class="form-sub"></p>
        <div class="fields"></div>
        <p class="form-error" hidden></p>
        <button class="lp-btn primary block" type="submit"></button>
      </form>
    </dialog>

    <section class="lp-contact reveal" id="lp-contact">
      <div>
        <span class="lp-eyebrow">Contact</span>
        <h2>Built by ATEN Systems</h2>
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
    const tick = (t) => { const p = dur ? Math.min(1, (t - t0) / dur) : 1; el.textContent = peso(target * (1 - Math.pow(1 - p, 3))); if (p < 1) requestAnimationFrame(tick); };
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
  // Phone / Web preview switch
  const wrap = root.querySelector('#lpDevice');
  if (!reduce) {
    const keys = Object.keys(DEMO);
    timer = setInterval(() => { if (wrap.dataset.device === 'phone') show(keys[(keys.indexOf(current) + 1) % keys.length]); }, 4200);
  }
  root.querySelectorAll('.lp-devices button').forEach((b) => b.addEventListener('click', () => {
    wrap.dataset.device = b.dataset.device;
    root.querySelectorAll('.lp-devices button').forEach((x) => x.classList.toggle('on', x === b));
  }));
  // gentle 3D tilt that follows the pointer
  if (!reduce) {
    const stage = root.querySelector('.lp-stage');
    stage.addEventListener('pointermove', (e) => {
      const r = stage.getBoundingClientRect();
      const x = (e.clientX - r.left) / r.width - 0.5, y = (e.clientY - r.top) / r.height - 0.5;
      wrap.style.transform = `rotateY(${x * 10}deg) rotateX(${-y * 7}deg)`;
    });
    stage.addEventListener('pointerleave', () => { wrap.style.transform = ''; });
  }
  // budget planner
  const income = root.querySelector('#lpIncome'), out = root.querySelector('#lpPlanOut');
  let split = [50, 30, 20];
  const plan = () => {
    const inc = Math.max(0, Number(income.value) || 0);
    const parts = [['Needs', 'Rent, food, bills, transport', '#4D90E8'], ['Wants', 'Eating out, shopping, fun', '#F5A020'], ['Savings', 'Emergency fund, goals, debt', '#34D399']];
    out.innerHTML = `<div class="lp-plan-bar">${parts.map(([, , c], i) => `<i style="--w:${split[i]}%;--c:${c}"></i>`).join('')}</div>
      ${parts.map(([n, , c], i) => `<div class="lp-plan-row"><span class="dot" style="background:${c}"></span><span>${n} <small>${split[i]}%</small></span><b>${peso0(inc * split[i] / 100)}</b></div>`).join('')}
      <p class="lp-plan-note">${svg('chart')} <strong>${peso0(inc * split[2] / 100 * 12)}</strong> saved in a year</p>`;
  };
  income.addEventListener('input', plan);
  root.querySelectorAll('.lp-presets button').forEach((b) => b.addEventListener('click', () => {
    split = b.dataset.split.split(',').map(Number);
    root.querySelectorAll('.lp-presets button').forEach((x) => x.classList.toggle('on', x === b));
    plan();
  }));
  plan();
  // local-first switch
  const sw = root.querySelector('#lpCloud'), diagram = root.querySelector('#lpDiagram'), text = root.querySelector('#lpModeText');
  const mode = () => {
    diagram.classList.toggle('cloud', sw.checked);
    text.textContent = sw.checked
      ? 'Phone, server and web stay in sync within seconds.'
      : 'Everything stays on your phone. No account, no internet needed.';
  };
  sw.addEventListener('change', mode); mode();
  // reveal on scroll
  if (!('IntersectionObserver' in window)) { root.querySelectorAll('.reveal').forEach((el) => el.classList.add('shown')); return; }
  const io = new IntersectionObserver((entries) => entries.forEach((en) => { if (en.isIntersecting) { en.target.classList.add('shown'); io.unobserve(en.target); } }), { threshold: 0.05 });
  root.querySelectorAll('.reveal').forEach((el) => (reduce ? el.classList.add('shown') : io.observe(el)));
}

function wireForm(app, status, { onSignedIn, browserName }) {
  const form = app.querySelector('form'), err = form.querySelector('.form-error');
  const submitBtn = form.querySelector('button[type=submit]');
  const modes = status.needsSetup ? ['setup'] : ['signin', 'signup'];
  let mode = (location.hash === '#lp-signup' || location.hash === '#/signup') && !status.needsSetup ? 'signup' : modes[0];
  const field = (name, label, type = 'text', ac = '', extra = '') =>
    `<label class="lp-field"><span>${label}</span><input name="${name}" type="${type}" ${ac ? `autocomplete="${ac}"` : ''} ${extra} required></label>`;
  const draw = () => {
    form.querySelector('.tabs').innerHTML = modes.length > 1
      ? modes.map((m) => `<button type="button" class="tab ${m === mode ? 'on' : ''}" data-mode="${m}">${m === 'signin' ? 'Sign in' : 'Sign up'}</button>`).join('') : '';
    const closed = mode === 'signup' && !status.signupOpen;
    const t = {
      setup: ['Set up My Finance', 'Enter your one-time setup code to create the first account.', 'Create account'],
      signin: ['Welcome back', 'Sign in to your My Finance account.', 'Sign in'],
      signup: closed
        ? ['Sign-up is invite-only', 'Contact ATEN Systems to request an invite code.', 'Back to sign in']
        : ['Create your account', 'Enter the invite code you were given.', 'Create account'],
    }[mode];
    form.querySelector('.form-title').textContent = t[0];
    form.querySelector('.form-sub').textContent = t[1];
    submitBtn.textContent = t[2];
    submitBtn.hidden = false;
    form.querySelector('.fields').innerHTML = closed ? '' : (
      (mode === 'setup' ? field('setupCode', 'Setup code', 'text', 'one-time-code') : '')
      + (mode === 'signup' ? field('inviteCode', 'Invite code', 'text', 'off') : '')
      + field('username', 'Username', 'text', 'username', 'minlength="3" maxlength="64" pattern="[A-Za-z0-9._@-]{3,64}"')
      + field('password', mode === 'signin' ? 'Password' : 'Password (10+ characters)', 'password', mode === 'signin' ? 'current-password' : 'new-password', mode === 'signin' ? '' : 'minlength="10"')
      + (mode !== 'signin' ? field('password2', 'Confirm password', 'password', 'new-password', 'minlength="10"') : '')
    );
    err.hidden = true;
  };
  draw();
  // Open the dialog from any "Sign in" / "Sign up" / "Open the web app" link.
  const dialog = app.querySelector('#lp-signin');
  const open = (wanted) => {
    if (wanted && modes.includes(wanted)) mode = wanted;
    draw();
    if (!dialog.open) dialog.showModal();
    form.querySelector('input')?.focus();
  };
  app.querySelectorAll('a[href="#lp-signin"], a[href="#lp-signup"]').forEach((a) => a.addEventListener('click', (e) => {
    e.preventDefault();
    open(a.dataset.authMode || (a.getAttribute('href') === '#lp-signup' ? 'signup' : 'signin'));
  }));
  form.querySelector('.lp-close').addEventListener('click', () => dialog.close());
  dialog.addEventListener('click', (e) => { if (e.target === dialog) dialog.close(); }); // click on the backdrop
  if (status.needsSetup || ['#lp-signin', '#lp-signup', '#/signup'].includes(location.hash)) open();
  form.addEventListener('click', (e) => { const b = e.target.closest('[data-mode]'); if (b) { mode = b.dataset.mode; draw(); } });
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (mode === 'signup' && !status.signupOpen) { mode = 'signin'; draw(); return; }
    const f = Object.fromEntries(new FormData(form));
    err.hidden = true;
    const uname = String(f.username || '').trim();
    if (!/^[A-Za-z0-9._@-]{3,64}$/.test(uname)) { err.textContent = 'Username: 3–64 letters, digits, or . _ @ -'; err.hidden = false; return; }
    if (mode !== 'signin' && String(f.password || '').length < 10) { err.textContent = 'Password must be at least 10 characters'; err.hidden = false; return; }
    if (mode !== 'signin' && f.password !== f.password2) { err.textContent = 'Passwords do not match'; err.hidden = false; return; }
    submitBtn.disabled = true;
    try {
      const prev = JSON.parse(localStorage.getItem('ff_last_device') || 'null');
      const body = { username: uname, password: f.password, deviceName: browserName(), kind: 'web',
        ...(prev && prev.username === uname ? { deviceNo: prev.deviceNo } : {}),
        ...(mode === 'setup' ? { setupCode: f.setupCode } : {}), ...(mode === 'signup' ? { inviteCode: f.inviteCode } : {}) };
      const res = await api({ setup: '/api/setup', signin: '/api/login', signup: '/api/signup' }[mode], { method: 'POST', body });
      dialog.close();
      document.documentElement.classList.remove('lp-active');
      history.replaceState(null, '', '#/dashboard'); // landing anchors are not app routes
      await onSignedIn(res);
    } catch (ex) {
      err.textContent = ex.message; err.hidden = false; submitBtn.disabled = false;
    }
  });
}
