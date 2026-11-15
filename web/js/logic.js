// Finance logic mirrored from the Android app (FinanceRepository, DAOs, DashboardViewModel,
// CurrencyUtils, RecurrenceHelper, ForecastCashFlowUseCase) so both clients show the same numbers.
import { store } from './store.js';

// ---------------- currencies (CurrencyUtils) ----------------
export const CURRENCIES = [
  ['USD', 'US Dollar', '$', 1.0], ['EUR', 'Euro', '€', 1.087], ['GBP', 'British Pound', '£', 1.265],
  ['JPY', 'Japanese Yen', '¥', 0.00645], ['CAD', 'Canadian Dollar', 'CA$', 0.725], ['AUD', 'Australian Dollar', 'A$', 0.654],
  ['CHF', 'Swiss Franc', 'CHF', 1.111], ['CNY', 'Chinese Yuan', '¥', 0.138], ['INR', 'Indian Rupee', '₹', 0.012],
  ['PHP', 'Philippine Peso', '₱', 0.0172], ['SGD', 'Singapore Dollar', 'S$', 0.741], ['NZD', 'New Zealand Dollar', 'NZ$', 0.605],
  ['BRL', 'Brazilian Real', 'R$', 0.182], ['MXN', 'Mexican Peso', 'Mex$', 0.054],
].map(([code, name, symbol, usd]) => ({ code, name, symbol, usd }));
const CUR = Object.fromEntries(CURRENCIES.map((c) => [c.code, c]));
const SYMBOLS = { PHP: '₱', USD: '$', EUR: '€', GBP: '£', JPY: '¥', CAD: 'CA$', AUD: 'A$', CHF: 'CHF', CNY: '¥', INR: '₹', SGD: 'S$',
  NZD: 'NZ$', BRL: 'R$', MXN: 'Mex$', KRW: '₩', HKD: 'HK$', THB: '฿', VND: '₫', IDR: 'Rp', MYR: 'RM', TRY: '₺', RUB: '₽',
  SEK: 'kr', NOK: 'kr', DKK: 'kr', PLN: 'zł', ZAR: 'R', AED: 'AED', SAR: 'SAR' };
export const symbol = (code) => SYMBOLS[String(code || '').toUpperCase()] || String(code || '').toUpperCase();

export function convert(amount, from, to, rates = store.all('exchange_rates')) {
  from = String(from || '').toUpperCase().trim(); to = String(to || '').toUpperCase().trim();
  if (from === to) return amount;
  const direct = rates.find((r) => r.from_currency.toUpperCase() === from && r.to_currency.toUpperCase() === to);
  if (direct && direct.rate > 0) return amount * direct.rate;
  const inverse = rates.find((r) => r.from_currency.toUpperCase() === to && r.to_currency.toUpperCase() === from);
  if (inverse && inverse.rate > 0) return amount / inverse.rate;
  const f = CUR[from]?.usd ?? 1, t = CUR[to]?.usd ?? 1;
  return t <= 0 ? amount : (amount * f) / t;
}
export const base = () => store.prefs.base_currency || 'PHP';

// ---------------- enums ----------------
export const ACCOUNT_TYPES = ['Cash', 'Bank Account', 'E-Wallet', 'Savings', 'Locked Deposit / Investment'];
export const SUB_ACCOUNT_TYPES = ['Time Deposit', 'Savings', 'Wallet', 'Pocket', 'Stash', 'Vault', 'Investment'];
export const COMPOUNDING = ['NONE', 'DAILY', 'MONTHLY', 'ANNUALLY'];
export const RECURRENCE = ['None', 'Daily', 'Weekdays', 'Weekends', 'Weekly', 'Biweekly', 'Monthly', 'Monthly:Last', 'Yearly'];
export const ASSET_CATEGORIES = ['Vehicle', 'Electronics', 'Real Estate', 'Jewelry', 'Precious Metals', 'Collectibles', 'Equipment', 'Other'];
export const CARD_TYPES = [['DEBIT', 'Debit Card'], ['CREDIT', 'Credit Card'], ['VIRTUAL', 'Virtual'], ['PREPAID', 'Prepaid']];
export const CARD_NETWORKS = ['VISA', 'MASTERCARD', 'AMEX', 'JCB', 'UNIONPAY', 'OTHER'];
export const CARD_FINISHES = ['OBSIDIAN_GOLD', 'MATTE_CARBON', 'ROYAL_SAPPHIRE', 'EMERALD_GLASS', 'CRIMSON_TITANIUM', 'CYBER_VIOLET', 'OCEAN_TEAL', 'BRONZE_LUXE'];
export const ICONS = {
  ic_food: '🍔', ic_transport: '🚗', ic_home: '🏠', ic_utilities: '💡', ic_entertainment: '🎬', ic_health: '🩺',
  ic_salary: '💼', ic_freelance: '🧑‍💻', ic_investment: '📈', ic_motortaxi: '🛵', ic_tech: '💻', ic_electronics: '🔌',
  ic_shopping: '🛍️', ic_savings: '🐷', ic_education: '🎓', ic_category: '🏷️', ic_asset: '💎', ic_gift: '🎁',
  ic_travel: '✈️', ic_pets: '🐾', ic_bills: '🧾', ic_coffee: '☕', ic_groceries: '🛒', ic_phone: '📱',
};
export const icon = (k) => ICONS[k] || '🏷️';

