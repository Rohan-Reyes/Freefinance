// Page renderers + actions. Each page: { title, render(params) -> html, actions: { name(el, params) } }
import { store } from './store.js';
import * as L from './logic.js';
import {
  esc, money, fmtDate, fmtDateTime, argbToCss, toast, modal, confirmDialog, formModal, formHTML, readForm,
  lineChart, barPairs, donut, progressBar, viewPrefs,
} from './ui.js';

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
    ...roots.flatMap((r) => [[r.id, `${L.icon(r.icon_res_id)} ${r.name}`], ...cats.filter((c) => c.parent_category_id === r.id).map((c) => [c.id, `   ↳ ${L.icon(c.icon_res_id)} ${c.name}`])])];
}
const accName = (id) => store.get('accounts', id)?.name || 'Deleted account';
const catOf = (id) => store.get('categories', id);
const catLabel = (id) => { const c = catOf(id); return c ? `${L.icon(c.icon_res_id)} ${esc(c.name)}` : '<span class="muted">Uncategorized</span>'; };
const empty = (text, cta = '') => `<div class="empty">${esc(text)}${cta}</div>`;
const curOpts = () => L.CURRENCIES.map((c) => [c.code, `${c.code} — ${c.name}`]);
const plus = (label, action, extra = '') => `<button class="btn primary" data-action="${action}" ${extra}>＋ ${esc(label)}</button>`;
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
    <div class="tx-icon ${amountClass(t)}">${t.type === 'Transfer' ? '⇄' : esc(L.icon(catOf(t.category_id)?.icon_res_id))}</div>
    <div class="tx-main"><strong>${esc(title)}</strong>
      <small>${where} · ${t.category_id ? catLabel(t.category_id) : esc(t.type)} · ${fmtDateTime(t.date)}</small>
      ${labels.length ? `<div class="chips">${labels.map((l) => `<span class="chip">${esc(l)}</span>`).join('')}</div>` : ''}</div>
    <div class="tx-amt ${amountClass(t)}">${txAmount(t, perspectiveAccountId)}
      ${t.status === 'Planned' ? '<small class="badge warn">Planned</small>' : ''}
      ${t.recurrence_rule && t.recurrence_rule !== 'None' ? `<small class="badge">${esc(t.recurrence_rule)}</small>` : ''}</div>
  </div>`;
}

// ---------------------------------------------------------------- transaction form
function txFields(type) {
  return [
    { key: 'type', label: 'Type', type: 'select', options: [['Expense', 'Expense'], ['Income', 'Income'], ['Transfer', 'Transfer']], half: true },
    { key: 'amount', label: 'Amount', type: 'money', required: true, half: true },
    { key: 'account_id', label: type === 'Transfer' ? 'From account' : 'Account', type: 'select', options: accountOptions(), numeric: true, required: true, half: true },
    { key: 'to_account_id', label: 'To account', type: 'select', options: accountOptions(true), numeric: true, half: true },
    { key: 'category_id', label: 'Category', type: 'select', options: categoryOptions(type === 'Income' ? 'Income' : 'Expense'), numeric: true },
    { key: 'date', label: 'Date & time', type: 'datetime', required: true, half: true },
    { key: 'status', label: 'Status', type: 'select', options: [['Cleared', 'Cleared'], ['Planned', 'Planned']], half: true },
    { key: 'recurrence_rule', label: 'Repeats', type: 'select', options: L.RECURRENCE.map((r) => [r, r === 'Monthly:Last' ? 'Monthly (last day)' : r]), half: true },
    { key: 'labels', label: 'Labels', hint: 'Comma separated, e.g. Project X, Reimbursable', half: true },
    { key: 'note', label: 'Note / description', type: 'textarea' },
  ];
}
export async function openTxForm(existing, preset = {}) {
  if (!store.all('accounts').length) { toast('Add an account first', 'warn'); location.hash = '#/accounts'; return; }
  const t = existing || { type: 'Expense', date: Date.now(), status: 'Cleared', recurrence_rule: 'None', account_id: store.all('accounts')[0].id, ...preset };
  const values = { ...t, labels: L.labelsOf(t).join(', ') };
  const toggle = (wrap) => {
    const type = wrap.querySelector('[name=type]').value;
    wrap.querySelector('[name=to_account_id]').closest('.field').style.display = type === 'Transfer' ? '' : 'none';
    wrap.querySelector('[name=category_id]').closest('.field').style.display = type === 'Transfer' ? 'none' : '';
    const sel = wrap.querySelector('[name=category_id]'), keep = sel.value;
    sel.innerHTML = categoryOptions(type === 'Income' ? 'Income' : 'Expense').map(([v, l]) => `<option value="${esc(v)}" ${String(v) === keep ? 'selected' : ''}>${esc(l)}</option>`).join('');
  };
  await formModal(existing ? 'Edit transaction' : 'New transaction', txFields(t.type), values, {
    wide: true,
    onOpen: (w) => { toggle(w); w.querySelector('[name=type]').addEventListener('change', () => toggle(w)); },
    extraActions: existing ? [{ label: 'Delete', kind: 'danger ghost', onClick: async () => {
      if (!(await confirmDialog('Delete transaction?', 'This cannot be undone.'))) return false;
      store.remove('transactions', existing.id); toast('Transaction deleted');
    } }] : [],
    onSubmit: (v) => {
      if (v.type === 'Transfer') {
        if (!v.to_account_id) throw new Error('Choose the destination account');
        if (v.to_account_id === v.account_id) throw new Error('From and To accounts must differ');
        v.category_id = null;
      } else v.to_account_id = null;
      v.labels = JSON.stringify((v.labels || '').split(',').map((s) => s.trim()).filter(Boolean));
      if (existing) store.update('transactions', existing.id, v); else L.addTransaction(v);
      toast(existing ? 'Transaction updated' : 'Transaction added');
    },
  });
}

// ---------------------------------------------------------------- DASHBOARD
const dashboard = {
  title: 'Dashboard',
  render() {
    const inclAssets = viewPrefs.get('includeAssets', false);
    const nw = L.netWorth(inclAssets);
    const m0 = new Date(); m0.setDate(1); m0.setHours(0, 0, 0, 0);
    const flow = L.moneyFlow(+m0);
    const ll = L.liquidVsLocked();
    const fc = L.forecast(30, inclAssets);
    const budgets = L.budgetProgress().slice(0, 4);
    const lent = store.all('debts').filter((d) => d.type === 'Lent' && d.status === 'Active');
    const borrowed = store.all('debts').filter((d) => d.type === 'Borrowed' && d.status === 'Active');
    const upcoming = [
      ...L.recurringTemplates().map((t) => ({ when: L.nextDueDate(t.date, t.recurrence_rule), title: t.note || catOf(t.category_id)?.name || 'Recurring', amount: t.type === 'Income' ? t.amount : -t.amount, kind: 'Subscription' })),
      ...store.all('planned_payouts').filter((p) => p.status === 'PENDING').map((p) => ({ when: p.due_date, title: p.title, amount: -p.amount, kind: 'Payout', cur: p.currency_code })),
      ...store.all('milestones').filter((m) => m.status === 'Pending').map((m) => ({ when: m.expected_date, title: m.title, amount: m.amount, kind: 'Milestone' })),
    ].sort((a, b) => a.when - b.when).slice(0, 6);
    const recent = store.all('transactions').sort((a, b) => b.date - a.date || b.id - a.id).slice(0, 8);
    const b = L.base();
    return `
    <section class="hero">
      <div><span class="eyebrow">Total ${inclAssets ? 'net worth' : 'cash balance'} · ${esc(b)}</span>
        <h1 class="hero-num">${money(nw.total)}</h1>
        <div class="hero-sub"><span>Cash ${money(nw.cash)}</span><span>Assets ${money(nw.assets)}</span>${nw.excluded ? `<span>${nw.excluded} account(s) excluded</span>` : ''}</div></div>
      <div class="hero-actions">
        <button class="btn" data-action="quick" data-type="Expense">− Expense</button>
        <button class="btn" data-action="quick" data-type="Income">＋ Income</button>
        <button class="btn" data-action="quick" data-type="Transfer">⇄ Transfer</button>
        <label class="switch"><input type="checkbox" data-action="toggleAssets" ${inclAssets ? 'checked' : ''}> Include assets</label>
      </div>
    </section>
    <div class="grid">
      <div class="card"><h3>This month</h3><div class="kpis">
        ${kpi('Income', money(flow.income), 'pos')}${kpi('Expenses', money(flow.expense), 'neg')}${kpi('Net', money(flow.net, b, { sign: true }), flow.net >= 0 ? 'pos' : 'neg')}</div></div>
      <div class="card"><h3>Liquid vs locked</h3>
        <div class="split"><i style="width:${ll.ratio * 100}%"></i></div>
        <div class="kpis">${kpi('Spendable', money(ll.spendable))}${kpi('Locked / invested', money(ll.locked))}</div></div>
      <div class="card span2"><h3>30-day cash-flow forecast</h3>
        ${lineChart(fc.map((d) => ({ y: d.balance, label: fmtDate(d.date, { month: 'short', day: 'numeric' }) })), { fmt: (v) => money(v) })}
        <div class="kpis">${kpi('Today', money(fc[0]?.balance ?? 0))}${kpi('In 30 days', money(fc[fc.length - 1]?.balance ?? 0))}</div></div>
      <div class="card"><h3>Budgets <a href="#/budgets" class="more">All</a></h3>
        ${budgets.length ? budgets.map((x) => `<div class="budget-line"><div><strong>${L.icon(x.category.icon_res_id)} ${esc(x.category.name)}</strong>
          <small>${money(x.spent)} of ${money(x.budget.amount_limit)}</small></div>${progressBar(x.ratio)}</div>`).join('') : empty('No budgets yet')}</div>
      <div class="card"><h3>Debts <a href="#/debts" class="more">All</a></h3>
        <div class="kpis">${kpi('Owed to you', money(lent.reduce((s, d) => s + d.remaining_amount, 0)), 'pos')}${kpi('You owe', money(borrowed.reduce((s, d) => s + d.remaining_amount, 0)), 'neg')}</div>
        ${lent.sort((a, b) => b.remaining_amount - a.remaining_amount).slice(0, 3).map((d) => `<div class="mini-row"><span>${esc(d.person_name)}</span><strong>${money(d.remaining_amount)}</strong></div>`).join('')}</div>
      <div class="card"><h3>Upcoming</h3>
        ${upcoming.length ? upcoming.map((u) => `<div class="mini-row"><span><small class="badge">${esc(u.kind)}</small> ${esc(u.title)}<br><small class="muted">${fmtDate(u.when)}</small></span>
          <strong class="${u.amount >= 0 ? 'pos' : 'neg'}">${money(u.amount, u.cur || b, { sign: true })}</strong></div>`).join('') : empty('Nothing scheduled')}</div>
      <div class="card span2"><h3>Monthly trend</h3>${barPairs(L.monthlyTrends(6), { fmt: (v) => money(v) })}</div>
      <div class="card span2"><h3>Recent transactions <a href="#/records" class="more">All</a></h3>
        ${recent.length ? recent.map((t) => txRow(t)).join('') : empty('No transactions yet', ` ${plus('Add one', 'quick', 'data-type="Expense"')}`)}</div>
    </div>`;
  },
  actions: {
    quick: (el) => openTxForm(null, { type: el.dataset.type }),
    toggleAssets: (el) => { viewPrefs.set('includeAssets', el.checked); store.emit(); },
    editTx: (el) => openTxForm(store.get('transactions', el.dataset.id)),
  },
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
    return `<div class="page-head"><div><h1>Accounts</h1><p class="muted">Total in tally: <strong>${money(nw.cash)}</strong></p></div>
      <div class="row">${plus('Account', 'addAccount')}<button class="btn" data-action="addSub">＋ Sub-account</button></div></div>
      ${tree.length ? `<div class="list">${tree.map(({ a, depth }) => {
        const bal = L.balanceOf(a);
        const conv = a.currency_code !== L.base() ? `<small class="muted">≈ ${money(L.convert(bal, a.currency_code, L.base()))}</small>` : '';
        const locked = a.is_locked || (a.locked_until && a.locked_until > Date.now());
        return `<div class="acc-row ${depth ? 'sub' : ''}">
          <span class="dot" style="background:${argbToCss(a.color)}"></span>
          <a class="acc-main" href="#/accounts/${a.id}"><strong>${esc(a.name)}</strong>
            <small>${esc(a.sub_account_type || a.type)} · ${esc(a.currency_code)}${locked ? ' · 🔒 Locked' : ''}${a.include_in_tally ? '' : ' · Excluded'}${a.interest_rate_pa > 0 ? ` · ${a.interest_rate_pa}% p.a.` : ''}</small></a>
          <div class="acc-bal"><strong class="${bal < 0 ? 'neg' : ''}">${money(bal, a.currency_code)}</strong>${conv}</div>
          <div class="row-actions">
            <button class="icon-btn" title="Adjust balance" data-action="adjust" data-id="${a.id}">⚖</button>
            <button class="icon-btn" title="Edit" data-action="editAccount" data-id="${a.id}">✎</button>
            <button class="icon-btn" title="Delete" data-action="deleteAccount" data-id="${a.id}">🗑</button></div>
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
    return `<div class="page-head"><div><a href="#/accounts" class="muted">← Accounts</a><h1><span class="dot big" style="background:${argbToCss(a.color)}"></span>${esc(a.name)}</h1>
      <p class="muted">${esc(a.sub_account_type || a.type)} · ${esc(a.currency_code)}</p></div>
      <div class="row"><button class="btn" data-action="add" data-type="Income">＋ Income</button><button class="btn" data-action="add" data-type="Expense">− Expense</button>
      <button class="btn" data-action="add" data-type="Transfer">⇄ Transfer</button><button class="btn" data-action="edit">Edit</button></div></div>
      <div class="kpis big">${kpi('Balance', money(L.balanceOf(a), a.currency_code))}${kpi('Starting balance', money(a.initial_balance, a.currency_code))}${kpi('Transactions', String(txs.length))}</div>
      ${subs.length ? `<h3>Sub-accounts</h3><div class="list">${subs.map((s) => `<a class="mini-row link" href="#/accounts/${s.id}"><span>${esc(s.name)} <small class="muted">${esc(s.sub_account_type || '')}</small></span><strong>${money(L.balanceOf(s), s.currency_code)}</strong></a>`).join('')}</div>` : ''}
      <h3>Ledger</h3><div class="list">${txs.length ? txs.map((t) => txRow(t, a.id)).join('') : empty('No transactions for this account')}</div>`;
  },
  actions: {
    add: (el, [id]) => openTxForm(null, { type: el.dataset.type, account_id: Number(id) }),
    edit: (el, [id]) => openAccountForm(store.get('accounts', id)),
    editTx: (el) => openTxForm(store.get('transactions', el.dataset.id)),
  },
};

