// UI helpers: escaping, formatting, modals/forms, toasts, small SVG charts.
import { symbol, base } from './logic.js';

export const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

// ---- local (non-synced) view preferences ----
export const viewPrefs = {
  get(k, d) { try { const v = localStorage.getItem('ff_view_' + k); return v == null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem('ff_view_' + k, JSON.stringify(v)); } catch { /* ignore */ } },
};

export function money(amount, currency = base(), { sign = false, hideable = true } = {}) {
  if (hideable && viewPrefs.get('hideMoney', false)) return `${symbol(currency)}••••`;
  const n = Number(amount) || 0;
  const s = Math.abs(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const pre = n < 0 ? '−' : sign && n > 0 ? '+' : '';
  return `${pre}${symbol(currency)}${s}`;
}
export const fmtDate = (ms, opts = { month: 'short', day: 'numeric', year: 'numeric' }) => (ms ? new Date(ms).toLocaleDateString(undefined, opts) : '—');
export const fmtDateTime = (ms) => (ms ? new Date(ms).toLocaleString(undefined, { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' }) : '—');
export const toInputDate = (ms) => { if (!ms) return ''; const d = new Date(ms); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };
export const toInputDateTime = (ms) => { if (!ms) return ''; const d = new Date(ms); return `${toInputDate(ms)}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`; };
export const fromInputDate = (s) => (s ? new Date(s.length <= 10 ? s + 'T00:00' : s).getTime() : null);

// ARGB signed int <-> CSS hex
export const argbToCss = (c) => '#' + ((Number(c) >>> 0) & 0xffffff).toString(16).padStart(6, '0');
export const cssToArgb = (hex) => (0xff000000 | parseInt(String(hex).replace('#', ''), 16)) | 0;

// ---- toast ----
export function toast(msg, kind = 'ok') {
  const el = document.createElement('div');
  el.className = `toast ${kind}`;
  el.textContent = msg;
  document.getElementById('toasts').appendChild(el);
  setTimeout(() => el.classList.add('out'), 2600);
  setTimeout(() => el.remove(), 3100);
}

// ---- modal ----
export function modal({ title, body, actions = [], wide = false, onOpen }) {
  return new Promise((resolve) => {
    const wrap = document.createElement('div');
    wrap.className = 'modal-wrap';
    wrap.innerHTML = `<div class="modal ${wide ? 'wide' : ''}" role="dialog" aria-modal="true" aria-label="${esc(title)}">
      <header><h2>${esc(title)}</h2><button class="icon-btn" data-close aria-label="Close">✕</button></header>
      <div class="modal-body">${body}</div>
      <footer>${actions.map((a, i) => `<button class="btn ${a.kind || ''}" data-act="${i}" type="${a.submit ? 'submit' : 'button'}">${esc(a.label)}</button>`).join('')}</footer>
    </div>`;
    const close = (v) => { wrap.remove(); document.removeEventListener('keydown', onKey); resolve(v); };
    const onKey = (e) => { if (e.key === 'Escape') close(null); };
    document.addEventListener('keydown', onKey);
    wrap.addEventListener('click', async (e) => {
      if (e.target === wrap || e.target.closest('[data-close]')) return close(null);
      const b = e.target.closest('[data-act]');
      if (!b) return;
      const a = actions[Number(b.dataset.act)];
      if (!a.onClick) return close(a.value ?? null);
      try {
        const r = await a.onClick(wrap);
        if (r !== false) close(r ?? a.value ?? true);
      } catch (err) { showFormError(wrap, err.message); }
    });
    // Enter inside a form = click the submit action (inline handlers are blocked by the CSP).
    wrap.querySelectorAll('form').forEach((f) => f.addEventListener('submit', (e) => {
      e.preventDefault();
      wrap.querySelector('footer button[type="submit"]')?.click();
    }));
    document.body.appendChild(wrap);
    onOpen?.(wrap);
    wrap.querySelector('input,select,textarea')?.focus();
  });
}
function showFormError(wrap, msg) {
  let el = wrap.querySelector('.form-error');
  if (!el) { el = document.createElement('p'); el.className = 'form-error'; wrap.querySelector('.modal-body').appendChild(el); }
  el.textContent = msg;
}
export const confirmDialog = (title, text, label = 'Delete', kind = 'danger') =>
  modal({ title, body: `<p class="muted">${esc(text)}</p>`, actions: [{ label: 'Cancel' }, { label, kind, value: true }] });

// ---- generic form ----
// field: { key, label, type: text|number|money|select|date|datetime|checkbox|textarea|color|password, options:[[v,l]], required, step, hint, half }
export function formHTML(fields, values = {}) {
  return `<form class="form" novalidate>${fields.map((f) => {
    if (f.type === 'section') return `<h3 class="form-section">${esc(f.label)}</h3>`;
    const v = values[f.key];
    const id = `f_${f.key}`;
    const req = f.required ? 'required' : '';
    let input;
    switch (f.type) {
      case 'select':
        input = `<select id="${id}" name="${f.key}" ${req}>${(f.options || []).map(([ov, ol]) =>
          `<option value="${esc(ov)}" ${String(ov) === String(v ?? '') ? 'selected' : ''}>${esc(ol)}</option>`).join('')}</select>`;
        break;
      case 'checkbox':
        return `<label class="check ${f.half ? 'half' : ''}"><input type="checkbox" name="${f.key}" ${v ? 'checked' : ''}> ${esc(f.label)}</label>`;
      case 'textarea':
        input = `<textarea id="${id}" name="${f.key}" rows="3" ${req}>${esc(v ?? '')}</textarea>`; break;
      case 'date':
        input = `<input id="${id}" type="date" name="${f.key}" value="${toInputDate(v)}" ${req}>`; break;
      case 'datetime':
        input = `<input id="${id}" type="datetime-local" name="${f.key}" value="${toInputDateTime(v)}" ${req}>`; break;
      case 'color':
        input = `<input id="${id}" type="color" name="${f.key}" value="${v != null ? argbToCss(v) : '#00897b'}">`; break;
      case 'number': case 'money':
        input = `<input id="${id}" type="number" inputmode="decimal" name="${f.key}" step="${f.step || 'any'}" value="${v ?? ''}" ${req}>`; break;
      default:
        input = `<input id="${id}" type="${f.type || 'text'}" name="${f.key}" value="${esc(v ?? '')}" ${req} ${f.autocomplete ? `autocomplete="${f.autocomplete}"` : ''}>`;
    }
    return `<label class="field ${f.half ? 'half' : ''}" for="${id}"><span>${esc(f.label)}${f.required ? ' *' : ''}</span>${input}${f.hint ? `<small>${esc(f.hint)}</small>` : ''}</label>`;
  }).join('')}</form>`;
}
export function readForm(wrap, fields) {
  const form = wrap.querySelector('form');
  const out = {};
  for (const f of fields) {
    if (f.type === 'section') continue;
    const el = form.elements[f.key];
    if (!el) continue;
    let v;
    if (f.type === 'checkbox') v = el.checked ? 1 : 0;
    else if (f.type === 'number' || f.type === 'money') v = el.value === '' ? null : Number(el.value);
    else if (f.type === 'date' || f.type === 'datetime') v = fromInputDate(el.value);
    else if (f.type === 'color') v = cssToArgb(el.value);
    else if (f.type === 'select') v = el.value === '' ? null : (f.numeric ? Number(el.value) : el.value);
    else v = el.value.trim() === '' ? null : el.value.trim();
    if (f.required && (v == null || v === '')) throw new Error(`${f.label} is required`);
    if ((f.type === 'money') && v != null && (!Number.isFinite(v) || v < 0)) throw new Error(`${f.label} must be a positive number`);
    out[f.key] = v;
  }
  return out;
}
export function formModal(title, fields, values = {}, { submitLabel = 'Save', onSubmit, extraActions = [], wide = false, onOpen } = {}) {
  return modal({
    title, wide, body: formHTML(fields, values), onOpen,
    actions: [...extraActions, { label: 'Cancel' }, { label: submitLabel, kind: 'primary', submit: true, onClick: (w) => onSubmit(readForm(w, fields), w) }],
  });
}

// ---- charts (SVG) ----
export function lineChart(points, { height = 160, color = 'var(--accent)', fmt = (v) => v } = {}) {
  if (points.length < 2) return '<div class="empty small">Not enough data</div>';
  const W = 600, H = height, pad = 8;
  const vals = points.map((p) => p.y), min = Math.min(...vals), max = Math.max(...vals), span = max - min || 1;
  const x = (i) => pad + (i / (points.length - 1)) * (W - pad * 2);
  const y = (v) => pad + (1 - (v - min) / span) * (H - pad * 2 - 14);
  const d = points.map((p, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(p.y).toFixed(1)}`).join(' ');
  const area = `${d} L${x(points.length - 1).toFixed(1)},${H - 14} L${x(0).toFixed(1)},${H - 14} Z`;
  const gid = 'g' + Math.random().toString(36).slice(2, 8);
  return `<svg class="chart" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" role="img">
    <defs><linearGradient id="${gid}" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="${color}" stop-opacity=".28"/><stop offset="1" stop-color="${color}" stop-opacity="0"/></linearGradient></defs>
    <path d="${area}" fill="url(#${gid})"/><path d="${d}" fill="none" stroke="${color}" stroke-width="2.5" vector-effect="non-scaling-stroke" stroke-linejoin="round"/>
    ${points.map((p, i) => `<rect x="${x(i) - (W / points.length) / 2}" y="0" width="${W / points.length}" height="${H}" fill="transparent"><title>${esc(p.label)}: ${esc(fmt(p.y))}</title></rect>`).join('')}
  </svg>
  <div class="chart-axis"><span>${esc(points[0].label)}</span><span>${esc(points[Math.floor(points.length / 2)].label)}</span><span>${esc(points[points.length - 1].label)}</span></div>`;
}
export function barPairs(rows, { fmt = (v) => v } = {}) {
  const max = Math.max(1, ...rows.flatMap((r) => [r.income, r.expense]));
  return `<div class="bars">${rows.map((r) => `<div class="bar-col" title="${esc(r.label)} — income ${esc(fmt(r.income))}, expense ${esc(fmt(r.expense))}">
    <div class="bar-pair"><i class="inc" style="height:${(r.income / max) * 100}%"></i><i class="exp" style="height:${(r.expense / max) * 100}%"></i></div>
    <span>${esc(r.label)}</span></div>`).join('')}</div>
    <div class="legend"><span><i class="inc"></i>Income</span><span><i class="exp"></i>Expense</span></div>`;
}
export function donut(items, { size = 150 } = {}) {
  const total = items.reduce((s, i) => s + i.value, 0);
  if (!total) return '<div class="empty small">No spending in this period</div>';
  const r = 52, c = 2 * Math.PI * r; let off = 0;
  const segs = items.map((i) => {
    const len = (i.value / total) * c;
    const s = `<circle r="${r}" cx="70" cy="70" fill="none" stroke="${i.color}" stroke-width="22" stroke-dasharray="${len} ${c - len}" stroke-dashoffset="${-off}"><title>${esc(i.label)}</title></circle>`;
    off += len; return s;
  }).join('');
  return `<svg width="${size}" height="${size}" viewBox="0 0 140 140" class="donut"><g transform="rotate(-90 70 70)">${segs}</g></svg>`;
}
export const progressBar = (ratio, warn = 0.8) =>
  `<div class="progress ${ratio >= 1 ? 'over' : ratio >= warn ? 'warn' : ''}"><i style="width:${Math.min(100, Math.max(0, ratio * 100))}%"></i></div>`;