const up = (s) => String(s || '').toUpperCase();
export const isCleared = (t) => up(t.status) === 'CLEARED';
export const labelsOf = (t) => { try { const a = JSON.parse(t.labels || '[]'); return Array.isArray(a) ? a : []; } catch { return String(t.labels || '').split(',').map((x) => x.trim()).filter(Boolean); } };

// ---------------- balances (AccountDao.recalculateAccountBalance) ----------------
let _balCache = { v: -1, map: new Map() };
export function balances() {
  if (_balCache.v === store.version) return _balCache.map;
  const map = new Map();
  for (const a of store.all('accounts')) map.set(a.id, a.initial_balance || 0);
  for (const t of store.all('transactions')) {
    if (!isCleared(t)) continue;
    const ty = up(t.type), amt = Number(t.amount) || 0;
    if (ty === 'INCOME' && map.has(t.account_id)) map.set(t.account_id, map.get(t.account_id) + amt);
    else if (ty === 'EXPENSE' && map.has(t.account_id)) map.set(t.account_id, map.get(t.account_id) - amt);
    else if (ty === 'TRANSFER') {
      if (map.has(t.account_id)) map.set(t.account_id, map.get(t.account_id) - amt);
      if (t.to_account_id != null && map.has(t.to_account_id)) map.set(t.to_account_id, map.get(t.to_account_id) + amt);
    }
  }
  _balCache = { v: store.version, map };
  return map;
}
export const balanceOf = (acc) => balances().get(acc.id) ?? acc.current_balance ?? 0;
export function clearedSumFor(accountId) {
  return balanceOf(store.get('accounts', accountId)) - (store.get('accounts', accountId)?.initial_balance || 0);
}

// ---------------- dashboard numbers (DashboardViewModel.uiState) ----------------
export function netWorth(includeAssets) {
  const b = base();
  const tally = store.all('accounts').filter((a) => a.include_in_tally);
  const cash = tally.reduce((s, a) => s + convert(balanceOf(a), a.currency_code, b), 0);
  const assets = store.all('assets').filter((x) => x.include_in_net_worth).reduce((s, x) => s + convert(x.estimated_value, x.currency_code, b), 0);
  return { cash, assets, total: includeAssets ? cash + assets : cash, excluded: store.all('accounts').length - tally.length };
}

export function moneyFlow(from = 0, to = Infinity) {
  let income = 0, expense = 0;
  for (const t of store.all('transactions')) {
    if (!isCleared(t) || t.date < from || t.date > to) continue;
    if (t.type === 'Income') income += t.amount; else if (t.type === 'Expense') expense += t.amount;
  }
  return { income, expense, net: income - expense };
}

export function liquidVsLocked() {
  let spendable = 0, locked = 0; const n = Date.now();
  for (const a of store.all('accounts').filter((x) => x.include_in_tally)) {
    const isLocked = a.is_locked || (a.locked_until && a.locked_until > n) || up(a.type) === up('Locked Deposit / Investment');
    if (isLocked) locked += balanceOf(a); else spendable += balanceOf(a);
  }
  const total = spendable + locked;
  return { spendable, locked, total, ratio: total > 0 ? Math.min(1, Math.max(0, spendable / total)) : 0.5 };
}