// ---------------------------------------------------------------- RECORDS
const recordFilter = { q: '', type: '', account: '', category: '', status: '', month: '' };
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
    for (const t of txs.slice(0, 600)) { const k = fmtDate(t.date, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }); if (!groups.has(k)) groups.set(k, []); groups.get(k).push(t); }
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
      ${txs.length > 600 ? '<p class="muted center">Showing the latest 600 — refine the filters to see more.</p>' : ''}`;
  },
  actions: {
    add: () => openTxForm(null),
    editTx: (el) => openTxForm(store.get('transactions', el.dataset.id)),
  },
  onFilter(name, value) { recordFilter[name] = value; },
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
      <div class="row-actions"><button class="icon-btn" data-action="edit" data-id="${c.id}">✎</button><button class="icon-btn" data-action="del" data-id="${c.id}">🗑</button></div></div>`;
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
        <span class="row-actions"><button class="icon-btn" data-action="edit" data-id="${x.budget.id}">✎</button><button class="icon-btn" data-action="del" data-id="${x.budget.id}">🗑</button></span></h3>
        ${progressBar(x.ratio)}<div class="kpis">${kpi('Spent', money(x.spent), x.ratio >= 1 ? 'neg' : '')}${kpi(x.remaining >= 0 ? 'Left' : 'Over by', money(Math.abs(x.remaining)), x.remaining >= 0 ? 'pos' : 'neg')}</div>
        <small class="muted">${esc(x.budget.period)} · ${fmtDate(x.budget.start_date)} – ${fmtDate(x.budget.end_date)} · ${Math.round(x.ratio * 100)}%</small></div>`).join('')}</div>`
        : empty('No budgets yet. Set a spending limit for a category.')}
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
          ${progressBar(d.total_amount ? paid / d.total_amount : 0, 2)}
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
const subFields = () => [
  { key: 'note', label: 'Name (e.g. Netflix, Salary)', required: true },
  { key: 'type', label: 'Type', type: 'select', options: [['Expense', 'Expense / bill'], ['Income', 'Income']], half: true },
  { key: 'amount', label: 'Amount', type: 'money', required: true, half: true },
  { key: 'recurrence_rule', label: 'Repeats', type: 'select', options: L.RECURRENCE.filter((r) => r !== 'None').map((r) => [r, r === 'Monthly:Last' ? 'Monthly (last day)' : r]), half: true },
  { key: 'date', label: 'First / anchor date', type: 'datetime', required: true, half: true },
  { key: 'account_id', label: 'Account', type: 'select', numeric: true, options: accountOptions(), required: true, half: true },
  { key: 'category_id', label: 'Category', type: 'select', numeric: true, options: categoryOptions(null), half: true },
];
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
    return `<div class="page-head"><h1>Subscriptions & bills</h1><div class="row">${plus('Recurring', 'addSub')}<button class="btn" data-action="addPayout">＋ Planned payout</button></div></div>
      <div class="kpis big">${kpi('Monthly bills', money(mExp), 'neg')}${kpi('Monthly recurring income', money(mInc), 'pos')}${kpi('Pending payouts', money(pend.reduce((s, p) => s + L.convert(p.amount, p.currency_code, L.base()), 0)))}</div>
      <h3>Recurring</h3><div class="list">${subs.length ? subs.map(({ t, next, cur }) => `<div class="acc-row">
        <span class="cat-ico">${L.icon(catOf(t.category_id)?.icon_res_id)}</span>
        <div class="acc-main"><strong>${esc(t.note || catOf(t.category_id)?.name || 'Recurring')}</strong><small>${esc(L.scheduleLabel(t.recurrence_rule, t.date))} · next ${fmtDate(next)} · ${esc(accName(t.account_id))}</small></div>
        <div class="acc-bal"><strong class="${t.type === 'Income' ? 'pos' : 'neg'}">${money(t.type === 'Income' ? t.amount : -t.amount, cur, { sign: true })}</strong><small class="muted">≈ ${money(L.monthlyEquivalent(t.amount, t.recurrence_rule), cur)}/mo</small></div>
        <div class="row-actions"><button class="icon-btn" title="Log payment now" data-action="logSub" data-id="${t.id}">✔</button><button class="icon-btn" title="Edit" data-action="editSub" data-id="${t.id}">✎</button>
        <button class="icon-btn" title="Stop" data-action="stopSub" data-id="${t.id}">⏹</button><button class="icon-btn" title="Delete" data-action="delSub" data-id="${t.id}">🗑</button></div></div>`).join('') : empty('No recurring items')}</div>
      <h3>Planned payouts</h3><div class="list">${pend.length ? pend.map((p) => `<div class="acc-row">
        <span class="cat-ico">${p.priority === 'HIGH' ? '🔴' : p.priority === 'LOW' ? '⚪' : '🟡'}</span>
        <div class="acc-main"><strong>${esc(p.title)}</strong><small>Due ${fmtDate(p.due_date)}${p.due_date < Date.now() ? ' · <b class="neg">overdue</b>' : ''}${p.payee_or_vendor ? ' · ' + esc(p.payee_or_vendor) : ''}</small></div>
        <div class="acc-bal"><strong>${money(p.amount, p.currency_code)}</strong></div>
        <div class="row-actions"><button class="btn small" data-action="payPayout" data-id="${p.id}">Mark paid</button><button class="icon-btn" data-action="editPayout" data-id="${p.id}">✎</button><button class="icon-btn" data-action="delPayout" data-id="${p.id}">🗑</button></div></div>`).join('') : empty('No pending payouts')}</div>
      ${paid.length ? `<h3>Recently paid</h3><div class="list">${paid.map((p) => `<div class="acc-row"><span class="cat-ico">✅</span><div class="acc-main"><strong>${esc(p.title)}</strong><small>${esc(p.status)} ${p.paid_date ? fmtDate(p.paid_date) : ''}</small></div>
        <div class="acc-bal"><strong>${money(p.amount, p.currency_code)}</strong></div><div class="row-actions">${p.status === 'PAID' ? `<button class="btn small ghost" data-action="unpay" data-id="${p.id}">Undo</button>` : ''}</div></div>`).join('')}</div>` : ''}`;
  },
  actions: {
    addSub: () => (store.all('accounts').length ? formModal('New recurring item', subFields(), { type: 'Expense', recurrence_rule: 'Monthly', date: Date.now(), account_id: store.all('accounts')[0]?.id }, { wide: true, onSubmit: (v) => { L.addTransaction({ ...v, status: 'Planned', labels: '[]' }); toast('Recurring item added'); } }) : toast('Add an account first', 'warn')),
    editSub: (el) => { const t = store.get('transactions', el.dataset.id); return formModal('Edit recurring item', subFields(), t, { wide: true, onSubmit: (v) => { store.update('transactions', t.id, v); toast('Saved'); } }); },
    stopSub: async (el) => { if (await confirmDialog('Stop this recurring item?', 'It will no longer repeat or appear in forecasts as recurring.', 'Stop', 'primary')) { store.update('transactions', el.dataset.id, { recurrence_rule: 'None' }); toast('Stopped'); } },
    delSub: async (el) => { if (await confirmDialog('Delete recurring item?', 'This cannot be undone.')) { store.remove('transactions', el.dataset.id); toast('Deleted'); } },
    logSub: (el) => { const t = store.get('transactions', el.dataset.id); return formModal(`Log payment — ${t.note || 'recurring'}`, [{ key: 'amount', label: 'Amount', type: 'money', required: true, half: true }, { key: 'account_id', label: 'Account', type: 'select', numeric: true, options: accountOptions(), half: true }], t, { onSubmit: (v) => { L.addTransaction({ type: t.type, amount: v.amount, account_id: v.account_id, category_id: t.category_id, date: Date.now(), status: 'Cleared', note: t.note, labels: JSON.stringify(['Subscription']) }); toast('Payment logged'); } }); },
    addPayout: () => formModal('New planned payout', payoutFields(), { currency_code: L.base(), priority: 'NORMAL', due_date: Date.now() }, { wide: true, onSubmit: (v) => { store.insert('planned_payouts', { ...v, status: 'PENDING', created_at: Date.now() }); toast('Payout planned'); } }),
    editPayout: (el) => { const p = store.get('planned_payouts', el.dataset.id); return formModal('Edit payout', payoutFields(), p, { wide: true, onSubmit: (v) => { store.update('planned_payouts', p.id, v); toast('Saved'); } }); },
    delPayout: async (el) => { if (await confirmDialog('Delete payout?', 'This cannot be undone.')) { store.remove('planned_payouts', el.dataset.id); toast('Deleted'); } },
    payPayout: (el) => { const p = store.get('planned_payouts', el.dataset.id); return formModal(`Mark paid — ${p.title}`, [{ key: 'account', label: 'Paid from', type: 'select', numeric: true, options: accountOptions(), required: true }, { key: 'amount', label: 'Actual amount', type: 'money', required: true }], { account: p.target_account_id ?? store.all('accounts')[0]?.id, amount: p.amount }, { onSubmit: (v) => { L.markPayoutPaid(p, v.account, v.amount); toast('Marked paid'); } }); },
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
          <span class="row-actions"><button class="icon-btn" data-action="editProject" data-id="${p.id}">✎</button><button class="icon-btn" data-action="delProject" data-id="${p.id}">🗑</button></span></h3>
          <p class="muted">${esc(p.client || 'No client')} · ${money(got)} received of ${money(p.total_expected_fee)}</p>${progressBar(p.total_expected_fee ? got / p.total_expected_fee : 0, 2)}
          <div class="list">${pm.map((m) => `<div class="mini-row"><span>${m.status === 'Paid' ? '✅' : '⏳'} ${esc(m.title)}<br><small class="muted">${m.status === 'Paid' ? 'Paid ' + fmtDate(m.paid_at) : 'Expected ' + fmtDate(m.expected_date)}</small></span>
            <span class="row"><strong>${money(m.amount)}</strong>${m.status === 'Pending' ? `<button class="btn small" data-action="payMilestone" data-id="${m.id}">Mark paid</button>` : ''}
            <button class="icon-btn" data-action="editMilestone" data-id="${m.id}">✎</button><button class="icon-btn" data-action="delMilestone" data-id="${m.id}">🗑</button></span></div>`).join('')}</div>
          <button class="btn small" data-action="addMilestone" data-id="${p.id}">＋ Milestone</button></div>`;
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
        <div class="row-actions"><button class="icon-btn" data-action="edit" data-id="${a.id}">✎</button><button class="icon-btn" data-action="del" data-id="${a.id}">🗑</button></div></div>`; }).join('') : empty('No assets yet — log vehicles, gadgets, property, gold…')}</div>`;
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
        const num = show && digits ? digits.match(/.{1,4}/g).join('  ') : `••••  ••••  ••••  ${L.lastFour(c)}`;
        const util = c.credit_limit > 0 ? c.current_credit_used / c.credit_limit : 0;
        return `<div class="pay-card-wrap"><div class="pay-card ${c.is_frozen ? 'frozen' : ''}" style="background:linear-gradient(135deg,${c1},${c2})">
          <div class="pc-top"><span>${esc(c.issuing_bank || c.card_name)}</span><span>${esc(c.card_network)}</span></div>
          <div class="pc-num">${esc(num)}</div>
          <div class="pc-bot"><span>${esc(c.cardholder_name || '')}<br><small>${esc(c.card_name)}</small></span><span>${String(c.expiry_month).padStart(2, '0')}/${String(c.expiry_year % 100).padStart(2, '0')}${show && c.cvv ? `<br><small>CVV ${esc(c.cvv)}</small>` : ''}</span></div>
          ${c.is_frozen ? '<div class="pc-frozen">❄ Frozen</div>' : ''}</div>
          ${c.card_type === 'CREDIT' ? `<div class="pc-meta">${progressBar(util)}<small class="muted">${money(c.current_credit_used, c.currency_code)} of ${money(c.credit_limit, c.currency_code)} · min due ${money(c.minimum_payment_due, c.currency_code)}${c.payment_due_day ? ' · due day ' + c.payment_due_day : ''}</small></div>`
            : `<div class="pc-meta"><small class="muted">${esc(L.CARD_TYPES.find((t) => t[0] === c.card_type)?.[1] || c.card_type)} · ${c.linked_account_id ? 'linked to ' + esc(accName(c.linked_account_id)) : 'no linked account'}</small></div>`}
          <div class="row"><button class="btn small" data-action="reveal" data-id="${c.id}">${show ? 'Hide' : 'Reveal'}</button><button class="btn small" data-action="charge" data-id="${c.id}">Log charge</button>
          ${c.card_type === 'CREDIT' ? `<button class="btn small" data-action="payBill" data-id="${c.id}">Pay bill</button>` : ''}
          <button class="btn small ghost" data-action="freeze" data-id="${c.id}">${c.is_frozen ? 'Unfreeze' : 'Freeze'}</button><button class="icon-btn" data-action="edit" data-id="${c.id}">✎</button><button class="icon-btn" data-action="del" data-id="${c.id}">🗑</button></div></div>`;
      }).join('') : empty('No cards saved')}</div>`;
  },
  actions: {
    add: () => formModal('New card', cardFields(), { card_type: 'DEBIT', card_network: 'VISA', currency_code: L.base(), card_finish: 'OBSIDIAN_GOLD', expiry_month: 12, expiry_year: 28 }, { wide: true, onSubmit: (v) => { store.insert('payment_cards', { ...numDefaults(v), created_at: Date.now() }); toast('Card saved'); } }),
    edit: (el) => { const c = store.get('payment_cards', el.dataset.id); return formModal('Edit card', cardFields(), c, { wide: true, onSubmit: (v) => { store.update('payment_cards', c.id, numDefaults(v)); toast('Card updated'); } }); },
    del: async (el) => { if (await confirmDialog('Delete card?', 'This removes it from every synced device.')) { store.remove('payment_cards', el.dataset.id); toast('Card deleted'); } },
    reveal: (el) => { const id = Number(el.dataset.id); if (revealed.has(id)) revealed.delete(id); else { revealed.add(id); setTimeout(() => { revealed.delete(id); store.emit(); }, 30000); } store.emit(); },
    freeze: (el) => { const c = store.get('payment_cards', el.dataset.id); store.update('payment_cards', c.id, { is_frozen: c.is_frozen ? 0 : 1 }); },
    charge: (el) => { const c = store.get('payment_cards', el.dataset.id); return formModal(`Log charge — ${c.card_name}`, [{ key: 'amount', label: 'Amount', type: 'money', required: true, half: true }, { key: 'category', label: 'Category', type: 'select', numeric: true, options: categoryOptions('Expense'), half: true }, { key: 'note', label: 'Merchant / note' }, { key: 'account', label: c.card_type === 'CREDIT' ? 'Also record in account (optional)' : 'Account (defaults to linked)', type: 'select', numeric: true, options: accountOptions(true) }], {}, { onSubmit: (v) => { L.logCardCharge(c, v.amount, v.category, v.note, v.account); toast('Charge logged'); } }); },
    payBill: (el) => { const c = store.get('payment_cards', el.dataset.id); return formModal(`Pay bill — ${c.card_name}`, [{ key: 'amount', label: 'Payment amount', type: 'money', required: true }, { key: 'account', label: 'Pay from', type: 'select', numeric: true, options: accountOptions(), required: true }, { key: 'note', label: 'Note' }], { amount: c.statement_balance || c.current_credit_used }, { onSubmit: (v) => { L.payCardBill(c, v.amount, v.account, v.note); toast('Payment recorded'); } }); },
  },
};

