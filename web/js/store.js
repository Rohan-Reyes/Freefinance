// FreeFinance web — data store + two-way sync with the FreeFinance sync server.
// Rows use exactly the Android Room column names/encodings:
//   dates = epoch ms, booleans = 0/1, labels = JSON string, colors = signed ARGB ints.

export const SPAN = 2 ** 40;
const now = () => Date.now();

// Column defaults mirror the Room entities (all NOT NULL columns present).
export const SCHEMA = {
  accounts: () => ({ name: '', type: 'Cash', initial_balance: 0, current_balance: 0, color: -16738120, interest_rate_pa: 0,
    compounding_frequency: 'NONE', currency_code: 'PHP', is_locked: 0, locked_until: null, linked_app_package: null,
    include_in_tally: 1, parent_account_id: null, sub_account_type: null }),
  transactions: () => ({ account_id: null, to_account_id: null, type: 'Expense', amount: 0, category_id: null, date: now(),
    status: 'Cleared', recurrence_rule: 'None', labels: '[]', note: null, proof_image_uri: null }),
  categories: () => ({ name: '', icon_res_id: 'ic_category', type: 'Expense', color: -16738120, parent_category_id: null }),
  budgets: () => ({ category_id: null, amount_limit: 0, period: 'Monthly', start_date: now(), end_date: now() }),
  debts: () => ({ person_name: '', type: 'Lent', total_amount: 0, remaining_amount: 0, due_date: null, status: 'Active',
    notes: null, created_at: now() }),
  freelance_projects: () => ({ project_name: '', client: '', total_expected_fee: 0, status: 'Active', created_at: now() }),
  milestones: () => ({ project_id: null, title: '', amount: 0, expected_date: now(), status: 'Pending', paid_at: null }),
  exchange_rates: () => ({ from_currency: 'USD', to_currency: 'PHP', rate: 1, last_updated: now() }),
  receipts_inbox: () => ({ image_path: '', captured_at: now(), note: null, suggested_amount: null, suggested_type: 'Expense',
    status: 'PENDING', converted_transaction_id: null }),
  assets: () => ({ name: '', category: 'Other', estimated_value: 0, purchase_price: 0, purchase_date: null, currency_code: 'PHP',
    notes: null, icon_name: 'ic_asset', color: -16738120, include_in_net_worth: 1, created_at: now() }),
  planned_payouts: () => ({ title: '', amount: 0, currency_code: 'PHP', due_date: now(), target_account_id: null, category_id: null,
    payee_or_vendor: null, notes: null, priority: 'NORMAL', status: 'PENDING', paid_date: null, paid_transaction_id: null,
    created_at: now() }),
  payment_cards: () => ({ card_name: '', card_type: 'DEBIT', card_network: 'VISA', issuing_bank: '', cardholder_name: '',
    card_number: '', expiry_month: 12, expiry_year: 28, cvv: '', card_pin_hint: null, billing_address: null,
    customer_service_phone: null, currency_code: 'PHP', color: -14579213, card_finish: 'OBSIDIAN_GOLD', is_virtual: 0,
    is_frozen: 0, linked_account_id: null, daily_spend_limit: 0, credit_limit: 0, current_credit_used: 0, statement_balance: 0,
    minimum_payment_due: 0, statement_day: null, payment_due_day: null, apr_interest_rate: 0, annual_fee: 0,
    rewards_program: null, notes: null, created_at: now() }),
};
export const TABLE_NAMES = Object.keys(SCHEMA);

// Same referential actions as the Room foreign keys (applied locally on delete).
const CASCADES = {
  accounts: [{ tbl: 'transactions', col: 'account_id', action: 'delete' }],
  categories: [
    { tbl: 'transactions', col: 'category_id', action: 'null' },
    { tbl: 'budgets', col: 'category_id', action: 'delete' },
    { tbl: 'categories', col: 'parent_category_id', action: 'null' },
  ],
  freelance_projects: [{ tbl: 'milestones', col: 'project_id', action: 'delete' }],
};

// ---------------- tiny IndexedDB key/value ----------------
const idb = {
  db: null,
  open() {
    if (this.db) return Promise.resolve(this.db);
    return new Promise((res, rej) => {
      const r = indexedDB.open('freefinance', 1);
      r.onupgradeneeded = () => r.result.createObjectStore('kv');
      r.onsuccess = () => { this.db = r.result; res(this.db); };
      r.onerror = () => rej(r.error);
    });
  },
  async get(k) {
    const db = await this.open();
    return new Promise((res, rej) => {
      const r = db.transaction('kv').objectStore('kv').get(k);
      r.onsuccess = () => res(r.result); r.onerror = () => rej(r.error);
    });
  },
  async set(k, v) {
    const db = await this.open();
    return new Promise((res, rej) => {
      const t = db.transaction('kv', 'readwrite'); t.objectStore('kv').put(v, k);
      t.oncomplete = () => res(); t.onerror = () => rej(t.error);
    });
  },
  async clear() {
    const db = await this.open();
    return new Promise((res) => { const t = db.transaction('kv', 'readwrite'); t.objectStore('kv').clear(); t.oncomplete = () => res(); });
  },
};