// BudgetDao.getBudgetProgressByCategory
export function budgetProgress() {
  return store.all('budgets').map((b) => {
    const c = store.get('categories', b.category_id);
    const spent = store.all('transactions')
      .filter((t) => t.category_id === b.category_id && t.type === 'Expense' && t.status === 'Cleared' && t.date >= b.start_date && t.date <= b.end_date)
      .reduce((s, t) => s + t.amount, 0);
    return { budget: b, category: c, spent, remaining: b.amount_limit - spent, ratio: b.amount_limit > 0 ? spent / b.amount_limit : 0 };
  }).filter((x) => x.category).sort((a, b) => b.ratio - a.ratio);
}

// ---------------- recurrence (RecurrenceHelper) ----------------
const DAY = 86400000;
const startOfDay = (d) => { const x = new Date(d); x.setHours(0, 0, 0, 0); return x; };
export function occurrencesInMonth(rule, startMs, month, year) {
  if (!rule || up(rule) === 'NONE') return [];
  const s = new Date(startMs), sy = s.getFullYear(), sm = s.getMonth(), sd = s.getDate();
  if (year < sy || (year === sy && month < sm)) return [];
  const dim = new Date(year, month + 1, 0).getDate(), out = [], r = rule.trim(), R = up(r);
  const DOW = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
  for (let day = 1; day <= dim; day++) {
    const c = new Date(year, month, day, s.getHours(), s.getMinutes(), s.getSeconds()), w = c.getDay();
    let m;
    if (R === 'DAILY') m = true;
    else if (R === 'WEEKDAYS') m = w >= 1 && w <= 5;
    else if (R === 'WEEKENDS') m = w === 0 || w === 6;
    else if (R === 'WEEKLY') m = w === s.getDay();
    else if (R === 'BIWEEKLY' || R === 'BI-WEEKLY') { const diff = Math.trunc((c - s) / DAY); m = diff >= 0 && diff % 14 === 0; }
    else if (R.startsWith('CUSTOM:')) m = R.slice(7).includes(DOW[w]);
    else if (R.startsWith('MONTHLY:')) { const spec = r.slice(8).trim(); m = up(spec) === 'LAST' ? day === dim : day === Math.min(Math.max(parseInt(spec, 10) || sd, 1), dim); }
    else if (R === 'MONTHLY') m = day === Math.min(sd, dim);
    else if (R === 'YEARLY' || R === 'ANNUAL') m = month === sm && day === Math.min(sd, dim);
    else m = day === Math.min(sd, dim);
    if (m) out.push(day);
  }
  return out;
}
export function monthlyEquivalent(amount, rule) {
  if (!rule || up(rule) === 'NONE') return amount;
  const R = up(rule.trim());
  if (R === 'DAILY') return amount * 30.4;
  if (R === 'WEEKDAYS') return amount * 21.7;
  if (R === 'WEEKENDS') return amount * 8.7;
  if (R === 'WEEKLY') return amount * 4.333;
  if (R === 'BIWEEKLY' || R === 'BI-WEEKLY') return amount * 2.166;
  if (R.startsWith('CUSTOM:')) return amount * (Math.max(1, rule.slice(7).split(',').length) * 4.333);
  if (R.startsWith('MONTHLY')) return amount;
  if (R === 'YEARLY' || R === 'ANNUAL') return amount / 12;
  return amount;
}
export function nextDueDate(startMs, rule) {
  if (!rule || up(rule) === 'NONE') return startMs;
  const s = new Date(startMs), c = startOfDay(Date.now());
  c.setHours(s.getHours(), s.getMinutes(), 0, 0);
  for (let i = 0; i <= 90; i++) {
    if (occurrencesInMonth(rule, startMs, c.getMonth(), c.getFullYear()).includes(c.getDate())) {
      if (c.getTime() >= Date.now() - 60000 || i > 0) return c.getTime();
    }
    c.setDate(c.getDate() + 1);
  }
  return startMs;
}
export function scheduleLabel(rule, ms) {
  const d = new Date(ms), time = d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  const R = up(rule || 'None');
  if (R === 'NONE') return `One-time (${d.toLocaleDateString([], { month: 'short', day: 'numeric' })})`;
  if (R === 'DAILY') return `Daily at ${time}`;
  if (R === 'WEEKDAYS') return `Weekdays (Mon–Fri) at ${time}`;
  if (R === 'WEEKENDS') return `Weekends (Sat–Sun) at ${time}`;
  if (R === 'WEEKLY') return `Every ${d.toLocaleDateString([], { weekday: 'long' })} at ${time}`;
  if (R === 'BIWEEKLY' || R === 'BI-WEEKLY') return `Every 2 weeks (${d.toLocaleDateString([], { weekday: 'long' })})`;
  if (R.startsWith('CUSTOM:')) return `Every ${rule.slice(7).trim()} at ${time}`;
  if (R === 'MONTHLY:LAST') return `Monthly on the last day`;
  if (R.startsWith('MONTHLY')) return `Monthly on the ${ordinal(parseInt(rule.split(':')[1], 10) || d.getDate())}`;
  if (R === 'YEARLY' || R === 'ANNUAL') return `Yearly on ${d.toLocaleDateString([], { month: 'short', day: 'numeric' })}`;
  return rule;
}
export const ordinal = (n) => (n % 100 >= 11 && n % 100 <= 13 ? `${n}th` : `${n}${['th', 'st', 'nd', 'rd'][n % 10] || 'th'}`);