// ---------------------------------------------------------------- ANALYTICS
let anRange = 'month';
const PALETTE = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#e87ba4', '#008300', '#6250d6', '#e34948'];
const analytics = {
  title: 'Analytics',
  render() {
    const n = new Date(); let from, to = Date.now();
    if (anRange === 'month') from = +new Date(n.getFullYear(), n.getMonth(), 1);
    else if (anRange === 'last') { from = +new Date(n.getFullYear(), n.getMonth() - 1, 1); to = +new Date(n.getFullYear(), n.getMonth(), 1) - 1; }
    else if (anRange === '3m') from = +new Date(n.getFullYear(), n.getMonth() - 2, 1);
    else from = +new Date(n.getFullYear(), 0, 1);
    const cats = L.spendingByCategory(from, to), total = cats.reduce((s, c) => s + c.total, 0);
    const top = cats.slice(0, 7), other = cats.slice(7).reduce((s, c) => s + c.total, 0);
    const items = [...top.map((c, i) => ({ label: c.category?.name || 'Uncategorized', value: c.total, color: PALETTE[i] })), ...(other ? [{ label: 'Other', value: other, color: '#9aa0a6' }] : [])];
    const flow = L.moneyFlow(from, to), days = Math.max(1, Math.ceil((to - from) / 864e5));
    return `<div class="page-head"><h1>Analytics</h1><div class="tabs">${[['month', 'This month'], ['last', 'Last month'], ['3m', '3 months'], ['year', 'This year']].map(([k, l]) => `<button class="tab ${k === anRange ? 'on' : ''}" data-action="range" data-r="${k}">${l}</button>`).join('')}</div></div>
      <div class="kpis big">${kpi('Income', money(flow.income), 'pos')}${kpi('Expenses', money(flow.expense), 'neg')}${kpi('Savings rate', flow.income > 0 ? `${Math.round((flow.net / flow.income) * 100)}%` : '—')}${kpi('Avg daily spend', money(flow.expense / days))}</div>
      <div class="grid"><div class="card"><h3>Spending by category</h3><div class="donut-wrap">${donut(items)}
        <div class="legend-list">${items.map((i) => `<div class="mini-row"><span><i class="sw" style="background:${i.color}"></i>${esc(i.label)}</span><strong>${money(i.value)} <small class="muted">${total ? Math.round((i.value / total) * 100) : 0}%</small></strong></div>`).join('')}</div></div></div>
      <div class="card"><h3>Income vs expenses · 12 months</h3>${barPairs(L.monthlyTrends(12), { fmt: (v) => money(v) })}</div></div>`;
  },
  actions: { range: (el) => { anRange = el.dataset.r; store.emit(); } },
};