export class ApiError extends Error { constructor(msg, status) { super(msg); this.status = status; } }

export async function api(path, { method = 'GET', body, token } = {}) {
  let res;
  try {
    res = await fetch(path, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError('Offline — changes are saved and will sync when the server is reachable', 0);
  }
  const json = await res.json().catch(() => ({}));
  if (!res.ok) throw new ApiError(json.error || `Server error (${res.status})`, res.status);
  return json;
}

// ---------------- Store ----------------
class Store {
  constructor() {
    this.tables = Object.fromEntries(TABLE_NAMES.map((t) => [t, new Map()]));
    this.pending = new Map();      // "tbl:id" -> { tbl, id, ts, deleted }
    this.cursor = 0;
    this.counter = 0;               // local id counter inside this device's id range
    this.prefs = { base_currency: 'PHP' };
    this.prefsTs = 0;
    this.auth = JSON.parse(localStorage.getItem('ff_auth') || 'null');
    this.status = { syncing: false, lastSync: 0, error: null, online: true };
    this.version = 0;
    this.listeners = new Set();
    this.statusListeners = new Set();
    this._saveTimer = null;
    this._syncTimer = null;
    this._syncing = null;
    this._again = false;
  }

  subscribe(fn) { this.listeners.add(fn); return () => this.listeners.delete(fn); }
  emit() { this.version++; for (const fn of this.listeners) fn(); }
  /** Sync status changes (syncing / synced / error) only: the page is not re-rendered for these. */
  onStatus(fn) { this.statusListeners.add(fn); return () => this.statusListeners.delete(fn); }
  emitStatus() { for (const fn of this.statusListeners) fn(); }

  // ---- persistence ----
  async load() {
    if (!this.auth) return;
    const saved = await idb.get(`state:${this.auth.username}:${this.auth.deviceNo}`).catch(() => null);
    if (saved) {
      for (const t of TABLE_NAMES) this.tables[t] = new Map((saved.tables?.[t] || []).map((r) => [r.id, r]));
      this.pending = new Map((saved.pending || []).map((p) => [`${p.tbl}:${p.id}`, p]));
      this.cursor = saved.cursor || 0;
      this.counter = saved.counter || 0;
      this.prefs = { ...this.prefs, ...(saved.prefs || {}) };
      this.prefsTs = saved.prefsTs || 0;
    }
    this.recomputeCounter();
    this.emit();
  }
  scheduleSave() {
    clearTimeout(this._saveTimer);
    this._saveTimer = setTimeout(() => this.save(), 400);
  }
  async save() {
    if (!this.auth) return;
    const data = {
      tables: Object.fromEntries(TABLE_NAMES.map((t) => [t, [...this.tables[t].values()]])),
      pending: [...this.pending.values()], cursor: this.cursor, counter: this.counter,
      prefs: this.prefs, prefsTs: this.prefsTs,
    };
    await idb.set(`state:${this.auth.username}:${this.auth.deviceNo}`, data).catch(() => {});
  }
  recomputeCounter() {
    if (!this.auth) return;
    const lo = this.auth.deviceNo * SPAN, hi = lo + SPAN;
    let max = this.counter;
    for (const t of TABLE_NAMES) for (const id of this.tables[t].keys()) if (id >= lo && id < hi) max = Math.max(max, id - lo);
    this.counter = max;
  }

  // ---- auth ----
  setAuth(a) {
    this.auth = a;
    localStorage.setItem('ff_auth', JSON.stringify(a));
  }
  async signOut() {
    if (this.auth) {
      try { await this.sync(); } catch { /* best effort */ }
      await idb.set(`state:${this.auth.username}:${this.auth.deviceNo}`, null).catch(() => {});
    }
    localStorage.removeItem('ff_auth');
    location.reload();
  }

  // ---- reads ----
  all(tbl) { return [...this.tables[tbl].values()]; }
  get(tbl, id) { return id == null ? undefined : this.tables[tbl].get(Number(id)); }

  // ---- writes (local first, queued for sync) ----
  nextId() { this.counter += 1; return this.auth.deviceNo * SPAN + this.counter; }
  queue(tbl, id, deleted = false) {
    this.pending.set(`${tbl}:${id}`, { tbl, id, ts: now(), deleted });
  }
  insert(tbl, data) {
    const row = { ...SCHEMA[tbl](), ...data, id: this.nextId() };
    this.tables[tbl].set(row.id, row);
    this.queue(tbl, row.id);
    this.changed();
    return row;
  }
  update(tbl, id, patch) {
    const cur = this.get(tbl, id);
    if (!cur) return null;
    const row = { ...cur, ...patch, id: cur.id };
    this.tables[tbl].set(row.id, row);
    this.queue(tbl, row.id);
    this.changed();
    return row;
  }
  /** Writes a full row with a given id (used by undo to restore exactly what was there). */
  put(tbl, row) {
    this.tables[tbl].set(row.id, { ...SCHEMA[tbl](), ...row });
    this.queue(tbl, row.id);
    this.changed();
    return row;
  }
  remove(tbl, id) {
    id = Number(id);
    if (!this.tables[tbl].has(id)) return;
    this.cascade(tbl, id, true);
    this.tables[tbl].delete(id);
    this.queue(tbl, id, true);
    this.changed();
  }
  cascade(tbl, id, queueChanges) {
    for (const c of CASCADES[tbl] || []) {
      for (const row of [...this.tables[c.tbl].values()]) {
        if (Number(row[c.col]) !== id || (c.tbl === tbl && row.id === id)) continue;
        if (c.action === 'delete') {
          this.cascade(c.tbl, row.id, queueChanges);
          this.tables[c.tbl].delete(row.id);
          if (queueChanges) this.queue(c.tbl, row.id, true);
        } else {
          this.tables[c.tbl].set(row.id, { ...row, [c.col]: null });
          if (queueChanges) this.queue(c.tbl, row.id);
        }
      }
    }
  }
  setPref(key, value) { this.setPrefs({ [key]: value }); }
  setPrefs(patch) {
    this.prefs = { ...this.prefs, ...patch };
    this.prefsTs = now();
    this.pending.set('prefs:1', { tbl: 'prefs', id: 1, ts: this.prefsTs, deleted: false });
    this.changed();
  }
  changed() {
    this.emit();
    this.scheduleSave();
    clearTimeout(this._syncTimer);
    this._syncTimer = setTimeout(() => this.sync().catch(() => {}), 800);
  }

  // ---- sync ----
  async sync() {
    if (!this.auth) return;
    if (this._syncing) { this._again = true; return this._syncing; }
    this.status = { ...this.status, syncing: true };
    this.emitStatus();
    this._syncing = (async () => {
      try {
        await this.push();
        await this.pull();
        this.status = { syncing: false, lastSync: now(), error: null, online: true };
      } catch (e) {
        this.status = { ...this.status, syncing: false, error: e.message, online: e.status !== 0 };
        if (e.status === 401) { localStorage.removeItem('ff_auth'); this.auth = null; }
        throw e;
      } finally {
        this._syncing = null;
        this.emitStatus();
        this.scheduleSave();
        if (this._again) { this._again = false; this.sync().catch(() => {}); }
      }
    })();
    return this._syncing;
  }

  async push() {
    const entries = [...this.pending.values()];
    for (let i = 0; i < entries.length; i += 500) {
      const batch = entries.slice(i, i + 500);
      const changes = batch.map((p) => {
        if (p.tbl === 'prefs') return { tbl: 'prefs', id: 1, ts: p.ts, data: { ...this.prefs } };
        const row = this.get(p.tbl, p.id);
        if (p.deleted || !row) return { tbl: p.tbl, id: p.id, ts: p.ts, deleted: true };
        return { tbl: p.tbl, id: p.id, ts: p.ts, data: { ...row } };
      });
      await api('/api/sync/push', { method: 'POST', body: { changes }, token: this.auth.token });
      for (const p of batch) {
        const key = `${p.tbl}:${p.id}`;
        if (this.pending.get(key)?.ts === p.ts) this.pending.delete(key); // keep edits made during the request
      }
    }
  }

  async pull() {
    let more = true, guard = 0, changed = false;
    while (more && guard++ < 500) {
      const res = await api(`/api/sync/pull?since=${this.cursor}`, { token: this.auth.token });
      for (const r of res.records) if (this.applyRemote(r)) changed = true;
      this.cursor = res.cursor;
      more = res.hasMore;
    }
    if (changed) this.emit();
  }

  applyRemote(r) {
    const key = `${r.tbl}:${r.id}`;
    const pend = this.pending.get(key);
    if (pend && pend.ts > r.ts) return false; // our newer local edit wins; it will be pushed
    if (pend) this.pending.delete(key);
    if (r.tbl === 'prefs') {
      if (!r.deleted && r.data) { this.prefs = { ...this.prefs, ...r.data }; this.prefsTs = r.ts; }
      return true;
    }
    if (!this.tables[r.tbl]) return false;
    if (r.deleted) {
      this.cascade(r.tbl, r.id, false);
      this.tables[r.tbl].delete(r.id);
    } else {
      // Keep rows complete even if they came from an older client that lacks newer columns.
      this.tables[r.tbl].set(r.id, { ...SCHEMA[r.tbl](), ...r.data, id: r.id });
    }
    return true;
  }

  async resetLocalCache() {
    await idb.clear();
    for (const t of TABLE_NAMES) this.tables[t] = new Map();
    this.pending = new Map(); this.cursor = 0;
    await this.sync();
  }
}

export const store = new Store();
