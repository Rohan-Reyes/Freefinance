// Page renderers + actions. Each page: { title, render(params) -> html, actions: { name(el, params) } }
import { store } from './store.js?v=18';
import * as L from './logic.js?v=18';
import { lock, askPin, choosePin, TIMEOUT_OPTIONS } from './security.js?v=18';
import { uploadPhoto, hydratePhotos, isSha, exportPhotos, importPhotos } from './images.js?v=18';
import { BANKS, bankFor } from './banks.js?v=18';
import { detectBrand, brandMark, networkMark, logoSha } from './brands.js?v=18';
import { svg } from './icons.js?v=18';
import {
  esc, money, fmtDate, fmtDateTime, argbToCss, toast, modal, confirmDialog, formModal, formHTML, readForm,
  lineChart, barPairs, donut, progressBar, viewPrefs, undo, celebrate, PALETTES, HERO_STYLES, applyTheme, THEME_MODES, themePrefs,
} from './ui.js?v=18';

// ---------------------------------------------------------------- shared helpers
const sortByName = (a, b) => String(a.name || '').localeCompare(String(b.name || ''));
function accountsTree() {
  const accs = store.all('accounts').sort(sortByName);
  const parents = accs.filter((a) => a.parent_account_id == null || !store.get('accounts', a.parent_account_id));
  return parents.flatMap((p) => [{ a: p, depth: 0 }, ...accs.filter((c) => c.parent_account_id === p.id).map((c) => ({ a: c, depth: 1 }))]);
}
const accountOptions = (blank = false) => [...(blank ? [['', '— None —']] : []),
  ...accountsTree().map(({ a, depth }) => [a.id, `${depth ? '↳ ' : ''}${a.name} (${a.currency_code})`])];
function categoryOptions(type, blank = true) {
  const cats = store.all('categories').filter((c) => !type || c.type === type).sort(sortByName);
  const roots = cats.filter((c) => c.parent_category_id == null || !cats.some((p) => p.id === c.parent_category_id));
  return [...(blank ? [['', '— No category —']] : []),
    ...roots.flatMap((r) => [[r.id, r.name], ...cats.filter((c) => c.parent_category_id === r.id).map((c) => [c.id, `   ↳ ${c.name}`])])];
}
const accName = (id) => store.get('accounts', id)?.name || 'Deleted account';
const catOf = (id) => store.get('categories', id);
const catLabel = (id) => { const c = catOf(id); return c ? `${L.icon(c.icon_res_id)} ${esc(c.name)}` : '<span class="muted">Uncategorized</span>'; };
const empty = (text, cta = '') => `<div class="empty">${esc(text)}${cta}</div>`;
const curOpts = () => L.CURRENCIES.map((c) => [c.code, `${c.code} — ${c.name}`]);
const plus = (label, action, extra = '') => `<button class="btn primary" data-action="${action}" ${extra}>${svg('add')}${esc(label)}</button>`;
/** Account logo: real bank/e-wallet mark when recognised, otherwise a coloured initial. */
function accMark(a, size = 34) {
  const b = detectBrand(a.name, a.linked_app_package);
  if (b) return brandMark(b, size);
  const c = argbToCss(a.color);
  return `<span class="brand-mark" style="width:${size}px;height:${size}px;background:${c}22;color:${c};font-size:${Math.round(size * 0.44)}px">${esc((a.name || '?').slice(0, 1).toUpperCase())}</span>`;
}
const prio = (p) => `<i class="prio ${p === 'HIGH' ? 'hi' : p === 'LOW' ? 'lo' : 'mid'}" title="${p === 'HIGH' ? 'High' : p === 'LOW' ? 'Low' : 'Normal'} priority"></i>`;
const kpi = (label, value, cls = '', sub = '') => `<div class="kpi ${cls}"><span>${esc(label)}</span><strong>${value}</strong>${sub ? `<small>${sub}</small>` : ''}</div>`;
const amountClass = (t) => (t.type === 'Income' ? 'pos' : t.type === 'Expense' ? 'neg' : 'xfer');
function txAmount(t, perspectiveAccountId) {
  const cur = store.get('accounts', t.account_id)?.currency_code || L.base();
  if (t.type === 'Transfer') {
    const inbound = perspectiveAccountId != null && t.to_account_id === perspectiveAccountId;
    return money(inbound ? t.amount : -t.amount, cur, { sign: inbound });
  }
  return money(t.type === 'Income' ? t.amount : -t.amount, cur, { sign: true });
}
function txRow(t, perspectiveAccountId) {
  const labels = L.labelsOf(t);
  const title = t.note || catOf(t.category_id)?.name || t.type;
  const where = t.type === 'Transfer' ? `${esc(accName(t.account_id))} → ${esc(accName(t.to_account_id))}` : esc(accName(t.account_id));
  return `<div class="tx-row" data-action="editTx" data-id="${t.id}" tabindex="0">
    <div class="tx-icon ${amountClass(t)}">${t.type === 'Transfer' ? svg('swap') : L.icon(catOf(t.category_id)?.icon_res_id)}</div>
    <div class="tx-main"><strong>${esc(title)}</strong>
      <small>${where} · ${t.category_id ? catLabel(t.category_id) : esc(t.type)} · ${fmtDateTime(t.date)}</small>
      ${labels.length ? `<div class="chips">${labels.map((l) => `<span class="chip">${esc(l)}</span>`).join('')}</div>` : ''}</div>
    <div class="tx-amt ${amountClass(t)}">${txAmount(t, perspectiveAccountId)}
      ${isSha(t.proof_sha256) ? `<button class="badge photo-badge" data-photo="${t.proof_sha256}" data-title="${esc(t.note || 'Proof')}">${svg('attach')}Photo</button>` : t.proof_image_uri ? `<small class="badge" title="Stored on the phone only">${svg('attach')}On phone</small>` : ''}
      ${t.status === 'Planned' ? '<small class="badge warn">Planned</small>' : ''}
      ${t.recurrence_rule && t.recurrence_rule !== 'None' ? `<small class="badge">${esc(t.recurrence_rule)}</small>` : ''}</div>
  </div>`;
}

// ---------------------------------------------------------------- transaction form
function txFields(type, rule) {
  return [
    { key: 'type', label: 'Type', type: 'select', options: [['Expense', 'Expense'], ['Income', 'Income'], ['Transfer', 'Transfer']], half: true },
    { key: 'amount', label: 'Amount', type: 'money', required: true, half: true },
    { key: 'account_id', label: type === 'Transfer' ? 'From account' : 'Account', type: 'select', options: accountOptions(), numeric: true, required: true, half: true },
    { key: 'to_account_id', label: 'To account', type: 'select', options: accountOptions(true), numeric: true, half: true },
    { key: 'category_id', label: 'Category', type: 'select', options: categoryOptions(type === 'Income' ? 'Income' : 'Expense'), numeric: true },
    { key: 'date', label: 'Date & time', type: 'datetime', required: true, half: true },
    { key: 'status', label: 'Status', type: 'select', options: [['Cleared', 'Cleared'], ['Planned', 'Planned']], half: true },
    { key: 'recurrence_rule', label: 'Repeats', type: 'select', options: [...L.RECURRENCE, ...(rule && !L.RECURRENCE.includes(rule) ? [rule] : [])].map((r) => [r, r === 'Monthly:Last' ? 'Monthly (last day)' : r]), half: true },
    { key: 'labels', label: 'Labels', hint: 'Comma separated, e.g. Project X, Reimbursable', half: true },
    { key: 'note', label: 'Note / description', type: 'textarea' },
  ];
}
export async function openTxForm(existing, preset = {}) {
  if (!store.all('accounts').length) { toast('Add an account first', 'warn'); location.hash = '#/accounts'; return; }
  const t = existing || { type: 'Expense', date: Date.now(), status: 'Cleared', recurrence_rule: 'None', account_id: store.all('accounts')[0].id, ...preset };
  const values = { ...t, labels: L.labelsOf(t).join(', ') };
  let proof = isSha(t.proof_sha256) ? t.proof_sha256 : null, proofChanged = false;
  const paintProof = (w) => { const box = w.querySelector('.proof-box'); box.innerHTML = proof ? `<img data-sha="${proof}" alt="Proof photo"><button type="button" class="btn small ghost danger" data-proof-remove>Remove photo</button>` : (t.proof_image_uri && !proofChanged ? `<small class="muted">${svg('attach')} A photo is attached on the phone (not uploaded yet).</small>` : '<small class="muted">No photo attached.</small>'); hydratePhotos(box); };
  const toggle = (wrap) => {
    const type = wrap.querySelector('[name=type]').value;
    wrap.querySelector('[name=to_account_id]').closest('.field').style.display = type === 'Transfer' ? '' : 'none';
    wrap.querySelector('[name=category_id]').closest('.field').style.display = type === 'Transfer' ? 'none' : '';
    const sel = wrap.querySelector('[name=category_id]'), keep = sel.value;
    sel.innerHTML = categoryOptions(type === 'Income' ? 'Income' : 'Expense').map(([v, l]) => `<option value="${esc(v)}" ${String(v) === keep ? 'selected' : ''}>${esc(l)}</option>`).join('');
  };
  await formModal(existing ? 'Edit transaction' : 'New transaction', txFields(t.type, t.recurrence_rule), values, {
    wide: true,
    onOpen: (w) => {
      toggle(w); w.querySelector('[name=type]').addEventListener('change', () => toggle(w));
      w.querySelector('.form').insertAdjacentHTML('beforeend', `<div class="field"><span>Proof photo / receipt</span><div class="proof-box"></div>
        <label class="btn small">${svg('camera')}Attach photo<input type="file" accept="image/*" capture="environment" hidden data-proof-file></label></div>`);
      paintProof(w);
      w.querySelector('[data-proof-file]').addEventListener('change', async (e) => {
        const f = e.target.files?.[0]; if (!f) return;
        const box = w.querySelector('.proof-box'); box.innerHTML = '<small class="muted">Uploading…</small>';
        try { proof = await uploadPhoto(f); proofChanged = true; } catch (x) { toast(x.message, 'err'); }
        paintProof(w);
      });
      w.querySelector('.proof-box').addEventListener('click', (e) => { if (e.target.closest('[data-proof-remove]')) { proof = null; proofChanged = true; paintProof(w); } });
    },
    extraActions: existing ? [{ label: 'Delete', kind: 'danger ghost', onClick: async () => {
      if (!(await confirmDialog('Delete transaction?', 'You can undo this right after.'))) return false;
      const snapshot = { ...existing };
      store.remove('transactions', existing.id);
      undo.set(`Deleted: ${existing.note || existing.type}`, () => store.put('transactions', snapshot));
    } }] : [],
    onSubmit: (v) => {
      if (v.type === 'Transfer') {
        if (!v.to_account_id) throw new Error('Choose the destination account');
        if (v.to_account_id === v.account_id) throw new Error('From and To accounts must differ');
        v.category_id = null;
      } else v.to_account_id = null;
      v.labels = JSON.stringify((v.labels || '').split(',').map((s) => s.trim()).filter(Boolean));
      if (proofChanged) { v.proof_sha256 = proof; if (!proof) v.proof_image_uri = null; }
      if (existing) {
        const snapshot = { ...existing };
        store.update('transactions', existing.id, v);
        undo.set('Transaction updated', () => store.put('transactions', snapshot));
      } else {
        const row = L.addTransaction(v);
        const cur = store.get('accounts', row.account_id)?.currency_code || L.base();
        celebrate(row.note || catOf(row.category_id)?.name || row.type, money(row.type === 'Income' ? row.amount : -row.amount, cur, { sign: true, hideable: false }), row.type === 'Income' ? 'pos' : row.type === 'Expense' ? 'neg' : 'xfer');
        undo.set(`Added: ${row.note || row.type}`, () => store.remove('transactions', row.id));
      }
    },
  });
}