// ---------------------------------------------------------------- RECEIPTS
const receipts = {
  title: 'Receipts',
  render() {
    const list = store.all('receipts_inbox').sort((a, b) => b.captured_at - a.captured_at);
    const pending = list.filter((r) => r.status === 'PENDING');
    return `<div class="page-head"><div><h1>Receipts inbox</h1><p class="muted">Receipt photos are captured on the phone and stay on it; their details sync here.</p></div></div>
      <div class="list">${list.length ? list.map((r) => `<div class="acc-row"><span class="cat-ico">${r.status === 'PENDING' ? '🧾' : r.status === 'CONVERTED' ? '✅' : '🗑'}</span>
        <div class="acc-main"><strong>${esc(r.note || 'Receipt')}</strong><small>${fmtDateTime(r.captured_at)} · ${esc(r.status)}${r.suggested_amount != null ? ' · ' + money(r.suggested_amount) : ''}</small></div>
        <div class="row-actions">${r.status === 'PENDING' ? `<button class="btn small" data-action="convert" data-id="${r.id}">Convert</button><button class="btn small ghost" data-action="discard" data-id="${r.id}">Discard</button>` : ''}
        <button class="icon-btn" data-action="del" data-id="${r.id}">🗑</button></div></div>`).join('') : empty('No receipts. Snap them from the phone app.')}</div>
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
    del: async (el) => { if (await confirmDialog('Delete receipt record?', 'The photo on the phone will no longer be listed.')) { store.remove('receipts_inbox', el.dataset.id); toast('Deleted'); } },
  },
};

// ---------------------------------------------------------------- SETTINGS
let devicesCache = null;
const settings = {
  title: 'Settings',
  render() {
    const s = store.status, a = store.auth;
    const rates = store.all('exchange_rates').sort((x, y) => x.from_currency.localeCompare(y.from_currency));
    if (!devicesCache) settings.loadDevices();
    const theme = viewPrefs.get('theme', 'system');
    return `<div class="page-head"><h1>Settings</h1></div>
    <div class="grid">
      <div class="card"><h3>Sync</h3>
        <p>Signed in as <strong>${esc(a.username)}</strong> · this browser is device #${a.deviceNo}</p>
        <p class="muted">${s.syncing ? 'Syncing…' : s.error ? `⚠ ${esc(s.error)}` : `Last sync ${s.lastSync ? fmtDateTime(s.lastSync) : 'never'}`} · ${store.pending.size} pending change(s)</p>
        <div class="row"><button class="btn primary" data-action="syncNow">Sync now</button><button class="btn" data-action="resetCache">Re-download everything</button><button class="btn danger ghost" data-action="signOut">Sign out</button></div></div>
      <div class="card"><h3>Display</h3>
        <label class="field"><span>Base currency (synced to all devices)</span><select data-action-change="baseCurrency">${L.CURRENCIES.map((c) => `<option value="${c.code}" ${c.code === L.base() ? 'selected' : ''}>${c.code} — ${esc(c.name)}</option>`).join('')}</select></label>
        <label class="field"><span>Theme (this browser)</span><select data-action-change="theme">${[['system', 'System'], ['light', 'Light'], ['dark', 'Dark']].map(([v, l]) => `<option value="${v}" ${v === theme ? 'selected' : ''}>${l}</option>`).join('')}</select></label>
        <label class="check"><input type="checkbox" data-action-change="hideMoney" ${viewPrefs.get('hideMoney', false) ? 'checked' : ''}> Hide amounts (privacy mask)</label></div>
      <div class="card span2"><h3>Exchange rates <span class="row-actions"><button class="btn small" data-action="addRate">＋ Rate</button></span></h3>
        <p class="muted">Used to convert accounts into ${esc(L.base())}. Without a rate, built-in reference rates are used.</p>
        <div class="list">${rates.length ? rates.map((r) => `<div class="mini-row"><span>1 ${esc(r.from_currency)} = <strong>${r.rate}</strong> ${esc(r.to_currency)} <small class="muted">updated ${fmtDate(r.last_updated)}</small></span>
          <span class="row-actions"><button class="icon-btn" data-action="editRate" data-id="${r.id}">✎</button><button class="icon-btn" data-action="delRate" data-id="${r.id}">🗑</button></span></div>`).join('') : empty('No custom rates')}</div></div>
      <div class="card"><h3>Devices</h3><div class="list">${devicesCache ? devicesCache.devices.map((d) => `<div class="mini-row"><span>${d.kind === 'android' ? '📱' : d.kind === 'web' ? '🖥' : '🔌'} ${esc(d.name || 'Device')} #${d.device_no}${d.device_no === devicesCache.current ? ' <small class="badge">this one</small>' : ''}<br>
        <small class="muted">${d.revoked ? 'Signed out' : 'Last seen ' + fmtDateTime(d.last_seen)}</small></span>${!d.revoked && d.device_no !== devicesCache.current ? `<button class="btn small ghost danger" data-action="revoke" data-id="${d.device_no}">Sign out</button>` : ''}</div>`).join('') : '<p class="muted">Loading…</p>'}</div></div>
      <div class="card"><h3>Security & data</h3>
        <div class="row"><button class="btn" data-action="changePassword">Change password</button><button class="btn" data-action="exportJson">Export JSON</button><button class="btn" data-action="exportCsv">Export CSV</button></div>
        <p class="muted">JSON export contains every record (including card numbers) — store it safely.</p></div>
    </div>`;
  },
  async loadDevices() {
    try { const { api } = await import('./store.js'); devicesCache = await api('/api/devices', { token: store.auth.token }); store.emit(); } catch { devicesCache = { devices: [], current: 0 }; }
  },
  actions: {
    syncNow: async () => { try { await store.sync(); toast('Synced'); } catch (e) { toast(e.message, 'err'); } },
    resetCache: async () => { if (await confirmDialog('Re-download everything?', 'Clears this browser\'s copy and downloads all data from the server again. Unsynced changes in this browser are lost.', 'Re-download', 'primary')) { await store.resetLocalCache(); toast('Re-downloaded'); } },
    signOut: async () => { if (await confirmDialog('Sign out?', 'This browser\'s local copy will be removed.', 'Sign out', 'primary')) store.signOut(); },
    addRate: () => formModal('Exchange rate', [{ key: 'from_currency', label: 'From', type: 'select', options: curOpts(), half: true }, { key: 'to_currency', label: 'To', type: 'select', options: curOpts(), half: true }, { key: 'rate', label: 'Rate', type: 'number', required: true }], { from_currency: 'USD', to_currency: L.base() }, { onSubmit: (v) => {
      if (v.from_currency === v.to_currency) throw new Error('Pick two different currencies');
      if (!(v.rate > 0)) throw new Error('Rate must be positive');
      const dup = store.all('exchange_rates').find((r) => r.from_currency === v.from_currency && r.to_currency === v.to_currency);
      if (dup) store.update('exchange_rates', dup.id, { rate: v.rate, last_updated: Date.now() }); else store.insert('exchange_rates', { ...v, last_updated: Date.now() });
      toast('Rate saved'); } }),
    editRate: (el) => { const r = store.get('exchange_rates', el.dataset.id); return formModal(`1 ${r.from_currency} = ? ${r.to_currency}`, [{ key: 'rate', label: 'Rate', type: 'number', required: true }], r, { onSubmit: (v) => { if (!(v.rate > 0)) throw new Error('Rate must be positive'); store.update('exchange_rates', r.id, { rate: v.rate, last_updated: Date.now() }); toast('Rate updated'); } }); },
    delRate: async (el) => { if (await confirmDialog('Delete rate?', 'Conversions will fall back to reference rates.')) store.remove('exchange_rates', el.dataset.id); },
    revoke: async (el) => { if (await confirmDialog('Sign out that device?', 'It will need the password to sync again.', 'Sign out', 'danger')) { const { api } = await import('./store.js'); await api(`/api/devices/${el.dataset.id}`, { method: 'DELETE', token: store.auth.token }); devicesCache = null; toast('Device signed out'); store.emit(); } },
    changePassword: async () => { const { api } = await import('./store.js'); return formModal('Change password', [{ key: 'currentPassword', label: 'Current password', type: 'password', required: true, autocomplete: 'current-password' }, { key: 'newPassword', label: 'New password (10+ characters)', type: 'password', required: true, autocomplete: 'new-password' }], {}, { onSubmit: async (v) => { await api('/api/password', { method: 'POST', body: v, token: store.auth.token }); toast('Password changed'); } }); },
    exportJson: () => { const data = { exportedAt: Date.now(), baseCurrency: L.base(), ...Object.fromEntries(Object.keys(store.tables).map((t) => [t, store.all(t)])) }; download(`freefinance-${new Date().toISOString().slice(0, 10)}.json`, JSON.stringify(data, null, 2), 'application/json'); },
    exportCsv: () => {
      const q = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`;
      const rows = [['Date', 'Type', 'Amount', 'Currency', 'Account', 'To account', 'Category', 'Status', 'Recurrence', 'Labels', 'Note']].concat(
        store.all('transactions').sort((a, b) => a.date - b.date).map((t) => [new Date(t.date).toISOString(), t.type, t.amount, store.get('accounts', t.account_id)?.currency_code || '', accName(t.account_id), t.to_account_id ? accName(t.to_account_id) : '', catOf(t.category_id)?.name || '', t.status, t.recurrence_rule, L.labelsOf(t).join('; '), t.note || '']));
      download(`freefinance-transactions-${new Date().toISOString().slice(0, 10)}.csv`, rows.map((r) => r.map(q).join(',')).join('\n'), 'text/csv');
    },
  },
  changes: {
    baseCurrency: (el) => { store.setPref('base_currency', el.value); toast(`Base currency: ${el.value}`); },
    theme: (el) => { viewPrefs.set('theme', el.value); document.documentElement.dataset.theme = el.value; },
    hideMoney: (el) => { viewPrefs.set('hideMoney', el.checked); store.emit(); },
  },
};
function download(name, text, type) {
  const url = URL.createObjectURL(new Blob([text], { type }));
  const a = Object.assign(document.createElement('a'), { href: url, download: name });
  document.body.appendChild(a); a.click(); a.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export const PAGES = { dashboard, accounts, account: accountDetail, records, categories, budgets, debts, subscriptions, commissions, assets, cards, analytics, receipts, settings };
export const NAV = [
  ['dashboard', '◧', 'Dashboard'], ['accounts', '🏦', 'Accounts'], ['records', '☰', 'Records'], ['budgets', '◔', 'Budgets'],
  ['subscriptions', '↻', 'Subscriptions'], ['debts', '⇆', 'Debts'], ['commissions', '💼', 'Commissions'], ['assets', '💎', 'Assets'],
  ['cards', '💳', 'Cards'], ['analytics', '📊', 'Analytics'], ['receipts', '🧾', 'Receipts'], ['categories', '🏷', 'Categories'], ['settings', '⚙', 'Settings'],
];