export const recurringTemplates = () => store.all('transactions')
  .filter((t) => up(t.recurrence_rule) !== 'NONE' && t.recurrence_rule != null && t.status === 'Planned');

// ---------------- forecast (ForecastCashFlowUseCase) ----------------
export function forecast(days = 30, includeAssets = false) {
  const scheduled = store.all('transactions').filter((t) => t.status === 'Planned' || (t.recurrence_rule && t.recurrence_rule !== 'None'));
  const pendingMs = store.all('milestones').filter((m) => up(m.status) === 'PENDING');
  const accounts = store.all('accounts');
  let running = netWorth(includeAssets).total;
  const out = [];
  const day0 = startOfDay(Date.now());
  for (let i = 0; i < days; i++) {
    const target = new Date(day0); target.setDate(day0.getDate() + i);
    let delta = 0;
    for (const t of scheduled) {
      const td = startOfDay(t.date);
      if (target < td) continue;
      const rule = (t.recurrence_rule || 'None').trim();
      let occurs;
      if (up(rule) === 'NONE') occurs = up(t.status) === 'PLANNED' && +td === +target;
      else occurs = occurrencesInMonth(rule, t.date, target.getMonth(), target.getFullYear()).includes(target.getDate());
      if (occurs) delta += t.type === 'Income' ? t.amount : t.type === 'Expense' ? -t.amount : 0;
    }
    for (const m of pendingMs) if (+startOfDay(m.expected_date) === +target) delta += m.amount;
    for (const a of accounts) {
      const bal = balanceOf(a);
      if (a.interest_rate_pa > 0 && bal > 0 && a.compounding_frequency !== 'NONE') delta += bal * (a.interest_rate_pa / 100 / 365);
    }
    running += delta;
    out.push({ date: +target, balance: running, delta });
  }
  return out;
}

// ---------------- analytics ----------------
export function monthlyTrends(months = 6) {
  const out = [], n = new Date();
  for (let i = months - 1; i >= 0; i--) {
    const s = new Date(n.getFullYear(), n.getMonth() - i, 1), e = new Date(n.getFullYear(), n.getMonth() - i + 1, 1);
    const f = moneyFlow(+s, +e - 1);
    out.push({ label: s.toLocaleDateString([], { month: 'short' }), start: +s, ...f });
  }
  return out;
}
export function spendingByCategory(from, to) {
  const map = new Map();
  for (const t of store.all('transactions')) {
    if (t.type !== 'Expense' || !isCleared(t) || t.date < from || t.date > to) continue;
    const k = t.category_id ?? 0;
    map.set(k, (map.get(k) || 0) + t.amount);
  }
  return [...map.entries()].map(([id, total]) => ({ category: store.get('categories', id), total })).sort((a, b) => b.total - a.total);
}