// ---------------------------------------------------------------- DASHBOARD (modular cards, DashboardCardType)
const monthStart = () => { const d = new Date(); d.setDate(1); d.setHours(0, 0, 0, 0); return +d; };
const DASH_CARDS = [
  ['TOTAL_BALANCE', 'Total Balance', 'Total liquid balance and active account count overview', () => {
    const accs = store.all('accounts').filter((a) => a.parent_account_id == null).sort((a, b) => L.convert(L.balanceOf(b), b.currency_code, L.base()) - L.convert(L.balanceOf(a), a.currency_code, L.base()));
    return `<h3>Accounts <a href="#/accounts" class="more">All</a></h3>
      ${accs.slice(0, 6).map((a) => `<a class="mini-row link acc-mini" href="#/accounts/${a.id}"><span>${accMark(a, 28)}${esc(a.name)}</span><strong class="${L.balanceOf(a) < 0 ? 'neg' : ''}">${money(L.balanceOf(a), a.currency_code)}</strong></a>`).join('') || empty('No accounts configured')}`; }],
  ['MONEY_FLOW', 'Money Flow', 'Income vs expense breakdown and net cash flow gauge', () => {
    const all = L.moneyFlow(), m = L.moneyFlow(monthStart()), tot = all.income + all.expense;
    return `<h3>Money flow breakdown</h3><div class="split two"><i style="width:${tot ? (all.income / tot) * 100 : 50}%"></i></div>
      <div class="kpis">${kpi('Income', money(all.income), 'pos')}${kpi('Expenses', money(all.expense), 'neg')}${kpi('Net cash balance', money(all.net, L.base(), { sign: true }), all.net >= 0 ? 'pos' : 'neg')}</div>
      <small class="muted">All time · this month: ${money(m.income)} in, ${money(m.expense)} out</small>`; }],
  ['BUDGET_PROGRESS', 'Budget Consumption', 'Progress against active monthly budgets', () => {
    const b = L.budgetProgress().slice(0, 4);
    return `<h3>Monthly budget health <a href="#/budgets" class="more">All</a></h3>${b.length ? b.map((x) => `<div class="budget-line"><div><strong>${L.icon(x.category.icon_res_id)} ${esc(x.category.name)}</strong>
      <small>${money(x.spent)} of ${money(x.budget.amount_limit)}</small></div>${progressBar(x.ratio)}</div>`).join('') : empty('No budgets yet')}`; }],
  ['DEBT_SUMMARY', 'Receivables & Debts', 'Top receivables summary of who owes you money', () => {
    const act = store.all('debts').filter((d) => d.status === 'Active'), lent = act.filter((d) => d.type === 'Lent'), bor = act.filter((d) => d.type === 'Borrowed');
    return `<h3>Receivables (owed to me) <a href="#/debts" class="more">All</a></h3>
      <div class="kpis">${kpi('Owed to you', money(lent.reduce((s, d) => s + d.remaining_amount, 0)), 'pos')}${kpi('You owe', money(bor.reduce((s, d) => s + d.remaining_amount, 0)), 'neg')}</div>
      ${lent.sort((a, b) => b.remaining_amount - a.remaining_amount).slice(0, 3).map((d) => `<div class="mini-row"><span>${esc(d.person_name)}${d.due_date ? `<br><small class="muted">due ${fmtDate(d.due_date)}</small>` : ''}</span><strong>${money(d.remaining_amount)}</strong></div>`).join('') || empty('No outstanding money owed to you.')}`; }],
  ['LIQUID_VS_LOCKED', 'Liquid vs Locked', 'Spendable liquid funds vs locked savings and time-deposits', () => {
    const ll = L.liquidVsLocked();
    return `<h3>Liquid vs locked funds</h3><div class="split"><i style="width:${ll.ratio * 100}%"></i></div>
      <div class="kpis">${kpi('Spendable liquid', money(ll.spendable))}${kpi('Locked / vaults', money(ll.locked))}</div><small class="muted">${Math.round(ll.ratio * 100)}% of your money is spendable</small>`; }],
  ['FORECAST_30', '30-Day Forecast', 'Projected balance over the next 30 days (planned, recurring, milestones, interest)', () => {
    const fc = L.forecast(30, viewPrefs.get('includeAssets', false));
    return `<h3>30-day cash-flow forecast</h3>${lineChart(fc.map((d) => ({ y: d.balance, label: fmtDate(d.date, { month: 'short', day: 'numeric' }) })), { fmt: (v) => money(v) })}
      <div class="kpis">${kpi('Today', money(fc[0]?.balance ?? 0))}${kpi('In 30 days', money(fc[fc.length - 1]?.balance ?? 0))}</div>`; }, true],
  ['PROJECT_TRACKER', 'Project / Label Tracker', 'Net balance and performance for tagged initiatives', () => {
    const map = new Map();
    for (const t of store.all('transactions')) for (const l of L.labelsOf(t)) { const e = map.get(l) || { net: 0, n: 0, inc: 0, exp: 0 }; if (t.type === 'Income') { e.net += t.amount; e.inc += t.amount; } else if (t.type === 'Expense') { e.net -= t.amount; e.exp += t.amount; } e.n++; map.set(l, e); }
    const rows = [...map.entries()].sort((a, b) => b[1].n - a[1].n).slice(0, 5);
    return `<h3>Tagged initiative performance</h3>${rows.length ? rows.map(([l, e]) => `<div class="mini-row"><span><span class="chip">#${esc(l)}</span> <small class="muted">${e.n} entr${e.n === 1 ? 'y' : 'ies'}</small></span><strong class="${e.net >= 0 ? 'pos' : 'neg'}">${money(e.net, L.base(), { sign: true })}</strong></div>`).join('') : empty('Add labels to transactions to track projects')}`; }],
  ['LONG_TERM_EXPECTED', 'Long-Term Expected', 'Delayed receivables, planned inflows and pending milestones', () => {
    const list = [
      ...store.all('debts').filter((d) => d.type === 'Lent' && d.status === 'Active').map((d) => ({ title: `Receivable from ${d.person_name}`, amount: d.remaining_amount, date: d.due_date || Date.now(), src: d.person_name })),
      ...store.all('milestones').filter((m) => m.status === 'Pending').map((m) => ({ title: `Milestone: ${m.title}`, amount: m.amount, date: m.expected_date, src: 'Milestone' })),
      ...store.all('transactions').filter((t) => t.type === 'Income' && (t.status === 'Planned' || (t.recurrence_rule && t.recurrence_rule !== 'None'))).map((t) => ({ title: t.note || 'Expected Inflow', amount: t.amount, date: t.recurrence_rule !== 'None' ? L.nextDueDate(t.date, t.recurrence_rule) : t.date, src: 'Scheduled' })),
    ].sort((a, b) => a.date - b.date);
    return `<h3>Long-term expected inflows</h3>${list.length ? `<p class="muted">Total ${money(list.reduce((s, x) => s + x.amount, 0))}</p>` + list.slice(0, 5).map((x) => `<div class="mini-row"><span>${esc(x.title)}<br><small class="muted">${esc(x.src)} · ${fmtDate(x.date)}</small></span><strong class="pos">${money(x.amount)}</strong></div>`).join('') : empty('No scheduled delayed inflows or long-term receivables.')}`; }],
  ['WEEKLY_FORECAST', '7-Day Weekly Sparkline', 'Short-term projected balance for the coming week', () => {
    const fc = L.forecast(7, viewPrefs.get('includeAssets', false)), d = (fc[6]?.balance ?? 0) - (fc[0]?.balance ?? 0);
    return `<h3>7-day outlook</h3>${lineChart(fc.map((x) => ({ y: x.balance, label: fmtDate(x.date, { weekday: 'short' }) })), { height: 90, fmt: (v) => money(v) })}
      <div class="kpis">${kpi('In 7 days', money(fc[6]?.balance ?? 0))}${kpi('Change', money(d, L.base(), { sign: true }), d >= 0 ? 'pos' : 'neg')}</div>`; }],
  ['UPCOMING_BILLS', 'Upcoming Bills & Subs', 'Next scheduled recurring bills and subscriptions', () => {
    const bills = L.recurringTemplates().filter((t) => t.type === 'Expense').map((t) => ({ t, next: L.nextDueDate(t.date, t.recurrence_rule) })).sort((a, b) => a.next - b.next);
    return `<h3>Upcoming bills & subs <a href="#/subscriptions" class="more">All</a></h3>${bills.length ? `<p class="muted">Upcoming scheduled: ${money(bills.reduce((s, x) => s + L.monthlyEquivalent(x.t.amount, x.t.recurrence_rule), 0))}/mo</p>` + bills.slice(0, 5).map(({ t, next }) => `<div class="mini-row"><span>${esc(t.note || 'Bill')}<br><small class="muted">${fmtDate(next)} · ${esc(L.scheduleLabel(t.recurrence_rule, t.date))}</small></span><strong class="neg">${money(-t.amount, L.base(), { sign: true })}</strong></div>`).join('') : empty('No recurring bills or subscriptions scheduled.')}`; }],
  ['SPENDING_CATEGORIES', 'Top Spending Categories', 'Where your money went this month', () => {
    const cats = L.spendingByCategory(monthStart(), Date.now()).slice(0, 5), max = cats[0]?.total || 1;
    return `<h3>Top spending categories</h3>${cats.length ? cats.map((c) => `<div class="budget-line"><div><strong>${L.icon(c.category?.icon_res_id)} ${esc(c.category?.name || 'Uncategorized')}</strong><small>${money(c.total)}</small></div>${progressBar(c.total / max, 2, { neutral: true })}</div>`).join('') : empty('No category expense records recorded yet.')}`; }],
  ['SAVINGS_RATE', 'Savings Rate Gauge', "This month's share of income kept", () => {
    const f = L.moneyFlow(monthStart()), r = f.income > 0 ? Math.max(0, (f.net / f.income) * 100) : 0;
    return `<h3>Current savings rate</h3><div class="gauge-row"><div class="score-ring small ${r >= 20 ? '' : r >= 10 ? 'warn' : 'neg'}" style="--p:${Math.min(100, r)}"><span>${Math.round(r)}%</span></div>
      <div><p>${money(f.net, L.base(), { sign: true })} saved of ${money(f.income)} income</p><small class="muted">Target: 20% or more</small></div></div>`; }],
  ['SPENDING_TRENDS', 'Monthly Spending Trends', 'Six months of income vs expenses', () => `<h3>Monthly spending trends</h3>${barPairs(L.monthlyTrends(6), { fmt: (v) => money(v) })}`, true],
  ['PLANNED_PAYOUTS', 'Planned Payouts', 'Scheduled one-off commitments', () => {
    const p = store.all('planned_payouts').filter((x) => x.status === 'PENDING').sort((a, b) => a.due_date - b.due_date);
    return `<h3>Planned payouts <a href="#/subscriptions" class="more">All</a></h3>${p.length ? p.slice(0, 5).map((x) => `<div class="mini-row"><span>${prio(x.priority)} ${esc(x.title)}<br><small class="muted">due ${fmtDate(x.due_date)}${x.due_date < Date.now() ? ' · <b class="neg">overdue</b>' : ''}</small></span><strong>${money(x.amount, x.currency_code)}</strong></div>`).join('') : empty("No pending planned payouts. You're all settled!")}`; }],
  ['RECENT', 'Recent Transactions', 'Your latest ledger entries', () => {
    const recent = store.all('transactions').sort((a, b) => b.date - a.date || b.id - a.id).slice(0, 8);
    return `<h3>Recent transactions <a href="#/records" class="more">All</a></h3>${recent.length ? recent.map((t) => txRow(t)).join('') : empty('No Transactions Logged Yet', ` ${plus('Add one', 'quick', 'data-type="Expense"')}`)}`; }, true],
];
function dashLayout() {
  const saved = viewPrefs.get('dashCards', null), ids = DASH_CARDS.map((c) => c[0]);
  const order = [...(saved?.order || []).filter((id) => ids.includes(id)), ...ids.filter((id) => !(saved?.order || []).includes(id))];
  return { order, hidden: new Set(saved?.hidden || []) };
}
const dashboard = {
  title: 'Dashboard',
  render() {
    const inclAssets = viewPrefs.get('includeAssets', false), nw = L.netWorth(inclAssets), b = L.base();
    const { order, hidden } = dashLayout(), byId = Object.fromEntries(DASH_CARDS.map((c) => [c[0], c]));
    const m = L.moneyFlow(monthStart());
    return `
    <section class="hero">
      <div class="hero-top"><span class="eyebrow">Total ${inclAssets ? 'net worth' : 'balance'} · ${esc(b)}</span>
        <div class="hero-tools"><label class="switch"><input type="checkbox" data-action="toggleAssets" ${inclAssets ? 'checked' : ''}><span>Include assets</span></label>
        <a class="hero-icon" href="#/customize" title="Customize dashboard" aria-label="Customize dashboard">${svg('tune')}</a></div></div>
      <h1 class="hero-num">${money(nw.total)}</h1>
      <div class="hero-stats"><span><small>Cash</small>${money(nw.cash)}</span><span><small>Assets</small>${money(nw.assets)}</span>
        <span><small>In this month</small><b class="pos">${money(m.income)}</b></span><span><small>Out this month</small><b class="neg">${money(m.expense)}</b></span></div>
      <div class="hero-actions">
        <button class="btn" data-action="quick" data-type="Expense">${svg('remove')}Expense</button>
        <button class="btn" data-action="quick" data-type="Income">${svg('add')}Income</button>
        <button class="btn" data-action="quick" data-type="Transfer">${svg('swap')}Transfer</button>
        ${undo.action ? `<button class="btn" data-action="undo">${svg('undo')}Undo</button>` : ''}
      </div>
    </section>
    <div class="grid dash">${order.filter((id) => !hidden.has(id)).map((id) => `<div class="card ${byId[id][4] ? 'span2' : ''}" data-card="${id}">${byId[id][3]()}</div>`).join('') || empty('All cards are hidden — use Customize to show some.')}</div>`;
  },
  actions: {
    quick: (el) => openTxForm(null, { type: el.dataset.type }),
    toggleAssets: (el) => { viewPrefs.set('includeAssets', el.checked); store.emit(); },
    editTx: (el) => openTxForm(store.get('transactions', el.dataset.id)),
    undo: () => { undo.perform(); store.emit(); },
  },
};

// ---------------------------------------------------------------- CUSTOMIZE DASHBOARD (EditDashboardScreen)
const customize = {
  title: 'Customize dashboard',
  render() {
    const { order, hidden } = dashLayout(), byId = Object.fromEntries(DASH_CARDS.map((c) => [c[0], c]));
    return `<div class="page-head"><div><a href="#/dashboard" class="muted">← Dashboard</a><h1>Reorder & toggle cards</h1><p class="muted">Drag anywhere to reorder, or use the arrows. Saved in this browser.</p></div>
      <button class="btn" data-action="reset">Reset to default</button></div>
      <div class="list" id="custList">${order.map((id, i) => `<div class="cust-row" draggable="true" data-id="${id}"><span class="grip">⠿</span>
        <div><strong>${esc(byId[id][1])}</strong><small>${esc(byId[id][2])}</small></div>
        <button class="icon-btn" data-action="move" data-id="${id}" data-d="-1" ${i === 0 ? 'disabled' : ''} aria-label="Move up">↑</button>
        <button class="icon-btn" data-action="move" data-id="${id}" data-d="1" ${i === order.length - 1 ? 'disabled' : ''} aria-label="Move down">↓</button>
        <label class="check" style="width:auto;margin:0"><input type="checkbox" data-action="toggle" data-id="${id}" ${hidden.has(id) ? '' : 'checked'}> Show</label></div>`).join('')}</div>`;
  },
  save(order, hidden) { viewPrefs.set('dashCards', { order, hidden: [...hidden] }); store.emit(); },
  actions: {
    move: (el) => { const { order, hidden } = dashLayout(), i = order.indexOf(el.dataset.id), j = i + Number(el.dataset.d); if (j < 0 || j >= order.length) return; [order[i], order[j]] = [order[j], order[i]]; customize.save(order, hidden); },
    toggle: (el) => { const { order, hidden } = dashLayout(); if (el.checked) hidden.delete(el.dataset.id); else hidden.add(el.dataset.id); customize.save(order, hidden); },
    reset: () => { viewPrefs.set('dashCards', null); store.emit(); toast('Dashboard reset'); },
  },
  drop(fromId, toId) { const { order, hidden } = dashLayout(); const i = order.indexOf(fromId); order.splice(i, 1); order.splice(order.indexOf(toId) + (i <= order.indexOf(toId) ? 1 : 0), 0, fromId); customize.save(order, hidden); },
};

// ---------------------------------------------------------------- ACCOUNTS
const accountFields = (isSub) => [
  { key: 'name', label: 'Account name', required: true },
  ...(isSub ? [{ key: 'parent_account_id', label: 'Parent account', type: 'select', numeric: true, options: store.all('accounts').filter((a) => a.parent_account_id == null).sort(sortByName).map((a) => [a.id, a.name]), half: true },
    { key: 'sub_account_type', label: 'Sub-account type', type: 'select', options: L.SUB_ACCOUNT_TYPES.map((s) => [s, s]), half: true }] : []),
  { key: 'type', label: 'Type', type: 'select', options: L.ACCOUNT_TYPES.map((t) => [t, t]), half: true },
  { key: 'currency_code', label: 'Currency', type: 'select', options: curOpts(), half: true },
  { key: 'initial_balance', label: 'Starting balance', type: 'number', half: true },
  { key: 'color', label: 'Color', type: 'color', half: true },
  { key: 'interest_rate_pa', label: 'Interest rate % p.a.', type: 'number', half: true },
  { key: 'compounding_frequency', label: 'Compounding', type: 'select', options: L.COMPOUNDING.map((c) => [c, c[0] + c.slice(1).toLowerCase()]), half: true },
  { key: 'linked_app_package', label: 'Linked banking app', type: 'select', options: [['', '— None —'], ...BANKS.map((b) => [b[0], b[1]])], half: true, hint: 'The phone opens this app; the web opens its website.' },
  { key: 'locked_until', label: 'Locked until (optional)', type: 'date', half: true },
  { key: 'is_locked', label: 'Locked (not spendable)', type: 'checkbox', half: true },
  { key: 'include_in_tally', label: 'Include in total balance', type: 'checkbox', half: true },
];
async function openAccountForm(existing, { sub = false, parentId } = {}) {
  const isSub = sub || existing?.parent_account_id != null;
  const values = existing ? { ...existing } : { type: isSub ? 'Savings' : 'Cash', currency_code: L.base(), include_in_tally: 1, initial_balance: 0, compounding_frequency: 'NONE', parent_account_id: parentId, sub_account_type: isSub ? 'Savings' : null, color: -16738120 };
  await formModal(existing ? 'Edit account' : isSub ? 'New sub-account' : 'New account', accountFields(isSub), values, {
    wide: true,
    onSubmit: (v) => {
      v.initial_balance = v.initial_balance ?? 0; v.interest_rate_pa = v.interest_rate_pa ?? 0;
      if (existing) {
        const updated = store.update('accounts', existing.id, v);
        store.update('accounts', existing.id, { current_balance: L.balanceOf(updated) });
      } else store.insert('accounts', { ...v, current_balance: v.initial_balance });
      toast(existing ? 'Account updated' : 'Account created');
    },
  });
}
const accounts = {
  title: 'Accounts',
  render() {
    const tree = accountsTree();
    const nw = L.netWorth(false);
    const negatives = store.all('accounts').filter((a) => L.balanceOf(a) < 0);
    return `<div class="page-head"><div><h1>Accounts</h1><p class="muted">Total in tally: <strong>${money(nw.cash)}</strong></p></div>
      <div class="row">${plus('Account', 'addAccount')}<button class="btn" data-action="addSub">${svg('add')}Sub-account</button></div></div>
      ${negatives.length ? `<div class="banner warn">${svg('warn')} Negative balance detected in ${negatives.map((a) => `<strong>${esc(a.name)}</strong>`).join(', ')}. ${negatives.map((a) => `<button class="btn small" data-action="adjust" data-id="${a.id}">Fix ${esc(a.name)} ${svg('arrow')}</button>`).join(' ')}</div>` : ''}
      ${tree.length ? `<div class="list">${tree.map(({ a, depth }) => {
        const bal = L.balanceOf(a);
        const conv = a.currency_code !== L.base() ? `<small class="muted">≈ ${money(L.convert(bal, a.currency_code, L.base()))}</small>` : '';
        const locked = a.is_locked || (a.locked_until && a.locked_until > Date.now());
        return `<div class="acc-row ${depth ? 'sub' : ''}">
          ${depth ? `<span class="dot" style="background:${argbToCss(a.color)}"></span>` : accMark(a, 38)}
          <a class="acc-main" href="#/accounts/${a.id}"><strong>${esc(a.name)}</strong>
            <small>${esc(a.sub_account_type || a.type)} · ${esc(a.currency_code)}${locked ? ' · Locked' : ''}${a.include_in_tally ? '' : ' · Excluded'}${a.interest_rate_pa > 0 ? ` · ${a.interest_rate_pa}% p.a.` : ''}</small></a>
          <div class="acc-bal"><strong class="${bal < 0 ? 'neg' : ''}">${money(bal, a.currency_code)}</strong>${conv}</div>
          <div class="row-actions">
            ${bankFor(a.linked_app_package) ? `<a class="icon-btn launch" title="Open ${esc(bankFor(a.linked_app_package)[1])} website" href="${bankFor(a.linked_app_package)[2]}" target="_blank" rel="noopener noreferrer">${svg('open')}</a>` : ''}
            <button class="icon-btn" title="Adjust balance" data-action="adjust" data-id="${a.id}">${svg('calibrate')}</button>
            <button class="icon-btn" title="Edit" data-action="editAccount" data-id="${a.id}">${svg('edit')}</button>
            <button class="icon-btn" title="Delete" data-action="deleteAccount" data-id="${a.id}">${svg('delete')}</button></div>
        </div>`;
      }).join('')}</div>` : empty('No accounts yet. Add your cash, bank and e-wallet accounts to start tracking.')}`;
  },
  actions: {
    addAccount: () => openAccountForm(null),
    addSub: () => (store.all('accounts').length ? openAccountForm(null, { sub: true }) : toast('Create a main account first', 'warn')),
    editAccount: (el) => openAccountForm(store.get('accounts', el.dataset.id)),
    adjust: async (el) => {
      const a = store.get('accounts', el.dataset.id);
      await formModal(`Adjust balance — ${a.name}`, [{ key: 'target', label: `Actual balance now (${a.currency_code})`, type: 'number', required: true }],
        { target: L.balanceOf(a).toFixed(2) }, { onSubmit: (v) => { L.adjustAccountBalance(a, v.target); toast('Balance calibrated'); } });
    },
    deleteAccount: async (el) => {
      const a = store.get('accounts', el.dataset.id);
      const n = store.all('transactions').filter((t) => t.account_id === a.id).length;
      if (await confirmDialog(`Delete ${a.name}?`, `This also deletes its ${n} transaction(s) on every synced device.`)) { store.remove('accounts', a.id); toast('Account deleted'); }
    },
  },
};
const accountDetail = {
  title: 'Account',
  render([id]) {
    const a = store.get('accounts', id);
    if (!a) return empty('Account not found');
    const txs = store.all('transactions').filter((t) => t.account_id === a.id || t.to_account_id === a.id).sort((x, y) => y.date - x.date || y.id - x.id);
    const subs = store.all('accounts').filter((s) => s.parent_account_id === a.id);
    return `<div class="page-head"><div><a href="#/accounts" class="muted">← Accounts</a><h1>${accMark(a, 40)}${esc(a.name)}</h1>
      <p class="muted">${esc(a.sub_account_type || a.type)} · ${esc(a.currency_code)}</p></div>
      <div class="row"><button class="btn" data-action="add" data-type="Income">${svg('add')}Income</button><button class="btn" data-action="add" data-type="Expense">${svg('remove')}Expense</button>
      <button class="btn" data-action="add" data-type="Transfer">${svg('swap')}Transfer</button><button class="btn" data-action="edit">Edit</button>${bankFor(a.linked_app_package) ? `<a class="btn primary" href="${bankFor(a.linked_app_package)[2]}" target="_blank" rel="noopener noreferrer">Open ${esc(bankFor(a.linked_app_package)[1])} ${svg('open')}</a>` : ''}</div></div>
      ${L.balanceOf(a) < 0 ? `<div class="banner warn">${svg('warn')} Negative balance detected. Calibrate it to the real balance. <button class="btn small" data-action="calibrate">Fix balance ${svg('arrow')}</button></div>` : ''}
      <div class="kpis big">${kpi('Current balance', `<button class="linkish" data-action="calibrate" title="Tap to calibrate">${money(L.balanceOf(a), a.currency_code)}</button>`)}${kpi('Starting balance', money(a.initial_balance, a.currency_code))}${kpi('Transactions', String(txs.length))}</div>
      ${(() => { const cm = L.cashMovement(a.id); return `<div class="card"><h3>Cash movement summary <small class="badge ${cm.net >= 0 ? 'pos' : 'neg'}">${cm.net >= 0 ? 'Net Positive' : 'Net Outflow'}</small></h3><div class="split two"><i style="width:${cm.inflow + cm.outflow ? (cm.inflow / (cm.inflow + cm.outflow)) * 100 : 50}%"></i></div><div class="kpis">${kpi('Total inflow', money(cm.inflow, a.currency_code), 'pos')}${kpi('Total outflow', money(cm.outflow, a.currency_code), 'neg')}${kpi('Net', money(cm.net, a.currency_code, { sign: true }), cm.net >= 0 ? 'pos' : 'neg')}</div></div>`; })()}
      ${subs.length ? `<h3>Sub-accounts</h3><div class="list">${subs.map((s) => `<a class="mini-row link" href="#/accounts/${s.id}"><span>${esc(s.name)} <small class="muted">${esc(s.sub_account_type || '')}</small></span><strong>${money(L.balanceOf(s), s.currency_code)}</strong></a>`).join('')}</div>` : ''}
      <h3>Ledger</h3><div class="list">${txs.length ? txs.map((t) => txRow(t, a.id)).join('') : empty('No transactions for this account')}</div>`;
  },
  actions: {
    add: (el, [id]) => openTxForm(null, { type: el.dataset.type, account_id: Number(id) }),
    edit: (el, [id]) => openAccountForm(store.get('accounts', id)),
    calibrate: async (el, [id]) => { const a = store.get('accounts', id); await formModal(`Calibrate balance — ${a.name}`, [{ key: 'target', label: `Real balance right now (${a.currency_code})`, type: 'number', required: true, hint: 'Adjusts the starting balance so the ledger matches reality — no transaction is added.' }], { target: L.balanceOf(a).toFixed(2) }, { submitLabel: 'Calibrate', onSubmit: (v) => { const snap = { ...a }; L.adjustAccountBalance(a, v.target); undo.set('Balance calibrated', () => store.put('accounts', snap)); } }); },
    editTx: (el) => openTxForm(store.get('transactions', el.dataset.id)),
  },
};

// ---------------------------------------------------------------- RECORDS
const recordFilter = { q: '', type: '', account: '', category: '', status: '', month: '' };
const PAGE_SIZE = 120;
let recordLimit = PAGE_SIZE;
const records = {
  title: 'Records',
  render() {
    const f = recordFilter;
    let txs = store.all('transactions');
    if (f.type) txs = txs.filter((t) => t.type === f.type);
    if (f.status) txs = txs.filter((t) => t.status === f.status);
    if (f.account) txs = txs.filter((t) => t.account_id === Number(f.account) || t.to_account_id === Number(f.account));
    if (f.category) txs = txs.filter((t) => t.category_id === Number(f.category));
    if (f.month) { const [y, m] = f.month.split('-').map(Number); const s = +new Date(y, m - 1, 1), e = +new Date(y, m, 1); txs = txs.filter((t) => t.date >= s && t.date < e); }
    if (f.q) { const q = f.q.toLowerCase(); txs = txs.filter((t) => `${t.note || ''} ${t.labels || ''} ${catOf(t.category_id)?.name || ''} ${accName(t.account_id)}`.toLowerCase().includes(q)); }
    txs.sort((a, b) => b.date - a.date || b.id - a.id);
    const inc = txs.filter((t) => t.type === 'Income' && t.status === 'Cleared').reduce((s, t) => s + L.convert(t.amount, store.get('accounts', t.account_id)?.currency_code || L.base(), L.base()), 0);
    const exp = txs.filter((t) => t.type === 'Expense' && t.status === 'Cleared').reduce((s, t) => s + L.convert(t.amount, store.get('accounts', t.account_id)?.currency_code || L.base(), L.base()), 0);
    const groups = new Map();
    for (const t of txs.slice(0, recordLimit)) { const k = fmtDate(t.date, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }); if (!groups.has(k)) groups.set(k, []); groups.get(k).push(t); }
    const sel = (name, opts, v) => `<select data-filter="${name}">${opts.map(([ov, ol]) => `<option value="${esc(ov)}" ${String(ov) === String(v) ? 'selected' : ''}>${esc(ol)}</option>`).join('')}</select>`;
    return `<div class="page-head"><div><h1>Records</h1><p class="muted">${txs.length} transaction(s) · income ${money(inc)} · expenses ${money(exp)}</p></div>${plus('Transaction', 'add')}</div>
      <div class="filters">
        <input type="search" placeholder="Search notes, labels, categories…" data-filter="q" value="${esc(f.q)}">
        ${sel('type', [['', 'All types'], ['Expense', 'Expense'], ['Income', 'Income'], ['Transfer', 'Transfer']], f.type)}
        ${sel('status', [['', 'Any status'], ['Cleared', 'Cleared'], ['Planned', 'Planned']], f.status)}
        ${sel('account', [['', 'All accounts'], ...accountOptions()], f.account)}
        ${sel('category', [['', 'All categories'], ...categoryOptions(null, false)], f.category)}
        <input type="month" data-filter="month" value="${esc(f.month)}">
      </div>
      ${groups.size ? [...groups.entries()].map(([day, list]) => `<h4 class="day">${esc(day)}</h4><div class="list">${list.map((t) => txRow(t)).join('')}</div>`).join('')
        : empty('No matching transactions')}
      ${txs.length > recordLimit ? `<div class="center"><button class="btn" data-action="more">Show more <small class="muted">· ${txs.length - recordLimit} older</small></button></div>` : ''}`;
  },
  actions: {
    add: () => openTxForm(null),
    editTx: (el) => openTxForm(store.get('transactions', el.dataset.id)),
    more: () => { recordLimit += PAGE_SIZE * 2; store.emit(); },
  },
  onFilter(name, value) { recordFilter[name] = value; recordLimit = PAGE_SIZE; },
};

// ---------------------------------------------------------------- CATEGORIES
const catFields = (type) => [
  { key: 'name', label: 'Name', required: true },
  { key: 'type', label: 'Type', type: 'select', options: [['Expense', 'Expense'], ['Income', 'Income']], half: true },
  { key: 'icon_res_id', label: 'Icon', type: 'select', options: Object.entries(L.ICONS).map(([k, e]) => [k, `${e} ${k.replace('ic_', '')}`]), half: true },
  { key: 'parent_category_id', label: 'Parent category', type: 'select', numeric: true, options: [['', '— Top level —'], ...store.all('categories').filter((c) => c.parent_category_id == null && (!type || c.type === type)).sort(sortByName).map((c) => [c.id, c.name])], half: true },
  { key: 'color', label: 'Color', type: 'color', half: true },
];
let catTab = 'Expense';
const categories = {
  title: 'Categories',
  render() {
    const cats = store.all('categories').filter((c) => c.type === catTab).sort(sortByName);
    const roots = cats.filter((c) => c.parent_category_id == null || !cats.some((p) => p.id === c.parent_category_id));
    const row = (c, sub) => `<div class="acc-row ${sub ? 'sub' : ''}"><span class="cat-ico" style="background:${argbToCss(c.color)}22;color:${argbToCss(c.color)}">${L.icon(c.icon_res_id)}</span>
      <div class="acc-main"><strong>${esc(c.name)}</strong><small>${store.all('transactions').filter((t) => t.category_id === c.id).length} transaction(s)</small></div>
      <div class="row-actions"><button class="icon-btn" data-action="edit" data-id="${c.id}">${svg('edit')}</button><button class="icon-btn" data-action="del" data-id="${c.id}">${svg('delete')}</button></div></div>`;
    return `<div class="page-head"><h1>Categories</h1>${plus('Category', 'add')}</div>
      <div class="tabs">${['Expense', 'Income'].map((t) => `<button class="tab ${t === catTab ? 'on' : ''}" data-action="tab" data-tab="${t}">${t}</button>`).join('')}</div>
      <div class="list">${roots.length ? roots.map((r) => row(r) + cats.filter((c) => c.parent_category_id === r.id).map((c) => row(c, true)).join('')).join('') : empty('No categories')}</div>`;
  },
  actions: {
    tab: (el) => { catTab = el.dataset.tab; store.emit(); },
    add: () => formModal('New category', catFields(catTab), { type: catTab, icon_res_id: 'ic_category', color: -16738120 }, { onSubmit: (v) => { store.insert('categories', v); toast('Category added'); } }),
    edit: (el) => { const c = store.get('categories', el.dataset.id); return formModal('Edit category', catFields(c.type), c, { onSubmit: (v) => { if (v.parent_category_id === c.id) v.parent_category_id = null; store.update('categories', c.id, v); toast('Category updated'); } }); },
    del: async (el) => { const c = store.get('categories', el.dataset.id); if (await confirmDialog(`Delete ${c.name}?`, 'Transactions keep existing but become uncategorized. Budgets for this category are deleted.')) { store.remove('categories', c.id); toast('Category deleted'); } },
  },
};

// ---------------------------------------------------------------- BUDGETS
function periodRange(period, ref = new Date()) {
  if (period === 'Weekly') { const s = new Date(ref); s.setHours(0, 0, 0, 0); s.setDate(s.getDate() - ((s.getDay() + 6) % 7)); const e = new Date(s); e.setDate(s.getDate() + 7); return [+s, +e - 1]; }
  const s = new Date(ref.getFullYear(), ref.getMonth(), 1), e = new Date(ref.getFullYear(), ref.getMonth() + 1, 1); return [+s, +e - 1];
}
const budgetFields = () => [
  { key: 'category_id', label: 'Category', type: 'select', numeric: true, options: categoryOptions('Expense', false), required: true },
  { key: 'amount_limit', label: 'Limit', type: 'money', required: true, half: true },
  { key: 'period', label: 'Period', type: 'select', options: [['Monthly', 'Monthly (this month)'], ['Weekly', 'Weekly (this week)'], ['Custom', 'Custom dates']], half: true },
  { key: 'start_date', label: 'Start (custom)', type: 'date', half: true },
  { key: 'end_date', label: 'End (custom)', type: 'date', half: true },
];
function saveBudget(v, existing) {
  if (v.period !== 'Custom') [v.start_date, v.end_date] = periodRange(v.period);
  else { if (!v.start_date || !v.end_date) throw new Error('Pick start and end dates'); v.end_date += 86399999; }
  if (existing) store.update('budgets', existing.id, v); else store.insert('budgets', v);
  toast('Budget saved');
}
const budgets = {
  title: 'Budgets',
  render() {
    const list = L.budgetProgress();
    const total = list.reduce((s, x) => s + x.budget.amount_limit, 0), spent = list.reduce((s, x) => s + x.spent, 0);
    return `<div class="page-head"><div><h1>Budgets</h1><p class="muted">${money(spent)} spent of ${money(total)} budgeted</p></div>${plus('Budget', 'add')}</div>
      ${list.length ? `<div class="grid">${list.map((x) => `<div class="card"><h3>${L.icon(x.category.icon_res_id)} ${esc(x.category.name)}
        <span class="row-actions"><button class="icon-btn" data-action="edit" data-id="${x.budget.id}">${svg('edit')}</button><button class="icon-btn" data-action="del" data-id="${x.budget.id}">${svg('delete')}</button></span></h3>
        ${progressBar(x.ratio)}<div class="kpis">${kpi('Spent', money(x.spent), x.ratio >= 1 ? 'neg' : '')}${kpi(x.remaining >= 0 ? 'Left' : 'Over by', money(Math.abs(x.remaining)), x.remaining >= 0 ? 'pos' : 'neg')}</div>
        <small class="muted">${esc(x.budget.period)} · ${fmtDate(x.budget.start_date)} – ${fmtDate(x.budget.end_date)} · ${Math.round(x.ratio * 100)}%</small></div>`).join('')}</div>`
        : empty('No budgets yet. Set a spending limit for a category.')}
      ${(() => { const ex = L.categoryExpensesAllTime(), tot = ex.reduce((s2, x) => s2 + x.total, 0); if (!ex.length) return `<div class="card"><h3>Expense breakdown</h3>${empty('No Cleared Expenses Yet')}</div>`;
        const items = ex.map((x, i) => ({ label: x.category.name, value: x.total, color: ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#e87ba4', '#008300', '#6250d6', '#e34948'][i % 8] }));
        return `<div class="card"><h3>Expense breakdown <small class="muted">${ex.length} categories tracked</small></h3><div class="donut-wrap">${donut(items)}<div class="legend-list"><div class="mini-row"><span><strong>Total spending</strong></span><strong>${money(tot)}</strong></div>
          ${items.map((i, k) => `<div class="mini-row"><span><i class="sw" style="background:${i.color}"></i>${L.icon(ex[k].category.icon_res_id)} ${esc(i.label)}</span><strong>${money(i.value)} <small class="muted">${Math.round((i.value / tot) * 100)}%</small></strong></div>`).join('')}</div></div></div>`; })()}
      ${list.some((x) => x.budget.end_date < Date.now()) ? `<p class="muted center"><button class="btn" data-action="rollover">Roll expired budgets into the current period</button></p>` : ''}`;
  },
  actions: {
    add: () => formModal('New budget', budgetFields(), { period: 'Monthly' }, { onSubmit: (v) => saveBudget(v) }),
    edit: (el) => { const b = store.get('budgets', el.dataset.id); return formModal('Edit budget', budgetFields(), b, { onSubmit: (v) => saveBudget(v, b) }); },
    del: async (el) => { if (await confirmDialog('Delete budget?', 'Spending history is not affected.')) { store.remove('budgets', el.dataset.id); toast('Budget deleted'); } },
    rollover: () => { for (const b of store.all('budgets').filter((x) => x.end_date < Date.now() && x.period !== 'Custom')) { const [s, e] = periodRange(b.period); store.update('budgets', b.id, { start_date: s, end_date: e }); } toast('Budgets moved to the current period'); },
  },
};

// ---------------------------------------------------------------- DEBTS
let debtTab = 'Lent';
const debtFields = [
  { key: 'person_name', label: 'Person', required: true },
  { key: 'type', label: 'Type', type: 'select', options: [['Lent', 'I lent money (they owe me)'], ['Borrowed', 'I borrowed (I owe them)']] },
  { key: 'total_amount', label: 'Amount', type: 'money', required: true, half: true },
  { key: 'due_date', label: 'Due date', type: 'date', half: true },
  { key: 'notes', label: 'Notes', type: 'textarea' },
];
const debts = {
  title: 'Debts',
  render() {
    const all = store.all('debts');
    const list = (debtTab === 'Settled' ? all.filter((d) => d.status === 'Settled') : all.filter((d) => d.type === debtTab && d.status !== 'Settled')).sort((a, b) => (a.due_date || 9e15) - (b.due_date || 9e15));
    const sum = (t) => all.filter((d) => d.type === t && d.status === 'Active').reduce((s, d) => s + d.remaining_amount, 0);
    return `<div class="page-head"><h1>Debts & receivables</h1>${plus('Debt', 'add')}</div>
      <div class="kpis big">${kpi('Owed to you', money(sum('Lent')), 'pos')}${kpi('You owe', money(sum('Borrowed')), 'neg')}${kpi('Net', money(sum('Lent') - sum('Borrowed'), L.base(), { sign: true }))}</div>
      <div class="tabs">${['Lent', 'Borrowed', 'Settled'].map((t) => `<button class="tab ${t === debtTab ? 'on' : ''}" data-action="tab" data-tab="${t}">${t === 'Lent' ? 'Receivables' : t === 'Borrowed' ? 'Payables' : 'Settled'}</button>`).join('')}</div>
      <div class="list">${list.length ? list.map((d) => {
        const paid = d.total_amount - d.remaining_amount, overdue = d.status === 'Active' && d.due_date && d.due_date < Date.now();
        return `<div class="card flat"><div class="mini-row"><span><strong>${esc(d.person_name)}</strong> <small class="badge ${d.type === 'Lent' ? 'pos' : 'neg'}">${esc(d.type)}</small>${overdue ? ' <small class="badge danger">Overdue</small>' : ''}<br>
          <small class="muted">${d.due_date ? 'Due ' + fmtDate(d.due_date) : 'No due date'}${d.notes ? ' · ' + esc(d.notes) : ''}</small></span>
          <strong>${money(d.remaining_amount)}<br><small class="muted">of ${money(d.total_amount)}</small></strong></div>
          ${progressBar(d.total_amount ? paid / d.total_amount : 0, 2, { good: true })}
          <div class="row">${d.status === 'Active' ? `<button class="btn small" data-action="pay" data-id="${d.id}">Record payment</button><button class="btn small" data-action="settle" data-id="${d.id}">Settle in full</button>` : ''}
          <button class="btn small ghost" data-action="edit" data-id="${d.id}">Edit</button><button class="btn small ghost danger" data-action="del" data-id="${d.id}">Delete</button></div></div>`;
      }).join('') : empty('Nothing here')}</div>`;
  },
  actions: {
    tab: (el) => { debtTab = el.dataset.tab; store.emit(); },
    add: () => formModal('New debt', debtFields, { type: debtTab === 'Borrowed' ? 'Borrowed' : 'Lent' }, { onSubmit: (v) => { store.insert('debts', { ...v, remaining_amount: v.total_amount, status: 'Active', created_at: Date.now() }); toast('Debt added'); } }),
    edit: (el) => { const d = store.get('debts', el.dataset.id); return formModal('Edit debt', [...debtFields, { key: 'remaining_amount', label: 'Remaining', type: 'money', half: true }], d, { onSubmit: (v) => { store.update('debts', d.id, { ...v, status: v.remaining_amount <= 0.0001 ? 'Settled' : 'Active' }); toast('Debt updated'); } }); },
    pay: (el) => { const d = store.get('debts', el.dataset.id); return formModal(`Payment — ${d.person_name}`, [{ key: 'amount', label: 'Amount', type: 'money', required: true }, { key: 'account', label: d.type === 'Lent' ? 'Deposit into account' : 'Pay from account', type: 'select', numeric: true, options: accountOptions(true), hint: 'Optional — logs a transaction' }], { amount: d.remaining_amount }, { onSubmit: (v) => { L.payDebtPortion(d, v.amount, v.account); toast('Payment recorded'); } }); },
    settle: (el) => { const d = store.get('debts', el.dataset.id); return formModal(`Settle — ${d.person_name}`, [{ key: 'account', label: d.type === 'Lent' ? 'Deposit into account' : 'Pay from account', type: 'select', numeric: true, options: accountOptions(true), hint: 'Optional — logs a transaction for the remaining amount' }], {}, { submitLabel: 'Settle', onSubmit: (v) => { L.settleDebt(d, v.account); toast('Debt settled'); } }); },
    del: async (el) => { if (await confirmDialog('Delete debt?', 'Logged repayment transactions stay in your records.')) { store.remove('debts', el.dataset.id); toast('Debt deleted'); } },
  },
};

// ---------------------------------------------------------------- SUBSCRIPTIONS & PAYOUTS
const CADENCES = [['Daily', 'Every day'], ['Weekdays', 'Weekdays (Mon–Fri)'], ['Weekends', 'Weekends (Sat–Sun)'], ['Weekly', 'Weekly (same weekday)'],
  ['Biweekly', 'Every 2 weeks'], ['Custom', 'Specific days of the week'], ['Monthly', 'Monthly (same date)'], ['MonthlyDay', 'Monthly on a chosen day'],
  ['Monthly:Last', 'Monthly on the last day'], ['Yearly', 'Yearly']];
const subFields = () => [
  { key: 'note', label: 'Name (e.g. Netflix, Salary)', required: true },
  { key: 'type', label: 'Type', type: 'select', options: [['Expense', 'Expense / bill'], ['Income', 'Income / receivable']], half: true },
  { key: 'amount', label: 'Amount', type: 'money', required: true, half: true },
  { key: 'cadence', label: 'Recurrence cadence', type: 'select', options: CADENCES, half: true },
  { key: 'days', label: 'Select active days', type: 'weekdays', half: true },
  { key: 'dom', label: 'Day of the month (1–31)', type: 'number', half: true },
  { key: 'date', label: 'Starts / next due', type: 'date', required: true, half: true },
  { key: 'time', label: 'Time of day', type: 'time', half: true },
  { key: 'account_id', label: 'Account', type: 'select', numeric: true, options: accountOptions(), required: true, half: true },
  { key: 'category_id', label: 'Category', type: 'select', numeric: true, options: categoryOptions(null), half: true },
];
// rule string <-> form values (RecurrenceHelper formats: Custom:MON,WED · Monthly:15 · Monthly:Last)
function ruleToForm(t) {
  const r = t.recurrence_rule || 'Monthly', d = new Date(t.date);
  const base = { ...t, time: `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`, days: [], dom: d.getDate() };
  if (/^custom:/i.test(r)) return { ...base, cadence: 'Custom', days: r.slice(7).split(',').map((x) => x.trim()) };
  if (/^monthly:last$/i.test(r)) return { ...base, cadence: 'Monthly:Last' };
  if (/^monthly:\d+$/i.test(r)) return { ...base, cadence: 'MonthlyDay', dom: Number(r.split(':')[1]) };
  return { ...base, cadence: CADENCES.some((c) => c[0] === r) ? r : 'Monthly' };
}
function formToRule(v) {
  let rule = v.cadence;
  if (rule === 'Custom') { if (!v.days.length) throw new Error('Pick at least one day'); rule = `Custom:${v.days.join(',')}`; }
  if (rule === 'MonthlyDay') { const n = Math.round(v.dom || 0); if (n < 1 || n > 31) throw new Error('Day of the month must be 1–31'); rule = `Monthly:${n}`; }
  const date = new Date(v.date); const [hh, mm] = String(v.time || '09:00').split(':').map(Number); date.setHours(hh || 0, mm || 0, 0, 0);
  const out = { note: v.note, type: v.type, amount: v.amount, account_id: v.account_id, category_id: v.category_id, recurrence_rule: rule, date: +date };
  return out;
}
function cadenceToggle(w) {
  const c = w.querySelector('[name=cadence]').value;
  w.querySelector('[data-field=days]').style.display = c === 'Custom' ? '' : 'none';
  w.querySelector('[name=dom]').closest('.field').style.display = c === 'MonthlyDay' ? '' : 'none';
}
const cadenceOpen = (w) => { cadenceToggle(w); w.querySelector('[name=cadence]').addEventListener('change', () => cadenceToggle(w)); };
const payoutFields = () => [
  { key: 'title', label: 'Title', required: true },
  { key: 'amount', label: 'Amount', type: 'money', required: true, half: true },
  { key: 'currency_code', label: 'Currency', type: 'select', options: curOpts(), half: true },
  { key: 'due_date', label: 'Due date', type: 'date', required: true, half: true },
  { key: 'priority', label: 'Priority', type: 'select', options: [['HIGH', 'High'], ['NORMAL', 'Normal'], ['LOW', 'Low']], half: true },
  { key: 'target_account_id', label: 'Pay from account', type: 'select', numeric: true, options: accountOptions(true), half: true },
  { key: 'category_id', label: 'Category', type: 'select', numeric: true, options: categoryOptions('Expense'), half: true },
  { key: 'payee_or_vendor', label: 'Payee / vendor' },
  { key: 'notes', label: 'Notes', type: 'textarea' },
];
const subscriptions = {
  title: 'Subscriptions',
  render() {
    const subs = L.recurringTemplates().map((t) => ({ t, next: L.nextDueDate(t.date, t.recurrence_rule), cur: store.get('accounts', t.account_id)?.currency_code || L.base() })).sort((a, b) => a.next - b.next);
    const mExp = subs.filter((s) => s.t.type === 'Expense').reduce((s, x) => s + L.convert(L.monthlyEquivalent(x.t.amount, x.t.recurrence_rule), x.cur, L.base()), 0);
    const mInc = subs.filter((s) => s.t.type === 'Income').reduce((s, x) => s + L.convert(L.monthlyEquivalent(x.t.amount, x.t.recurrence_rule), x.cur, L.base()), 0);
    const pend = store.all('planned_payouts').filter((p) => p.status === 'PENDING').sort((a, b) => a.due_date - b.due_date);
    const paid = store.all('planned_payouts').filter((p) => p.status !== 'PENDING').sort((a, b) => (b.paid_date || 0) - (a.paid_date || 0)).slice(0, 10);
    return `<div class="page-head"><h1>Subscriptions & bills</h1><div class="row">${plus('Recurring', 'addSub')}<button class="btn" data-action="addPayout">${svg('add')}Planned payout</button></div></div>
      <div class="kpis big">${kpi('Monthly bills', money(mExp), 'neg')}${kpi('Monthly recurring income', money(mInc), 'pos')}${kpi('Pending payouts', money(pend.reduce((s, p) => s + L.convert(p.amount, p.currency_code, L.base()), 0)))}</div>
      <h3>Recurring</h3><div class="list">${subs.length ? subs.map(({ t, next, cur }) => `<div class="acc-row">
        <span class="cat-ico">${L.icon(catOf(t.category_id)?.icon_res_id)}</span>
        <div class="acc-main"><strong>${esc(t.note || catOf(t.category_id)?.name || 'Recurring')}</strong><small>${esc(L.scheduleLabel(t.recurrence_rule, t.date))} · next ${fmtDate(next)} · ${esc(accName(t.account_id))}</small></div>
        <div class="acc-bal"><strong class="${t.type === 'Income' ? 'pos' : 'neg'}">${money(t.type === 'Income' ? t.amount : -t.amount, cur, { sign: true })}</strong><small class="muted">≈ ${money(L.monthlyEquivalent(t.amount, t.recurrence_rule), cur)}/mo</small></div>
        <div class="row-actions"><button class="icon-btn" title="${t.type === 'Income' ? 'Mark received' : 'Mark paid'}" data-action="logSub" data-id="${t.id}">${svg('check')}</button><button class="icon-btn" title="Skip to next cycle" data-action="skipSub" data-id="${t.id}">${svg('skip')}</button><button class="icon-btn" title="Edit" data-action="editSub" data-id="${t.id}">${svg('edit')}</button>
        <button class="icon-btn" title="Stop" data-action="stopSub" data-id="${t.id}">${svg('stop')}</button><button class="icon-btn" title="Delete" data-action="delSub" data-id="${t.id}">${svg('delete')}</button></div></div>`).join('') : empty('No recurring items')}</div>
      <h3>Planned payouts</h3><div class="list">${pend.length ? pend.map((p) => `<div class="acc-row">
        <span class="cat-ico">${prio(p.priority)}</span>
        <div class="acc-main"><strong>${esc(p.title)}</strong><small>Due ${fmtDate(p.due_date)}${p.due_date < Date.now() ? ' · <b class="neg">overdue</b>' : ''}${p.payee_or_vendor ? ' · ' + esc(p.payee_or_vendor) : ''}</small></div>
        <div class="acc-bal"><strong>${money(p.amount, p.currency_code)}</strong></div>
        <div class="row-actions"><button class="btn small" data-action="payPayout" data-id="${p.id}">Mark paid</button><button class="icon-btn" data-action="editPayout" data-id="${p.id}">${svg('edit')}</button><button class="icon-btn" data-action="delPayout" data-id="${p.id}">${svg('delete')}</button></div></div>`).join('') : empty('No pending payouts')}</div>
      ${paid.length ? `<h3>Recently paid</h3><div class="list">${paid.map((p) => `<div class="acc-row"><span class="cat-ico pos">${svg('done')}</span><div class="acc-main"><strong>${esc(p.title)}</strong><small>${esc(p.status)} ${p.paid_date ? fmtDate(p.paid_date) : ''}</small></div>
        <div class="acc-bal"><strong>${money(p.amount, p.currency_code)}</strong></div><div class="row-actions">${p.status === 'PAID' ? `<button class="btn small ghost" data-action="unpay" data-id="${p.id}">Undo</button>` : ''}</div></div>`).join('')}</div>` : ''}`;
  },
  actions: {
    addSub: () => (store.all('accounts').length ? formModal('New recurring item', subFields(), ruleToForm({ type: 'Expense', recurrence_rule: 'Monthly', date: Date.now(), account_id: store.all('accounts')[0]?.id }), { wide: true, onOpen: cadenceOpen, onSubmit: (v) => { L.addTransaction({ ...formToRule(v), status: 'Planned', labels: '[]' }); toast('Recurring item added'); } }) : toast('Add an account first', 'warn')),
    editSub: (el) => { const t = store.get('transactions', el.dataset.id); return formModal('Edit recurring item', subFields(), ruleToForm(t), { wide: true, onOpen: cadenceOpen, onSubmit: (v) => { store.update('transactions', t.id, formToRule(v)); toast('Saved'); } }); },
    skipSub: (el) => { const t = store.get('transactions', el.dataset.id); const u = L.skipRecurringCycle(t); undo.set(`Skipped to ${fmtDate(store.get('transactions', t.id).date)}`, u); },
    stopSub: async (el) => { if (await confirmDialog('Stop this recurring item?', 'It will no longer repeat or appear in forecasts as recurring.', 'Stop', 'primary')) { store.update('transactions', el.dataset.id, { recurrence_rule: 'None' }); toast('Stopped'); } },
    delSub: async (el) => { if (await confirmDialog('Delete recurring item?', 'This cannot be undone.')) { store.remove('transactions', el.dataset.id); toast('Deleted'); } },
    logSub: (el) => { const t = store.get('transactions', el.dataset.id), inc = t.type === 'Income'; return formModal(`${inc ? 'Receive' : 'Pay'} — ${t.note || 'recurring'}`, [{ key: 'amount', label: 'Amount', type: 'money', required: true, half: true }, { key: 'account_id', label: 'Account', type: 'select', numeric: true, options: accountOptions(), half: true }, { key: 'note', label: 'Note (optional)' }, { key: 'advance', label: 'Advance to next schedule cycle (moves next due date forward)', type: 'checkbox' }], { amount: t.amount, account_id: t.account_id, advance: 1 }, { submitLabel: inc ? 'Mark received' : 'Mark paid', onSubmit: (v) => { const u = L.payRecurring(t, { amount: v.amount, accountId: v.account_id, advance: !!v.advance, note: v.note }); undo.set(`${inc ? 'Received' : 'Paid'}: ${t.note || t.type}`, u); } }); },
    addPayout: () => formModal('New planned payout', payoutFields(), { currency_code: L.base(), priority: 'NORMAL', due_date: Date.now() }, { wide: true, onSubmit: (v) => { store.insert('planned_payouts', { ...v, status: 'PENDING', created_at: Date.now() }); toast('Payout planned'); } }),
    editPayout: (el) => { const p = store.get('planned_payouts', el.dataset.id); return formModal('Edit payout', payoutFields(), p, { wide: true, onSubmit: (v) => { store.update('planned_payouts', p.id, v); toast('Saved'); } }); },
    delPayout: async (el) => { if (await confirmDialog('Delete payout?', 'This cannot be undone.')) { store.remove('planned_payouts', el.dataset.id); toast('Deleted'); } },
    payPayout: (el) => { const p = store.get('planned_payouts', el.dataset.id); return formModal(`Mark paid — ${p.title}`, [{ key: 'account', label: 'Paid from', type: 'select', numeric: true, options: accountOptions(), required: true }, { key: 'amount', label: 'Actual amount', type: 'money', required: true }], { account: p.target_account_id ?? store.all('accounts')[0]?.id, amount: p.amount }, { onSubmit: (v) => { L.markPayoutPaid(p, v.account, v.amount); undo.set(`Paid: ${p.title}`, () => L.unmarkPayoutPaid(store.get('planned_payouts', p.id))); } }); },
    unpay: async (el) => { const p = store.get('planned_payouts', el.dataset.id); if (await confirmDialog('Undo payment?', 'The expense transaction that was logged for it will be deleted.', 'Undo', 'primary')) { L.unmarkPayoutPaid(p); toast('Payment undone'); } },
  },
};

// ---------------------------------------------------------------- COMMISSIONS (freelance)
const projectFields = [
  { key: 'project_name', label: 'Project', required: true },
  { key: 'client', label: 'Client', half: true },
  { key: 'total_expected_fee', label: 'Total expected fee', type: 'money', half: true },
  { key: 'status', label: 'Status', type: 'select', options: [['Active', 'Active'], ['Completed', 'Completed']] },
];
const milestoneFields = [
  { key: 'title', label: 'Milestone', required: true },
  { key: 'amount', label: 'Amount', type: 'money', required: true, half: true },
  { key: 'expected_date', label: 'Expected date', type: 'date', required: true, half: true },
];
const commissions = {
  title: 'Commissions',
  render() {
    const projects = store.all('freelance_projects').sort((a, b) => (a.status === 'Active' ? -1 : 1) - (b.status === 'Active' ? -1 : 1) || b.created_at - a.created_at);
    const ms = store.all('milestones');
    const pendingTotal = ms.filter((m) => m.status === 'Pending').reduce((s, m) => s + m.amount, 0);
    const paidTotal = ms.filter((m) => m.status === 'Paid').reduce((s, m) => s + m.amount, 0);
    return `<div class="page-head"><h1>Freelance commissions</h1>${plus('Project', 'addProject')}</div>
      <div class="kpis big">${kpi('Pending', money(pendingTotal))}${kpi('Received', money(paidTotal), 'pos')}${kpi('Active projects', String(projects.filter((p) => p.status === 'Active').length))}</div>
      ${projects.length ? projects.map((p) => {
        const pm = ms.filter((m) => m.project_id === p.id).sort((a, b) => a.expected_date - b.expected_date);
        const got = pm.filter((m) => m.status === 'Paid').reduce((s, m) => s + m.amount, 0);
        return `<div class="card"><h3>${esc(p.project_name)} <small class="badge ${p.status === 'Active' ? '' : 'pos'}">${esc(p.status)}</small>
          <span class="row-actions"><button class="icon-btn" data-action="editProject" data-id="${p.id}">${svg('edit')}</button><button class="icon-btn" data-action="delProject" data-id="${p.id}">${svg('delete')}</button></span></h3>
          <p class="muted">${esc(p.client || 'No client')} · ${money(got)} received of ${money(p.total_expected_fee)}</p>${progressBar(p.total_expected_fee ? got / p.total_expected_fee : 0, 2, { good: true })}
          <div class="list">${pm.map((m) => `<div class="mini-row"><span>${m.status === 'Paid' ? svg('done', 'pos') : svg('clock', 'muted')} ${esc(m.title)}<br><small class="muted">${m.status === 'Paid' ? 'Paid ' + fmtDate(m.paid_at) : 'Expected ' + fmtDate(m.expected_date)}</small></span>
            <span class="row"><strong>${money(m.amount)}</strong>${m.status === 'Pending' ? `<button class="btn small" data-action="payMilestone" data-id="${m.id}">Mark paid</button>` : ''}
            <button class="icon-btn" data-action="editMilestone" data-id="${m.id}">${svg('edit')}</button><button class="icon-btn" data-action="delMilestone" data-id="${m.id}">${svg('delete')}</button></span></div>`).join('')}</div>
          <button class="btn small" data-action="addMilestone" data-id="${p.id}">${svg('add')}Milestone</button></div>`;
      }).join('') : empty('No freelance projects yet')}`;
  },
  actions: {
    addProject: () => formModal('New project', projectFields, { status: 'Active' }, { onSubmit: (v) => { store.insert('freelance_projects', { ...v, client: v.client || '', total_expected_fee: v.total_expected_fee || 0, created_at: Date.now() }); toast('Project added'); } }),
    editProject: (el) => { const p = store.get('freelance_projects', el.dataset.id); return formModal('Edit project', projectFields, p, { onSubmit: (v) => { store.update('freelance_projects', p.id, { ...v, client: v.client || '', total_expected_fee: v.total_expected_fee || 0 }); toast('Saved'); } }); },
    delProject: async (el) => { if (await confirmDialog('Delete project?', 'Its milestones are deleted too. Logged income stays.')) { store.remove('freelance_projects', el.dataset.id); toast('Project deleted'); } },
    addMilestone: (el) => formModal('New milestone', milestoneFields, { expected_date: Date.now() }, { onSubmit: (v) => { store.insert('milestones', { ...v, project_id: Number(el.dataset.id), status: 'Pending' }); toast('Milestone added'); } }),
    editMilestone: (el) => { const m = store.get('milestones', el.dataset.id); return formModal('Edit milestone', milestoneFields, m, { onSubmit: (v) => { store.update('milestones', m.id, v); toast('Saved'); } }); },
    delMilestone: async (el) => { if (await confirmDialog('Delete milestone?', 'This cannot be undone.')) { store.remove('milestones', el.dataset.id); toast('Deleted'); } },
    payMilestone: (el) => { const m = store.get('milestones', el.dataset.id); return formModal(`Mark paid — ${m.title}`, [{ key: 'account', label: 'Received into', type: 'select', numeric: true, options: accountOptions(), required: true }], {}, { onSubmit: (v) => { L.markMilestonePaid(m, v.account); toast('Income logged'); } }); },
  },
};

// ---------------------------------------------------------------- ASSETS
const assetFields = [
  { key: 'name', label: 'Name', required: true },
  { key: 'category', label: 'Category', type: 'select', options: L.ASSET_CATEGORIES.map((c) => [c, c]), half: true },
  { key: 'currency_code', label: 'Currency', type: 'select', options: curOpts(), half: true },
  { key: 'estimated_value', label: 'Current value', type: 'money', required: true, half: true },
  { key: 'purchase_price', label: 'Purchase price', type: 'money', half: true },
  { key: 'purchase_date', label: 'Purchase date', type: 'date', half: true },
  { key: 'color', label: 'Color', type: 'color', half: true },
  { key: 'include_in_net_worth', label: 'Include in net worth', type: 'checkbox' },
  { key: 'notes', label: 'Notes', type: 'textarea' },
];
const assets = {
  title: 'Assets',
  render() {
    const list = store.all('assets').sort((a, b) => b.estimated_value - a.estimated_value);
    const tv = list.reduce((s, a) => s + L.convert(a.estimated_value, a.currency_code, L.base()), 0);
    const tp = list.reduce((s, a) => s + L.convert(a.purchase_price || 0, a.currency_code, L.base()), 0);
    const byCat = L.ASSET_CATEGORIES.map((c) => ({ c, v: list.filter((a) => a.category === c).reduce((s, a) => s + L.convert(a.estimated_value, a.currency_code, L.base()), 0) })).filter((x) => x.v > 0);
    return `<div class="page-head"><h1>Assets & valuables</h1>${plus('Asset', 'add')}</div>
      <div class="kpis big">${kpi('Total value', money(tv))}${kpi('Gain / loss', money(tv - tp, L.base(), { sign: true }), tv - tp >= 0 ? 'pos' : 'neg')}${kpi('Items', String(list.length))}</div>
      ${byCat.length ? `<div class="chips wide">${byCat.map((x) => `<span class="chip">${esc(x.c)} · ${money(x.v)}</span>`).join('')}</div>` : ''}
      <div class="list">${list.length ? list.map((a) => { const g = a.estimated_value - (a.purchase_price || 0); return `<div class="acc-row">
        <span class="dot" style="background:${argbToCss(a.color)}"></span>
        <div class="acc-main"><strong>${esc(a.name)}</strong><small>${esc(a.category)}${a.purchase_date ? ' · bought ' + fmtDate(a.purchase_date) : ''}${a.include_in_net_worth ? '' : ' · not in net worth'}</small></div>
        <div class="acc-bal"><strong>${money(a.estimated_value, a.currency_code)}</strong>${a.purchase_price ? `<small class="${g >= 0 ? 'pos' : 'neg'}">${money(g, a.currency_code, { sign: true })} (${((g / a.purchase_price) * 100).toFixed(1)}%)</small>` : ''}</div>
        <div class="row-actions"><button class="icon-btn" data-action="edit" data-id="${a.id}">${svg('edit')}</button><button class="icon-btn" data-action="del" data-id="${a.id}">${svg('delete')}</button></div></div>`; }).join('') : empty('No assets yet — log vehicles, gadgets, property, gold…')}</div>`;
  },
  actions: {
    add: () => formModal('New asset', assetFields, { currency_code: L.base(), include_in_net_worth: 1, category: 'Other', color: -16738120 }, { wide: true, onSubmit: (v) => { store.insert('assets', { ...v, purchase_price: v.purchase_price || 0, created_at: Date.now() }); toast('Asset added'); } }),
    edit: (el) => { const a = store.get('assets', el.dataset.id); return formModal('Edit asset', assetFields, a, { wide: true, onSubmit: (v) => { store.update('assets', a.id, { ...v, purchase_price: v.purchase_price || 0 }); toast('Saved'); } }); },
    del: async (el) => { if (await confirmDialog('Delete asset?', 'This cannot be undone.')) { store.remove('assets', el.dataset.id); toast('Deleted'); } },
  },
};

// ---------------------------------------------------------------- CARDS
const revealed = new Set();
const FINISH = { OBSIDIAN_GOLD: ['#1b1b1f', '#b8913a'], MATTE_CARBON: ['#2b2d31', '#16171a'], ROYAL_SAPPHIRE: ['#16307a', '#3f6ee8'], EMERALD_GLASS: ['#0c5c46', '#2fbf8f'],
  CRIMSON_TITANIUM: ['#6e1020', '#d2384f'], CYBER_VIOLET: ['#3a1677', '#9b5cf6'], OCEAN_TEAL: ['#0b4f63', '#1fb6c9'], BRONZE_LUXE: ['#5a3a1a', '#c48a4a'] };
const cardFields = () => [
  { key: 'card_name', label: 'Card name', required: true },
  { key: 'card_type', label: 'Type', type: 'select', options: L.CARD_TYPES, half: true },
  { key: 'card_network', label: 'Network', type: 'select', options: L.CARD_NETWORKS.map((n) => [n, n]), half: true },
  { key: 'issuing_bank', label: 'Issuing bank', half: true },
  { key: 'cardholder_name', label: 'Cardholder name', half: true },
  { key: 'card_number', label: 'Card number', half: true, autocomplete: 'off' },
  { key: 'cvv', label: 'CVV', type: 'password', half: true, autocomplete: 'off' },
  { key: 'expiry_month', label: 'Expiry month', type: 'number', half: true },
  { key: 'expiry_year', label: 'Expiry year (YY)', type: 'number', half: true },
  { key: 'linked_account_id', label: 'Linked account', type: 'select', numeric: true, options: accountOptions(true), half: true },
  { key: 'currency_code', label: 'Currency', type: 'select', options: curOpts(), half: true },
  { key: 'card_finish', label: 'Finish', type: 'select', options: L.CARD_FINISHES.map((f) => [f, f.replace('_', ' ')]), half: true },
  { key: 'daily_spend_limit', label: 'Daily spend limit', type: 'money', half: true },
  { type: 'section', label: 'Credit card details' },
  { key: 'credit_limit', label: 'Credit limit', type: 'money', half: true },
  { key: 'current_credit_used', label: 'Current balance used', type: 'money', half: true },
  { key: 'statement_balance', label: 'Statement balance', type: 'money', half: true },
  { key: 'minimum_payment_due', label: 'Minimum due', type: 'money', half: true },
  { key: 'statement_day', label: 'Statement day', type: 'number', half: true },
  { key: 'payment_due_day', label: 'Payment due day', type: 'number', half: true },
  { key: 'apr_interest_rate', label: 'APR %', type: 'number', half: true },
  { key: 'annual_fee', label: 'Annual fee', type: 'money', half: true },
  { key: 'rewards_program', label: 'Rewards program' },
  { key: 'notes', label: 'Notes', type: 'textarea' },
];
const numDefaults = (v) => { for (const k of ['daily_spend_limit', 'credit_limit', 'current_credit_used', 'statement_balance', 'minimum_payment_due', 'apr_interest_rate', 'annual_fee']) v[k] = v[k] ?? 0; v.expiry_month = v.expiry_month ?? 12; v.expiry_year = v.expiry_year ?? 28; v.cvv = v.cvv ?? ''; v.issuing_bank = v.issuing_bank ?? ''; v.cardholder_name = v.cardholder_name ?? ''; v.card_number = v.card_number ?? ''; v.is_virtual = v.card_type === 'VIRTUAL' ? 1 : 0; return v; };
const cards = {
  title: 'Cards',
  render() {
    const list = store.all('payment_cards').sort((a, b) => a.created_at - b.created_at);
    const credit = list.filter((c) => c.card_type === 'CREDIT');
    const used = credit.reduce((s, c) => s + c.current_credit_used, 0), limit = credit.reduce((s, c) => s + c.credit_limit, 0);
    return `<div class="page-head"><h1>Cards</h1>${plus('Card', 'add')}</div>
      <div class="kpis big">${kpi('Cards', String(list.length))}${kpi('Credit used', money(used), 'neg')}${kpi('Available credit', money(Math.max(0, limit - used)), 'pos')}</div>
      <div class="cards-grid">${list.length ? list.map((c) => {
        const [c1, c2] = FINISH[c.card_finish] || FINISH.OBSIDIAN_GOLD, show = revealed.has(c.id), digits = String(c.card_number || '').replace(/\D/g, '');
        const brand = detectBrand(c.issuing_bank, c.card_name), bg = brand ? `linear-gradient(135deg,${brand.colors.join(',')})` : `linear-gradient(135deg,${c1},${c2})`;
        const sha = logoSha(brand);
        const title = brand ? `<span class="pc-brand">${sha ? `<img class="brand-logo" data-sha="${sha}" alt="" width="34" height="34">` : ''}<span class="pc-word" style="color:${brand.ink}">${esc(brand.wordmark)}</span></span>` : `<span class="pc-word">${esc(c.issuing_bank || c.card_name)}</span>`;
        const num = show && digits ? digits.match(/.{1,4}/g).join('  ') : `••••  ••••  ••••  ${L.lastFour(c)}`;
        const util = c.credit_limit > 0 ? c.current_credit_used / c.credit_limit : 0;
        return `<div class="pay-card-wrap"><div class="pay-card ${c.is_frozen ? 'frozen' : ''} ${brand ? 'brand-' + brand.id : ''}" style="background:${bg};${brand ? `color:${brand.ink}` : ''}">
          <div class="pc-top">${title}<small class="pc-type">${c.is_virtual ? 'VIRTUAL ' : ''}${esc(c.card_type)}</small></div>
          <div class="pc-chip"></div>
          <div class="pc-num">${esc(num)}</div>
          <div class="pc-bot"><span>${esc((c.cardholder_name || '').toUpperCase())}<br><small>EXP ${String(c.expiry_month).padStart(2, '0')}/${String(c.expiry_year % 100).padStart(2, '0')}${show && c.cvv ? ` · CVV ${esc(c.cvv)}` : ''}</small></span>${networkMark(c.card_network)}</div>
          ${c.is_frozen ? '<div class="pc-frozen">Frozen</div>' : ''}</div>
          ${c.card_type === 'CREDIT' ? `<div class="pc-meta">${progressBar(util)}<small class="muted">${money(c.current_credit_used, c.currency_code)} of ${money(c.credit_limit, c.currency_code)} · min due ${money(c.minimum_payment_due, c.currency_code)}${c.payment_due_day ? ' · due day ' + c.payment_due_day : ''}</small></div>`
            : `<div class="pc-meta"><small class="muted">${esc(L.CARD_TYPES.find((t) => t[0] === c.card_type)?.[1] || c.card_type)} · ${c.linked_account_id ? 'linked to ' + esc(accName(c.linked_account_id)) : 'no linked account'}</small></div>`}
          <div class="row"><button class="btn small" data-action="reveal" data-id="${c.id}">${show ? 'Hide' : 'Reveal'}</button><button class="btn small" data-action="charge" data-id="${c.id}">Log charge</button>
          ${c.card_type === 'CREDIT' ? `<button class="btn small" data-action="payBill" data-id="${c.id}">Pay bill</button>` : ''}
          <button class="btn small ghost" data-action="freeze" data-id="${c.id}">${c.is_frozen ? 'Unfreeze' : 'Freeze'}</button><button class="icon-btn" data-action="edit" data-id="${c.id}">${svg('edit')}</button><button class="icon-btn" data-action="del" data-id="${c.id}">${svg('delete')}</button></div></div>`;
      }).join('') : empty('No cards saved')}</div>`;
  },
  actions: {
    add: () => formModal('New card', cardFields(), { card_type: 'DEBIT', card_network: 'VISA', currency_code: L.base(), card_finish: 'OBSIDIAN_GOLD', expiry_month: 12, expiry_year: 28 }, { wide: true, onSubmit: (v) => { store.insert('payment_cards', { ...numDefaults(v), created_at: Date.now() }); toast('Card saved'); } }),
    edit: (el) => { const c = store.get('payment_cards', el.dataset.id); return formModal('Edit card', cardFields(), c, { wide: true, onSubmit: (v) => { store.update('payment_cards', c.id, numDefaults(v)); toast('Card updated'); } }); },
    del: async (el) => { if (await confirmDialog('Delete card?', 'This removes it from every synced device.')) { store.remove('payment_cards', el.dataset.id); toast('Card deleted'); } },
    reveal: async (el) => { const id = Number(el.dataset.id); if (revealed.has(id)) revealed.delete(id); else {
      if (lock.enabled && lock.requireForCards && !(await askPin({ title: 'Reveal card details', sub: 'Enter your PIN to show the full number and CVV' }))) return;
      revealed.add(id); setTimeout(() => { revealed.delete(id); store.emit(); }, 30000); } store.emit(); },
    freeze: (el) => { const c = store.get('payment_cards', el.dataset.id); store.update('payment_cards', c.id, { is_frozen: c.is_frozen ? 0 : 1 }); },
    charge: (el) => { const c = store.get('payment_cards', el.dataset.id); return formModal(`Log charge — ${c.card_name}`, [{ key: 'amount', label: 'Amount', type: 'money', required: true, half: true }, { key: 'category', label: 'Category', type: 'select', numeric: true, options: categoryOptions('Expense'), half: true }, { key: 'note', label: 'Merchant / note' }, { key: 'account', label: c.card_type === 'CREDIT' ? 'Also record in account (optional)' : 'Account (defaults to linked)', type: 'select', numeric: true, options: accountOptions(true) }], {}, { onSubmit: (v) => { L.logCardCharge(c, v.amount, v.category, v.note, v.account); toast('Charge logged'); } }); },
    payBill: (el) => { const c = store.get('payment_cards', el.dataset.id); return formModal(`Pay bill — ${c.card_name}`, [{ key: 'amount', label: 'Payment amount', type: 'money', required: true }, { key: 'account', label: 'Pay from', type: 'select', numeric: true, options: accountOptions(), required: true }, { key: 'note', label: 'Note' }], { amount: c.statement_balance || c.current_credit_used }, { onSubmit: (v) => { L.payCardBill(c, v.amount, v.account, v.note); toast('Payment recorded'); } }); },
  },
};

// ---------------------------------------------------------------- ANALYTICS (mirrors AnalyticsScreen tabs)
let anTab = 'calendar', anRange = 'month', anSel = null;
const anMonth = { y: new Date().getFullYear(), m: new Date().getMonth() };
const PALETTE = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#e87ba4', '#008300', '#6250d6', '#e34948'];
function monthPicker() {
  const label = new Date(anMonth.y, anMonth.m, 1).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  return `<div class="month-pick"><button class="icon-btn" data-action="month" data-d="-1" aria-label="Previous month">‹</button><strong>${esc(label)}</strong><button class="icon-btn" data-action="month" data-d="1" aria-label="Next month">›</button></div>`;
}
function calendarTab() {
  const { y, m } = anMonth, txs = L.monthTxs(y, m), dim = new Date(y, m + 1, 0).getDate(), first = new Date(y, m, 1).getDay();
  const daily = new Map(); for (const t of txs) { const d = new Date(t.date).getDate(), c = daily.get(d) || { inc: 0, exp: 0 }; if (t.type === 'Income') c.inc += t.amount; if (t.type === 'Expense') c.exp += t.amount; daily.set(d, c); }
  const maxExp = Math.max(1, ...[...daily.values()].map((v) => v.exp));
  const rec = new Map(); for (const t of L.recurringTemplates()) for (const d of L.occurrencesInMonth(t.recurrence_rule, t.date, m, y)) { if (!rec.has(d)) rec.set(d, []); rec.get(d).push(t); }
  const onDay = (ms) => { if (!ms) return 0; const d = new Date(ms); return d.getMonth() === m && d.getFullYear() === y ? d.getDate() : 0; };
  const debtsD = new Map(); for (const d of store.all('debts').filter((x) => x.status === 'Active')) { const k = onDay(d.due_date); if (k) { if (!debtsD.has(k)) debtsD.set(k, []); debtsD.get(k).push(d); } }
  const msD = new Map(); for (const x of store.all('milestones').filter((x) => x.status !== 'Paid')) { const k = onDay(x.expected_date); if (k) { if (!msD.has(k)) msD.set(k, []); msD.get(k).push(x); } }
  const today = new Date(); const isToday = (d) => today.getDate() === d && today.getMonth() === m && today.getFullYear() === y;
  const inc = txs.filter((t) => t.type === 'Income').reduce((s, t) => s + t.amount, 0), exp = txs.filter((t) => t.type === 'Expense').reduce((s, t) => s + t.amount, 0);
  const cells = [...Array(first).fill('<div class="hm-cell blank"></div>'), ...Array.from({ length: dim }, (_, i) => {
    const d = i + 1, v = daily.get(d), heat = v ? Math.min(1, v.exp / maxExp) : 0;
    return `<button class="hm-cell ${anSel === d ? 'sel' : ''} ${isToday(d) ? 'today' : ''}" data-action="day" data-d="${d}" style="--heat:${heat.toFixed(3)}">
      <span class="hm-d">${d}</span>${v?.exp ? `<span class="hm-v neg">${money(v.exp).replace(/\.\d\d$/, '')}</span>` : ''}${v?.inc ? `<span class="hm-v pos">+${money(v.inc).replace(/\.\d\d$/, '')}</span>` : ''}
      <span class="hm-marks">${rec.has(d) ? '<i class="mk rec" title="Recurring"></i>' : ''}${debtsD.has(d) ? '<i class="mk debt" title="Debt due"></i>' : ''}${msD.has(d) ? '<i class="mk ms" title="Milestone"></i>' : ''}</span></button>`;
  })].join('');
  let detail = '';
  if (anSel) {
    const dayTx = txs.filter((t) => new Date(t.date).getDate() === anSel);
    detail = `<div class="card"><h3>${fmtDate(+new Date(y, m, anSel), { weekday: 'long', month: 'long', day: 'numeric' })}</h3>
      ${dayTx.length ? `<div class="list">${dayTx.map((t) => txRow(t)).join('')}</div>` : '<p class="muted">No cleared transactions.</p>'}
      ${(rec.get(anSel) || []).map((t) => `<div class="mini-row"><span><small class="badge">Recurring</small> ${esc(t.note || 'Recurring')}</span><strong class="${t.type === 'Income' ? 'pos' : 'neg'}">${money(t.type === 'Income' ? t.amount : -t.amount, L.base(), { sign: true })}</strong></div>`).join('')}
      ${(debtsD.get(anSel) || []).map((d) => `<div class="mini-row"><span><small class="badge ${d.type === 'Lent' ? 'pos' : 'neg'}">Debt due</small> ${esc(d.person_name)}</span><strong>${money(d.remaining_amount)}</strong></div>`).join('')}
      ${(msD.get(anSel) || []).map((x) => `<div class="mini-row"><span><small class="badge pos">Milestone</small> ${esc(x.title)}</span><strong class="pos">${money(x.amount)}</strong></div>`).join('')}</div>`;
  }
  return `<div class="kpis big">${kpi('Income', money(inc), 'pos')}${kpi('Expenses', money(exp), 'neg')}${kpi('Net savings', money(inc - exp, L.base(), { sign: true }), inc - exp >= 0 ? 'pos' : 'neg')}${kpi('Logged transactions', String(txs.length))}</div>
    <div class="card"><h3>Daily cash activity heatmap</h3>
      <div class="hm-grid">${['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => `<div class="hm-h">${d}</div>`).join('')}${cells}</div>
      <div class="legend"><span><i class="mk rec"></i>Recurring</span><span><i class="mk debt"></i>Debt due</span><span><i class="mk ms"></i>Milestone</span><span>Darker = more spending</span></div></div>
    ${detail}`;
}
function forecastTab() {
  const f = L.monthEndForecast(anMonth.y, anMonth.m);
  const row = (label, v, cls = '') => `<div class="mini-row"><span>${esc(label)}</span><strong class="${cls}">${money(v, L.base(), { sign: true })}</strong></div>`;
  return `<section class="hero slim"><div><span class="eyebrow">Month-end forecast · day ${f.daysInMonth}</span><h1 class="hero-num">${money(f.projected)}</h1>
      <div class="hero-sub"><span>Estimated cash balance accounting for spending velocity, recurring bills, freelance milestones & debt payouts</span></div></div></section>
    <div class="kpis big">${kpi('Liquid balance now', money(f.liquid))}${kpi('Daily burn rate', money(f.burn), 'neg', `${money(f.spent)} over ${f.currentDay} day(s)`)}${kpi('Safe to spend / day', money(f.safeDaily), 'pos', `${f.daysRemaining} day(s) left`)}</div>
    <div class="grid">
      <div class="card"><h3>Cashflow velocity & commitments</h3>
        ${row('Liquid balance', f.liquid)}${row(`Projected spending (${f.daysRemaining} days × burn)`, -f.burn * f.daysRemaining, 'neg')}
        ${row('Remaining recurring income', f.recurringIn, 'pos')}${row('Remaining recurring bills', -f.recurringOut, 'neg')}
        ${row('Debts owed to you, due this month', f.debtIn, 'pos')}${row('Debts you owe, due this month', -f.debtOut, 'neg')}${row('Freelance milestones due', f.freelanceIn, 'pos')}
        <div class="mini-row total"><span>Projected month-end</span><strong>${money(f.projected)}</strong></div></div>
      <div class="card"><h3>Scheduled recurring items</h3>
        ${[...f.recOut, ...f.recIn].length ? [...f.recOut, ...f.recIn].map(({ t, amt }) => `<div class="mini-row"><span>${esc(t.note || 'Recurring')}<br><small class="muted">${esc(L.scheduleLabel(t.recurrence_rule, t.date))}</small></span><strong class="${t.type === 'Income' ? 'pos' : 'neg'}">${money(t.type === 'Income' ? amt : -amt, L.base(), { sign: true })}</strong></div>`).join('') : empty('Nothing left this month')}</div>
      <div class="card"><h3>Expected debt payouts / due dates</h3>
        ${[...f.lent, ...f.borrowed].length ? [...f.lent, ...f.borrowed].map((d) => `<div class="mini-row"><span>${esc(d.person_name)} <small class="badge ${d.type === 'Lent' ? 'pos' : 'neg'}">${d.type === 'Lent' ? 'owes you' : 'you owe'}</small><br><small class="muted">Due ${fmtDate(d.due_date)}</small></span><strong class="${d.type === 'Lent' ? 'pos' : 'neg'}">${money(d.remaining_amount)}</strong></div>`).join('') : empty('No debts due this month')}</div>
      <div class="card"><h3>Expected freelance payouts (milestones)</h3>
        ${f.milestones.length ? f.milestones.map((x) => `<div class="mini-row"><span>${esc(x.title)}<br><small class="muted">${esc(store.get('freelance_projects', x.project_id)?.project_name || '')} · ${fmtDate(x.expected_date)}</small></span><strong class="pos">${money(x.amount)}</strong></div>`).join('') : empty('No milestones due this month')}</div>
    </div>`;
}
function shiftsTab() {
  const rows = L.categoryShifts(anMonth.y, anMonth.m);
  return `<div class="card"><h3>Month-over-month spending shifts</h3>
    ${rows.length ? `<div class="list">${rows.map((r) => `<div class="mini-row"><span>${L.icon(r.category?.icon_res_id)} ${esc(r.category?.name || 'Deleted category')}<br><small class="muted">${money(r.previous)} → ${money(r.current)}</small></span>
      <strong class="${r.delta > 0 ? 'neg' : 'pos'}">${r.delta > 0 ? '▲' : r.delta < 0 ? '▼' : '•'} ${r.delta > 0 ? '+' : ''}${r.pct.toFixed(1)}%<br><small>${money(r.delta, L.base(), { sign: true })}</small></strong></div>`).join('')}</div>`
      : empty('No category expense history available for comparison.')}</div>`;
}
function healthTab() {
  const h = L.healthScore(anMonth.y, anMonth.m), cls = h.total >= 70 ? 'pos' : h.total >= 50 ? 'warn' : 'neg';
  const pillar = (label, score, max, sub) => `<div class="pillar"><div class="mini-row"><span>${esc(label)}<br><small class="muted">${esc(sub)}</small></span><strong>${score} / ${max} pts</strong></div>${progressBar(score / max, 2, { good: true })}</div>`;
  return `<div class="grid"><div class="card score-card ${cls}"><div class="score-ring" style="--p:${h.total}"><span>${h.total}</span><small>/ 100</small></div>
      <div><h2>Financial Health: ${esc(h.grade)}</h2><p class="muted">Offline algorithmic score from savings, budgets, emergency buffer and debt burden.</p></div></div>
    <div class="card"><h3>Health pillars breakdown</h3>
      ${pillar('Savings rate', h.savings, 30, `${h.savingsRate.toFixed(1)}% of this month's income saved`)}
      ${pillar('Budget discipline', h.budget, 25, store.all('budgets').length ? 'Budgets kept within their limits' : 'No budgets set (default points)')}
      ${pillar('Emergency buffer', h.emergency, 25, `${h.runway.toFixed(1)} months of runway in liquid accounts`)}
      ${pillar('Debt burden', h.debt, 20, h.activeDebts ? `${h.debtRatio.toFixed(1)}% of net cash owed` : 'No active borrowed debt')}</div>
    <div class="card span2"><h3>Smart financial recommendations</h3>${h.tips.length ? h.tips.map((t) => `<p class="tip">• ${esc(t)}</p>`).join('') : `<p class="pos">${svg('done')} Everything looks healthy — keep it up!</p>`}</div></div>`;
}
function overviewTab() {
  const n = new Date(); let from, to = Date.now();
  if (anRange === 'month') from = +new Date(n.getFullYear(), n.getMonth(), 1);
  else if (anRange === 'last') { from = +new Date(n.getFullYear(), n.getMonth() - 1, 1); to = +new Date(n.getFullYear(), n.getMonth(), 1) - 1; }
  else if (anRange === '3m') from = +new Date(n.getFullYear(), n.getMonth() - 2, 1);
  else from = +new Date(n.getFullYear(), 0, 1);
  const cats = L.spendingByCategory(from, to), total = cats.reduce((s, c) => s + c.total, 0);
  const top = cats.slice(0, 7), other = cats.slice(7).reduce((s, c) => s + c.total, 0);
  const items = [...top.map((c, i) => ({ label: c.category?.name || 'Uncategorized', value: c.total, color: PALETTE[i] })), ...(other ? [{ label: 'Other', value: other, color: '#9aa0a6' }] : [])];
  const flow = L.moneyFlow(from, to), days = Math.max(1, Math.ceil((to - from) / 864e5));
  return `<div class="tabs">${[['month', 'This month'], ['last', 'Last month'], ['3m', '3 months'], ['year', 'This year']].map(([k, l]) => `<button class="tab ${k === anRange ? 'on' : ''}" data-action="range" data-r="${k}">${l}</button>`).join('')}</div>
    <div class="kpis big">${kpi('Income', money(flow.income), 'pos')}${kpi('Expenses', money(flow.expense), 'neg')}${kpi('Savings rate', flow.income > 0 ? `${Math.round((flow.net / flow.income) * 100)}%` : '—')}${kpi('Avg daily spend', money(flow.expense / days))}</div>
    <div class="grid"><div class="card"><h3>Spending by category</h3><div class="donut-wrap">${donut(items)}
      <div class="legend-list">${items.map((i) => `<div class="mini-row"><span><i class="sw" style="background:${i.color}"></i>${esc(i.label)}</span><strong>${money(i.value)} <small class="muted">${total ? Math.round((i.value / total) * 100) : 0}%</small></strong></div>`).join('')}</div></div></div>
    <div class="card"><h3>Income vs expenses · 12 months</h3>${barPairs(L.monthlyTrends(12), { fmt: (v) => money(v) })}</div></div>`;
}
const AN_TABS = [['calendar', 'Calendar & Flow', calendarTab], ['forecast', 'Cashflow Forecast', forecastTab], ['shifts', 'Category Shifts', shiftsTab], ['health', 'Health Score', healthTab], ['overview', 'Spending Overview', overviewTab]];
const analytics = {
  title: 'Analytics',
  render() {
    const tab = AN_TABS.find((t) => t[0] === anTab) || AN_TABS[0];
    return `<div class="page-head"><h1>Analytics & forecast</h1>${anTab !== 'overview' ? monthPicker() : ''}</div>
      <div class="tabs">${AN_TABS.map(([k, l]) => `<button class="tab ${k === anTab ? 'on' : ''}" data-action="tab" data-t="${k}">${l}</button>`).join('')}</div>
      ${tab[2]()}`;
  },
  actions: {
    tab: (el) => { anTab = el.dataset.t; store.emit(); },
    range: (el) => { anRange = el.dataset.r; store.emit(); },
    month: (el) => { const d = new Date(anMonth.y, anMonth.m + Number(el.dataset.d), 1); anMonth.y = d.getFullYear(); anMonth.m = d.getMonth(); anSel = null; store.emit(); },
    day: (el) => { const d = Number(el.dataset.d); anSel = anSel === d ? null : d; store.emit(); },
    editTx: (el) => openTxForm(store.get('transactions', el.dataset.id)),
  },
};

// ---------------------------------------------------------------- RECEIPTS
const receipts = {
  title: 'Receipts',
  render() {
    const list = store.all('receipts_inbox').sort((a, b) => b.captured_at - a.captured_at);
    const pending = list.filter((r) => r.status === 'PENDING');
    return `<div class="page-head"><div><h1>Receipts inbox</h1><p class="muted">Snap receipts fast, review & classify at your own pace. Photos sync between the phone and the web.</p></div>
      <label class="btn primary">${svg('camera')}Add receipt photo<input type="file" accept="image/*" capture="environment" hidden data-action-change="addPhoto"></label></div>
      <div class="list">${list.length ? list.map((r) => `<div class="acc-row">${isSha(r.image_sha256) ? `<button class="thumb" data-photo="${r.image_sha256}" data-title="${esc(r.note || 'Receipt')}"><img data-sha="${r.image_sha256}" alt="Receipt"></button>` : `<span class="cat-ico" title="${r.image_path ? 'Photo stored on the phone only' : 'No photo'}">${r.status === 'PENDING' ? svg('ic_bills') : r.status === 'CONVERTED' ? svg('done') : svg('delete')}</span>`}
        <div class="acc-main"><strong>${esc(r.note || 'Receipt')}</strong><small>${fmtDateTime(r.captured_at)} · ${esc(r.status)}${r.suggested_amount != null ? ' · ' + money(r.suggested_amount) : ''}</small></div>
        <div class="row-actions">${r.status === 'PENDING' ? `<button class="btn small" data-action="convert" data-id="${r.id}">Convert</button><button class="btn small ghost" data-action="discard" data-id="${r.id}">Discard</button>` : ''}
        <button class="icon-btn" data-action="del" data-id="${r.id}">${svg('delete')}</button></div></div>`).join('') : empty('No receipts. Snap them from the phone app.')}</div>
      ${pending.length ? `<p class="muted center">${pending.length} waiting for review</p>` : ''}`;
  },
  actions: {
    convert: (el) => { const r = store.get('receipts_inbox', el.dataset.id); return formModal('Convert receipt', [
      { key: 'type', label: 'Type', type: 'select', options: [['Expense', 'Expense'], ['Income', 'Income']], half: true }, { key: 'amount', label: 'Amount', type: 'money', required: true, half: true },
      { key: 'accountId', label: 'Account', type: 'select', numeric: true, options: accountOptions(), required: true, half: true }, { key: 'categoryId', label: 'Category', type: 'select', numeric: true, options: categoryOptions(null), half: true },
      { key: 'date', label: 'Date', type: 'datetime', half: true }, { key: 'note', label: 'Note' }],
      { type: r.suggested_type, amount: r.suggested_amount, date: r.captured_at, note: r.note, accountId: store.all('accounts')[0]?.id },
      { onSubmit: (v) => { L.convertReceipt(r, v); toast('Converted to a transaction'); } }); },
    discard: (el) => { store.update('receipts_inbox', el.dataset.id, { status: 'DISCARDED' }); toast('Discarded'); },
    del: async (el) => { if (await confirmDialog('Delete receipt record?', 'This removes the receipt (and its photo link) on all devices.')) { store.remove('receipts_inbox', el.dataset.id); toast('Deleted'); } },
  },
  changes: {
    addPhoto: async (el) => {
      const f = el.files?.[0]; el.value = ''; if (!f) return;
      let sha; try { toast('Uploading…'); sha = await uploadPhoto(f); } catch (x) { return toast(x.message, 'err'); }
      await formModal('New receipt', [{ key: 'note', label: 'Merchant / note' }, { key: 'suggested_amount', label: 'Amount (optional)', type: 'money', half: true }, { key: 'suggested_type', label: 'Type', type: 'select', options: [['Expense', 'Expense'], ['Income', 'Income']], half: true }], { suggested_type: 'Expense' },
        { submitLabel: 'Save to inbox', onSubmit: (v) => { store.insert('receipts_inbox', { ...v, image_path: '', image_sha256: sha, captured_at: Date.now(), status: 'PENDING' }); toast('Receipt saved'); } });
    },
  },
};

// ---------------------------------------------------------------- SETTINGS
let devicesCache = null;
let serverInfo = null; // { app, invite }
function signupCard() {
  const inv = serverInfo?.invite;
  if (!serverInfo?.me?.admin) {
    return `<div class="card"><h3>${svg('people')}Sign-ups</h3><p class="muted">New accounts are invite-only. Ask the person who invited you, or contact ATEN Systems, for an invite code.</p></div>`;
  }
  const used = inv ? Math.min(1, inv.users / Math.max(1, inv.maxUsers)) : 0;
  return `<div class="card signup"><h3>${svg('people')}Sign-ups <span class="badge ${inv?.inviteCode ? 'pos' : ''}">${inv?.inviteCode ? 'Open' : 'Closed'}</span></h3>
    <p class="muted">Anyone with the invite code can create their own account on the website. Their data is separate from yours.</p>
    ${inv ? `<div class="signup-meter"><div class="progress"><i style="width:${used * 100}%"></i></div><small class="muted">${inv.users} of ${inv.maxUsers} accounts used</small></div>` : ''}
    ${inv?.inviteCode ? `<div class="invite-code"><span class="mono">${esc(inv.inviteCode)}</span><button class="icon-btn" data-action="copyInvite" title="Copy code" aria-label="Copy invite code">${svg('copy')}</button></div>` : ''}
    ${inv?.fromEnv ? '<p class="muted">The invite code is fixed in the server configuration.</p>' : `<div class="row"><button class="btn ${inv?.inviteCode ? '' : 'primary'}" data-action="newInvite">${inv?.inviteCode ? 'New code' : 'Open sign-up'}</button>${inv?.inviteCode ? '<button class="btn danger ghost" data-action="closeInvite">Close sign-up</button>' : ''}</div>`}</div>`;
}
const settings = {
  title: 'Settings',
  render() {
    const s = store.status, a = store.auth;
    const rates = store.all('exchange_rates').sort((x, y) => x.from_currency.localeCompare(y.from_currency));
    if (!devicesCache) settings.loadDevices();
    const theme = themePrefs();
    return `<div class="page-head"><h1>Settings</h1></div>
    <div class="grid">
      <div class="card"><h3>Sync</h3>
        <p>Signed in as <strong>${esc(a.username)}</strong></p>
        <p class="muted">${s.syncing ? 'Syncing…' : s.error ? `${svg('warn')} ${esc(s.error)}` : `Last sync ${s.lastSync ? fmtDateTime(s.lastSync) : 'never'}`} · ${store.pending.size} pending change(s)</p>
        <div class="row"><button class="btn primary" data-action="syncNow">Sync now</button><button class="btn" data-action="resetCache">Refresh all data</button><button class="btn danger ghost" data-action="signOut">Sign out</button></div></div>
      <div class="card"><h3>Display</h3>
        <label class="field"><span>Base currency (synced to all devices)</span><select data-action-change="baseCurrency">${L.CURRENCIES.map((c) => `<option value="${c.code}" ${c.code === L.base() ? 'selected' : ''}>${c.code} — ${esc(c.name)}</option>`).join('')}</select></label>
        <label class="field"><span>Theme mode (synced with your phone)</span><select data-action-change="theme">${THEME_MODES.map(([v, l]) => `<option value="${v}" ${v === theme.mode ? 'selected' : ''}>${l}</option>`).join('')}</select></label>
        <label class="check"><input type="checkbox" data-action-change="includeAssets" ${viewPrefs.get('includeAssets', false) ? 'checked' : ''}> Include assets in hero total</label>
        <label class="check"><input type="checkbox" data-action-change="hideMoney" ${viewPrefs.get('hideMoney', false) ? 'checked' : ''}> Hide amounts (privacy mask)</label>
        <a class="btn" href="#/customize">${svg('tune')}Customize dashboard cards</a></div>
      <div class="card span2"><h3>Theme palette <small class="muted">· synced with your phone</small></h3>${theme.preset === 'MATERIAL_YOU' ? '<p class="muted">Your phone uses Material You (wallpaper colours); the web shows Emerald Aurora for it.</p>' : ''}<div class="swatches">${PALETTES.map(([k, l, d]) => `<button class="swatch ${theme.preset === k ? 'on' : ''}" data-action="palette" data-k="${k}" data-hero="${PALETTES.find((p) => p[0] === k)[3]}"><i class="pal-${k}"></i><strong>${esc(l)}</strong><small>${esc(d)}</small></button>`).join('')}</div>
        <h3>Hero card atmosphere</h3><div class="swatches">${HERO_STYLES.map(([k, l]) => `<button class="swatch ${theme.hero === k ? 'on' : ''}" data-action="hero" data-k="${k}"><i class="hero-sw-${k}"></i><strong>${esc(l)}</strong></button>`).join('')}</div>
        </div>
      <div class="card"><h3>App lock (this browser)</h3>
        ${lock.enabled ? `<p class="muted">${svg('lock')} Locked with a 4-digit PIN. Wrong PINs trigger cooldowns; 10 in a row signs this browser out.</p>
          <label class="field"><span>Lock again</span><select data-action-change="lockTimeout">${TIMEOUT_OPTIONS.map(([v, l]) => `<option value="${v}" ${v === lock.timeoutMode ? 'selected' : ''}>${esc(l)}</option>`).join('')}</select><small>Also locks after 10 minutes without activity.</small></label>
          <label class="check"><input type="checkbox" data-action-change="lockCards" ${lock.requireForCards ? 'checked' : ''}> Require PIN to reveal card numbers & CVV</label>
          <div class="row"><button class="btn" data-action="lockNow">Lock now</button><button class="btn" data-action="changePin">Change PIN</button><button class="btn danger ghost" data-action="disableLock">Turn off</button></div>`
        : `<p class="muted">Protect this browser with a 4-digit PIN, like the phone app's security lock. Recommended — card numbers and CVVs are visible here.</p>
          <button class="btn primary" data-action="enableLock">Set up PIN lock</button>`}</div>
      <div class="card span2"><h3>Exchange rates <span class="row-actions"><button class="btn small" data-action="addRate">${svg('add')}Rate</button></span></h3>
        <div class="mini-row"><span>Live market rates · ${L.liveRates.fetchedAt ? `1 ${L.base() === 'USD' ? 'EUR' : 'USD'} = <strong>${money(L.convert(1, L.base() === 'USD' ? 'EUR' : 'USD', L.base()), L.base(), { hideable: false })}</strong> <small class="muted">updated ${fmtDateTime(L.liveRates.updatedAt || L.liveRates.fetchedAt)}</small>` : '<span class="muted">not downloaded yet</span>'}<br><small class="muted">Rates by <a href="https://www.exchangerate-api.com" target="_blank" rel="noopener">Exchange Rate API</a></small></span>
          <span class="row-actions"><button class="btn small" data-action="refreshRates">Refresh</button></span></div>
        <p class="muted">Custom rates below take priority over live rates.</p>
        <div class="list">${rates.length ? rates.map((r) => `<div class="mini-row"><span>1 ${esc(r.from_currency)} = <strong>${r.rate}</strong> ${esc(r.to_currency)} <small class="muted">updated ${fmtDate(r.last_updated)}</small></span>
          <span class="row-actions"><button class="icon-btn" data-action="editRate" data-id="${r.id}">${svg('edit')}</button><button class="icon-btn" data-action="delRate" data-id="${r.id}">${svg('delete')}</button></span></div>`).join('') : empty('No custom rates')}</div></div>
      <div class="card"><h3>Devices</h3><div class="list">${devicesCache ? devicesCache.devices.map((d) => `<div class="mini-row"><span>${esc(d.name || 'Device')} #${d.device_no}${d.device_no === devicesCache.current ? ' <small class="badge">this one</small>' : ''}<br>
        <small class="muted">${d.revoked ? 'Signed out' : 'Last seen ' + fmtDateTime(d.last_seen)}</small></span>${!d.revoked && d.device_no !== devicesCache.current ? `<button class="btn small ghost danger" data-action="revoke" data-id="${d.device_no}">Sign out</button>` : ''}</div>`).join('') : '<p class="muted">Loading…</p>'}</div></div>
      <div class="card"><h3>Security & data</h3>
        <div class="row"><button class="btn" data-action="changePassword">Change password</button><button class="btn" data-action="exportJson">Export JSON</button><button class="btn" data-action="exportCsv">Export CSV</button><label class="btn">Import JSON…<input type="file" accept=".json,application/json" data-action-change="importFile" hidden></label></div>
        <p class="muted">Import accepts the phone app's JSON backup or this web app's export.</p>
        <p class="muted">JSON export contains every record (including card numbers) — store it safely.</p></div>
    </div>`;
  },
  async loadServerInfo() {
    const { api } = await import('./store.js?v=18');
    const tok = store.auth.token;
    try {
      const [status, me] = await Promise.all([api('/api/status'), api('/api/me', { token: tok })]);
      const invite = me.admin ? await api('/api/admin/invite', { token: tok }).catch(() => null) : null;
      serverInfo = { app: status.app, me, invite };
    } catch { serverInfo = { app: null, me: {}, invite: null }; }
    store.emit();
  },
  async loadDevices() {
    try { const { api } = await import('./store.js?v=18'); devicesCache = await api('/api/devices', { token: store.auth.token }); store.emit(); } catch { devicesCache = { devices: [], current: 0 }; }
  },
  actions: {
    palette: (el) => { store.setPrefs({ app_theme_preset: el.dataset.k, hero_background_style: el.dataset.hero }); applyTheme(); },
    hero: (el) => { store.setPref('hero_background_style', el.dataset.k); applyTheme(); },
    enableLock: async () => { const pin = await choosePin(); if (!pin) return; await lock.setPin(pin); lock.setOption('cards', 1); toast('PIN lock enabled'); store.emit(); },
    changePin: async () => { if (!(await askPin({ title: 'Current PIN', sub: 'Enter your current PIN first' }))) return; const pin = await choosePin(); if (!pin) return; await lock.setPin(pin); toast('PIN changed'); },
    disableLock: async () => { if (!(await askPin({ title: 'Turn off app lock', sub: 'Enter your PIN to confirm' }))) return; lock.disable(); toast('App lock turned off'); store.emit(); },
    lockNow: () => lock.lockNow(),
    syncNow: async () => { try { await store.sync(); toast('Synced'); } catch (e) { toast(e.message, 'err'); } },
    resetCache: async () => { if (await confirmDialog('Refresh all data?', 'Clears this browser\'s copy and downloads all data from the server again. Unsynced changes in this browser are lost.', 'Re-download', 'primary')) { await store.resetLocalCache(); toast('Re-downloaded'); } },
    signOut: async () => { if (await confirmDialog('Sign out?', 'This browser\'s local copy will be removed.', 'Sign out', 'primary')) store.signOut(); },
    addRate: () => formModal('Exchange rate', [{ key: 'from_currency', label: 'From', type: 'select', options: curOpts(), half: true }, { key: 'to_currency', label: 'To', type: 'select', options: curOpts(), half: true }, { key: 'rate', label: 'Rate', type: 'number', required: true }], { from_currency: 'USD', to_currency: L.base() }, { onSubmit: (v) => {
      if (v.from_currency === v.to_currency) throw new Error('Pick two different currencies');
      if (!(v.rate > 0)) throw new Error('Rate must be positive');
      const dup = store.all('exchange_rates').find((r) => r.from_currency === v.from_currency && r.to_currency === v.to_currency);
      if (dup) store.update('exchange_rates', dup.id, { rate: v.rate, last_updated: Date.now() }); else store.insert('exchange_rates', { ...v, last_updated: Date.now() });
      toast('Rate saved'); } }),
    editRate: (el) => { const r = store.get('exchange_rates', el.dataset.id); return formModal(`1 ${r.from_currency} = ? ${r.to_currency}`, [{ key: 'rate', label: 'Rate', type: 'number', required: true }], r, { onSubmit: (v) => { if (!(v.rate > 0)) throw new Error('Rate must be positive'); store.update('exchange_rates', r.id, { rate: v.rate, last_updated: Date.now() }); toast('Rate updated'); } }); },
    refreshRates: async () => { toast((await L.refreshLiveRates(true)) ? 'Live rates updated' : 'Could not refresh rates'); },
    delRate: async (el) => { if (await confirmDialog('Delete rate?', 'Conversions will use live market rates.')) store.remove('exchange_rates', el.dataset.id); },
    newInvite: async () => { const { api } = await import('./store.js?v=18'); const r = await api('/api/admin/invite', { method: 'POST', body: {}, token: store.auth.token }); serverInfo = { ...serverInfo, invite: r }; toast('New invite code created'); store.emit(); },
    closeInvite: async () => { if (!(await confirmDialog('Close sign-up?', 'The current invite code stops working. Existing accounts are not affected.', 'Close sign-up', 'danger'))) return; const { api } = await import('./store.js?v=18'); const r = await api('/api/admin/invite', { method: 'POST', body: { disable: true }, token: store.auth.token }); serverInfo = { ...serverInfo, invite: r }; toast('Sign-up closed'); store.emit(); },
    revoke: async (el) => { if (await confirmDialog('Sign out that device?', 'It will need the password to sync again.', 'Sign out', 'danger')) { const { api } = await import('./store.js?v=18'); await api(`/api/devices/${el.dataset.id}`, { method: 'DELETE', token: store.auth.token }); devicesCache = null; toast('Device signed out'); store.emit(); } },
    changePassword: async () => { const { api } = await import('./store.js?v=18'); return formModal('Change password', [{ key: 'currentPassword', label: 'Current password', type: 'password', required: true, autocomplete: 'current-password' }, { key: 'newPassword', label: 'New password (10+ characters)', type: 'password', required: true, autocomplete: 'new-password' }], {}, { onSubmit: async (v) => { const r = await api('/api/password', { method: 'POST', body: v, token: store.auth.token }); toast(r.signedOut ? `Password changed. ${r.signedOut} other device(s) signed out.` : 'Password changed'); devicesCache = null; } }); },
    exportJson: async () => {
      toast('Preparing backup…');
      const tables = Object.fromEntries(Object.keys(store.tables).map((t) => [t, store.all(t)]));
      const shas = [...tables.transactions.map((t) => t.proof_sha256), ...tables.receipts_inbox.map((r) => r.image_sha256)];
      const images = await exportPhotos(shas);
      const settings = { ...store.prefs, include_assets_in_hero: !!viewPrefs.get('includeAssets', false), hide_money: !!viewPrefs.get('hideMoney', false) };
      const data = { version: 2, exportedAt: Date.now(), baseCurrency: L.base(), settings, ...tables, images };
      download(`freefinance-${new Date().toISOString().slice(0, 10)}.json`, JSON.stringify(data, null, 2), 'application/json');
      toast(`Backup exported · ${Object.keys(images).length} photo(s) included`);
    },
    exportCsv: () => {
      const q = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`;
      const rows = [['Date', 'Type', 'Amount', 'Currency', 'Account', 'To account', 'Category', 'Status', 'Recurrence', 'Labels', 'Note']].concat(
        store.all('transactions').sort((a, b) => a.date - b.date).map((t) => [new Date(t.date).toISOString(), t.type, t.amount, store.get('accounts', t.account_id)?.currency_code || '', accName(t.account_id), t.to_account_id ? accName(t.to_account_id) : '', catOf(t.category_id)?.name || '', t.status, t.recurrence_rule, L.labelsOf(t).join('; '), t.note || '']));
      download(`freefinance-transactions-${new Date().toISOString().slice(0, 10)}.csv`, rows.map((r) => r.map(q).join(',')).join('\n'), 'text/csv');
    },
  },
  changes: {
    baseCurrency: (el) => { store.setPref('base_currency', el.value); toast(`Base currency: ${el.value}`); },
    theme: (el) => { store.setPref('app_theme_mode', el.value); applyTheme(); },
    includeAssets: (el) => { viewPrefs.set('includeAssets', el.checked); store.emit(); },
    hideMoney: (el) => { viewPrefs.set('hideMoney', el.checked); store.emit(); },
    importFile: async (el) => {
      const file = el.files?.[0]; el.value = ''; if (!file) return;
      let parsed; try { parsed = L.parseBackup(await file.text()); } catch (e) { return toast(e.message, 'err'); }
      const list = Object.entries(parsed.counts).filter(([, n]) => n).map(([t, n]) => `<li>${esc(t.replace(/_/g, ' '))}: <strong>${n}</strong></li>`).join('')
        + (parsed.photoCount ? `<li>photos: <strong>${parsed.photoCount}</strong></li>` : '')
        + (Object.keys(parsed.settings).length ? '<li>settings (currency & theme — applied on Replace)</li>' : '');
      const mode = await modal({ title: 'Import backup', body: `<p>${parsed.fmt === 'android' ? 'Phone app backup' : 'Web export'} · ${esc(file.name)}</p><ul class="import-list">${list}</ul>
        <p class="muted"><strong>Merge</strong> adds these records alongside your current data. <strong>Replace</strong> deletes all current data on <u>every synced device</u> first (like the phone's import).</p>`,
        actions: [{ label: 'Cancel' }, { label: 'Merge', value: 'merge' }, { label: 'Replace everything', kind: 'danger', value: 'replace' }] });
      if (!mode) return;
      if (mode === 'replace' && !(await confirmDialog('Replace all data?', 'Every account, transaction and record will be deleted on all devices and replaced with the file contents.', 'Replace everything'))) return;
      const photos = parsed.photoCount ? await importPhotos(parsed.images) : 0;
      const n = L.importBackup(parsed, mode);
      if (mode === 'replace') {
        if (typeof parsed.settings.include_assets_in_hero === 'boolean') viewPrefs.set('includeAssets', parsed.settings.include_assets_in_hero);
        if (typeof parsed.settings.hide_money === 'boolean') viewPrefs.set('hideMoney', parsed.settings.hide_money);
        applyTheme();
      }
      toast(`Imported ${n} records${photos ? ` · ${photos} photo(s)` : ''}`);
    },
    lockTimeout: (el) => { lock.setOption('timeout', el.value); toast('Saved'); },
    lockCards: (el) => { lock.setOption('cards', el.checked ? 1 : 0); toast('Saved'); },
  },
};
function download(name, text, type) {
  const url = URL.createObjectURL(new Blob([text], { type }));
  const a = Object.assign(document.createElement('a'), { href: url, download: name });
  document.body.appendChild(a); a.click(); a.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// ---------------------------------------------------------------- ABOUT & SUPPORT / GET THE APP (Android-only screens' web equivalents)
let installPrompt = null;
window.addEventListener('beforeinstallprompt', (e) => { e.preventDefault(); installPrompt = e; store.emit(); });
const about = {
  title: 'About',
  render() {
    if (!serverInfo) settings.loadServerInfo();
    const app = serverInfo?.app;
    return `<div class="page-head"><h1>About</h1></div>
    <div class="grid">
      <div class="card span2 about-hero"><span class="logo"></span><div><h2>My Finance</h2><p class="muted">Personal finance for your phone and the web. Accounts, budgets, bills, cards and debts in one place. Local-first, with optional sync.</p>
        <p class="about-meta"><span>By <strong>Rohan Reyes</strong> · ATEN Systems</span>${app?.versionName ? `<span class="badge">Android ${esc(app.versionName)}</span>` : ''}</p></div></div>
      ${signupCard()}
      <div class="card"><h3>This account</h3><div class="mini-row"><span>Signed in as</span><strong>${esc(store.auth.username)}</strong></div>
        <div class="mini-row"><span>Records synced</span><strong>${Object.keys(store.tables).reduce((n, t) => n + store.tables[t].size, 0)}</strong></div>
        <div class="mini-row"><span>Exchange rates</span><small class="muted">by <a href="https://www.exchangerate-api.com" target="_blank" rel="noopener">Exchange Rate API</a></small></div></div>
      <div class="card"><h3>Privacy</h3><p class="muted">No ads and no trackers. Your data lives on your phone and, with sync on, in your My Finance account. This browser keeps an offline copy that is removed when you sign out.</p></div>
      <div class="card"><h3>Support</h3><p class="muted">Questions, feedback or a bug? ATEN Systems builds and maintains My Finance.</p>
        <div class="row"><a class="btn" href="https://aten-systems.site" target="_blank" rel="noopener noreferrer">aten-systems.site ${svg('open')}</a>
        <a class="btn primary" href="https://buymeacoffee.com/rohan_reyes" target="_blank" rel="noopener noreferrer">Buy me a coffee ${svg('open')}</a></div></div>
    </div>`;
  },
  actions: {
    newInvite: () => settings.actions.newInvite(),
    closeInvite: () => settings.actions.closeInvite(),
    copyInvite: async () => { try { await navigator.clipboard.writeText(serverInfo?.invite?.inviteCode || ''); toast('Invite code copied'); } catch { toast('Copy failed', 'err'); } },
  },
};
const getApp = {
  title: 'Get the app',
  render() {
    if (!serverInfo) settings.loadServerInfo();
    const app = serverInfo?.app;
    return `<div class="page-head"><h1>Get the app</h1></div>
    <div class="grid">
      <div class="card"><h3>Android app</h3>
        ${app ? `<p>Version ${esc(app.versionName || '—')} · ${(app.size / 1048576).toFixed(1)} MB · Android ${esc(app.minAndroid || '7.0')}+</p>
          <p class="muted">Download it on your phone and open the file. To update, download again and install over the current app; your data stays.</p>
          <div class="row"><a class="btn primary" href="${esc(app.url)}" download>Download APK</a></div>` : '<p class="muted">The Android app download is not available yet.</p>'}
        <p class="muted">Then open <strong>Settings → Data &amp; sync → Server sync</strong> in the app and sign in with this account.</p></div>
      <div class="card"><h3>Install this web app</h3><p class="muted">Add My Finance to your home screen or desktop. It opens in its own window and works offline.</p>
        ${installPrompt ? '<button class="btn primary" data-action="install">Install app</button>' : '<p class="muted">In your browser menu choose <strong>Install app</strong> or <strong>Add to Home Screen</strong>.</p>'}</div>
      <div class="card span2"><h3>Backups</h3><p class="muted">Your data is kept in your My Finance account. You can also export a full JSON backup (with photos and settings) or a CSV any time.</p>
        <a class="btn" href="#/settings">Open export &amp; import</a></div>
    </div>`;
  },
  actions: { install: async () => { if (!installPrompt) return; installPrompt.prompt(); await installPrompt.userChoice.catch(() => {}); installPrompt = null; store.emit(); } },
};

export const PAGES = { dashboard, customize, about, 'get-app': getApp, accounts, account: accountDetail, records, categories, budgets, debts, subscriptions, commissions, assets, cards, analytics, receipts, settings };
const NAV_PATHS = {"dashboard": `<path d="M3 13h8V3H3zm0 8h8v-6H3zm10 0h8V11h-8zm0-18v6h8V3z"/>`, "accounts": `<path d="M4 10h3v7H4zm6.5 0h3v7h-3zM2 19h20v3H2zm15-9h3v7h-3zM12 1 2 6v2h20V6z"/>`, "records": `<path d="M3 13h2v-2H3zm0 4h2v-2H3zm0-8h2V7H3zm4 4h14v-2H7zm0 4h14v-2H7zM7 7v2h14V7z"/>`, "budgets": `<path d="M11 2v20c-5.07-.5-9-4.79-9-10s3.93-9.5 9-10zm2.03 0v8.99H22c-.47-4.74-4.24-8.52-8.97-8.99zm0 11.01V22c4.74-.47 8.5-4.25 8.97-8.99z"/>`, "subscriptions": `<path d="M12 4V1L8 5l4 4V6a6 6 0 0 1 5.65 8.03l1.46 1.46A8 8 0 0 0 12 4zm-5.65 5.97L4.89 8.51A8 8 0 0 0 12 20v3l4-4-4-4v3a6 6 0 0 1-5.65-8.03z"/>`, "debts": `<path d="M16 11a3 3 0 1 0-3-3 3 3 0 0 0 3 3zm-8 0a3 3 0 1 0-3-3 3 3 0 0 0 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05A4.2 4.2 0 0 1 17 16.5V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>`, "commissions": `<path d="M10 16v-1H3v4a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-4h-7v1zm10-9h-4V5l-2-2h-4L8 5v2H4a2 2 0 0 0-2 2v3a2 2 0 0 0 2 2h6v-2h4v2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2zm-6 0h-4V5h4z"/>`, "assets": `<path d="M20 2H4a2 2 0 0 0-2 2v3.01c0 .72.43 1.34 1 1.69V20a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8.7c.57-.35 1-.97 1-1.69V4a2 2 0 0 0-2-2zm-5 12H9v-2h6zm5-7H4V4h16z"/>`, "cards": `<path d="M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 14H4v-6h16zm0-10H4V6h16z"/>`, "analytics": `<path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z"/>`, "receipts": `<path d="M18 17H6v-2h12zm0-4H6v-2h12zm0-4H6V7h12zM3 22l1.5-1.5L6 22l1.5-1.5L9 22l1.5-1.5L12 22l1.5-1.5L15 22l1.5-1.5L18 22l1.5-1.5L21 22V2l-1.5 1.5L18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2 4.5 3.5 3 2z"/>`, "categories": `<path d="M12 2l-5.5 9h11zm5.5 11a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9zM3 21.5h8v-8H3z"/>`, "settings": `<path d="M19.14 12.94a7.4 7.4 0 0 0 0-1.88l2.03-1.58a.48.48 0 0 0 .12-.61l-1.92-3.32a.48.48 0 0 0-.59-.21l-2.39.96a7 7 0 0 0-1.62-.94l-.36-2.54a.48.48 0 0 0-.48-.41h-3.84a.48.48 0 0 0-.47.41l-.36 2.54a7.4 7.4 0 0 0-1.62.94l-2.39-.96a.48.48 0 0 0-.59.21L2.74 8.87a.47.47 0 0 0 .12.61l2.03 1.58a7.4 7.4 0 0 0 0 1.88l-2.03 1.58a.48.48 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.21l2.39-.96c.5.38 1.04.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54a7.4 7.4 0 0 0 1.62-.94l2.39.96c.22.08.47 0 .59-.21l1.92-3.32a.48.48 0 0 0-.12-.61zM12 15.6A3.6 3.6 0 1 1 12 8.4a3.6 3.6 0 0 1 0 7.2z"/>`, "get-app": `<path d="M19 9h-4V3H9v6H5l7 7zM5 18v2h14v-2z"/>`, "about": `<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm1 15h-2v-6h2zm0-8h-2V7h2z"/>`};
const NAV_ICON = (k) => `<svg class="ico" viewBox="0 0 24 24" aria-hidden="true">${NAV_PATHS[k]}</svg>`;
const LABELS = { dashboard: 'Dashboard', analytics: 'Analytics', accounts: 'Accounts', records: 'Records', cards: 'Cards', receipts: 'Receipts',
  budgets: 'Budgets', subscriptions: 'Bills & subs', debts: 'Debts', commissions: 'Projects', assets: 'Assets', categories: 'Categories',
  settings: 'Settings', 'get-app': 'Get the app', about: 'About' };
/** Sidebar sections. */
export const NAV_GROUPS = [
  ['Overview', ['dashboard', 'analytics']],
  ['Money', ['accounts', 'records', 'cards', 'receipts']],
  ['Planning', ['budgets', 'subscriptions', 'debts', 'commissions', 'assets', 'categories']],
  ['App', ['settings', 'get-app', 'about']],
].map(([title, keys]) => [title, keys.map((k) => [k, NAV_ICON(k), LABELS[k]])]);
export const NAV = NAV_GROUPS.flatMap(([, items]) => items);