// ---------------- multi-row operations (FinanceRepository) ----------------
export function addTransaction(fields) {
  return store.insert('transactions', { recurrence_rule: 'None', labels: '[]', ...fields });
}
function quickTx(amount, type, accountId, labels, note, categoryId = null) {
  return addTransaction({ amount, type, account_id: Number(accountId), category_id: categoryId, date: Date.now(), status: 'Cleared',
    labels: JSON.stringify(labels), note });
}
export function settleDebt(debt, accountId) {
  const amt = debt.remaining_amount;
  store.update('debts', debt.id, { remaining_amount: 0, status: 'Settled' });
  if (accountId && amt > 0.0001) {
    const lent = up(debt.type) === 'LENT';
    quickTx(amt, lent ? 'Income' : 'Expense', accountId, ['Debt Repayment', lent ? 'Lent' : 'Borrowed', debt.person_name],
      lent ? `Received Lent Repayment from ${debt.person_name}` : `Repaid Borrowed Debt to ${debt.person_name}`);
  }
}
export function payDebtPortion(debt, payment, accountId) {
  const actual = Math.max(0, Math.min(payment, debt.remaining_amount));
  const remaining = Math.max(0, debt.remaining_amount - actual);
  store.update('debts', debt.id, { remaining_amount: remaining, status: remaining <= 0.0001 ? 'Settled' : 'Active' });
  if (accountId && actual > 0.0001) {
    const lent = up(debt.type) === 'LENT';
    quickTx(actual, lent ? 'Income' : 'Expense', accountId, ['Debt Repayment', lent ? 'Lent' : 'Borrowed', debt.person_name],
      lent ? `Received Partial Lent Repayment from ${debt.person_name}` : `Partial Repayment for Borrowed Debt to ${debt.person_name}`);
  }
}
export function markMilestonePaid(m, accountId) {
  if (up(m.status) === 'PAID') return;
  quickTx(m.amount, 'Income', accountId, ['Freelance', 'Milestone'], `Freelance Milestone: ${m.title}`);
  store.update('milestones', m.id, { status: 'Paid', paid_at: Date.now() });
}
export function markPayoutPaid(p, accountId, actualAmount) {
  const tx = addTransaction({ amount: actualAmount ?? p.amount, type: 'Expense', date: Date.now(), status: 'Cleared',
    account_id: Number(accountId), category_id: p.category_id, labels: JSON.stringify(['Planned Payout', p.title]),
    note: `Planned Payout: ${p.title}${p.payee_or_vendor ? ` (${p.payee_or_vendor})` : ''}` });
  store.update('planned_payouts', p.id, { status: 'PAID', paid_date: Date.now(), paid_transaction_id: tx.id });
}
export function unmarkPayoutPaid(p) {
  if (p.paid_transaction_id) store.remove('transactions', p.paid_transaction_id);
  store.update('planned_payouts', p.id, { status: 'PENDING', paid_date: null, paid_transaction_id: null });
}
export const lastFour = (c) => { const d = String(c.card_number || '').replace(/\D/g, ''); return d.length >= 4 ? d.slice(-4) : d.padStart(4, '•') || '••••'; };
export function logCardCharge(card, amount, categoryId, note, overrideAccountId) {
  const clean = `[${card.card_name} ••${lastFour(card)}]${note ? ' ' + note.trim() : ''}`;
  if (up(card.card_type) === 'CREDIT') {
    store.update('payment_cards', card.id, { current_credit_used: Math.max(0, card.current_credit_used + amount) });
    if (overrideAccountId) quickTx(amount, 'Expense', overrideAccountId, ['Card Spend', 'Credit Card', card.card_name], clean, categoryId);
  } else {
    const acc = overrideAccountId || card.linked_account_id;
    if (!acc) throw new Error('This card has no linked account — choose one.');
    quickTx(amount, 'Expense', acc, ['Card Spend', card.card_type, card.card_name], clean, categoryId);
  }
}
export function payCardBill(card, amount, sourceAccountId, note) {
  if (amount <= 0) return;
  const stmt = Math.max(0, card.statement_balance - amount);
  store.update('payment_cards', card.id, { current_credit_used: Math.max(0, card.current_credit_used - amount), statement_balance: stmt,
    minimum_payment_due: stmt <= 0 ? 0 : Math.max(0, card.minimum_payment_due - amount) });
  quickTx(amount, 'Expense', sourceAccountId, ['Credit Card Payment', card.card_name],
    note?.trim() || `Credit Card Bill Payment: ${card.card_name} (••${lastFour(card)})`);
}
export function convertReceipt(r, { accountId, amount, type, categoryId, date, note }) {
  const tx = addTransaction({ account_id: Number(accountId), type, amount, category_id: categoryId, date, status: 'Cleared', labels: '["Receipt"]', note });
  store.update('receipts_inbox', r.id, { status: 'CONVERTED', converted_transaction_id: tx.id, suggested_amount: amount, suggested_type: type });
}
// FinanceRepository.adjustAccountBalance: keep current = initial + cleared sum
export function adjustAccountBalance(acc, target) {
  const sum = balanceOf(acc) - (acc.initial_balance || 0);
  store.update('accounts', acc.id, { initial_balance: target - sum, current_balance: target });
}
