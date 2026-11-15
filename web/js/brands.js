// Brand-styled looks for Philippine banks / e-wallets (mirrors app/.../ui/brand/Brands.kt).
// Drawn styles in each issuer's colours with its name as a wordmark — not official logo artwork.
import { esc } from './ui.js?v=12';
import { store } from './store.js?v=12';

/** sha256 of the real app icon the phone uploaded for this brand (see ServerSyncManager.refreshBankLogos). */
export function logoSha(b) {
  if (!b) return null;
  try { const m = JSON.parse(store.prefs?.bank_logos || '{}'); return /^[a-f0-9]{64}$/.test(m[b.id] || '') ? m[b.id] : null; } catch { return null; }
}

const B = (id, name, wordmark, mono, colors, accent, keywords, extra = {}) => ({ id, name, wordmark, mono, colors, accent, keywords, ink: '#FFFFFF', ...extra });
export const BRANDS = [
  B('gcash', 'GCash', 'GCash', 'G', ['#2D7BFF', '#0057E7', '#003BB0'], '#9CC4FF', ['gcash', 'gsave', 'gcredit']),
  B('maya', 'Maya', 'maya', 'm', ['#151816', '#0E1A13', '#06331D'], '#00D26A', ['maya', 'paymaya'], { ink: '#00D26A' }),
  B('maribank', 'MariBank', 'MariBank', 'M', ['#FF8A1E', '#F26A0A', '#D9480F'], '#FFE0BF', ['maribank', 'mari bank']),
  B('seabank', 'SeaBank', 'SeaBank', 'S', ['#FF6B3D', '#E8491D', '#B8340F'], '#FFD4C2', ['seabank', 'sea bank']),
  B('gotyme', 'GoTyme', 'GoTyme', 'GT', ['#16C7C1', '#0B8C9A', '#0A3946'], '#B8FFF6', ['gotyme', 'go tyme']),
  B('bdo', 'BDO', 'BDO', 'B', ['#1046B8', '#0A3592', '#06225F'], '#FFC72C', ['bdo', 'banco de oro']),
  B('bpi', 'BPI', 'BPI', 'B', ['#C8161D', '#9E1016', '#66090D'], '#FFD6D7', ['bpi']),
  B('unionbank', 'UnionBank', 'UnionBank', 'U', ['#FF8A00', '#E56E00', '#3B3B3F'], '#FFE2BD', ['unionbank', 'union bank']),
  B('metrobank', 'Metrobank', 'Metrobank', 'M', ['#2361C7', '#16479A', '#0B2A5E'], '#CFE0FF', ['metrobank', 'metro bank']),
  B('securitybank', 'Security Bank', 'Security Bank', 'SB', ['#14AEDF', '#0A7BC2', '#07457A'], '#D3F3FF', ['security bank', 'securitybank']),
  B('rcbc', 'RCBC', 'RCBC', 'R', ['#1B4DA3', '#123677', '#0A1F47'], '#FF4A52', ['rcbc', 'diskartech']),
  B('landbank', 'LANDBANK', 'LANDBANK', 'L', ['#10A04F', '#00783A', '#004C24'], '#FFD84D', ['landbank', 'land bank']),
  B('pnb', 'PNB', 'PNB', 'P', ['#26357F', '#1A2560', '#10173D'], '#FF4D5A', ['pnb']),
  B('chinabank', 'China Bank', 'China Bank', 'C', ['#D0122F', '#9A0C22', '#5E0715'], '#F6D36B', ['china bank', 'chinabank']),
  B('eastwest', 'EastWest', 'EastWest', 'E', ['#6A35A0', '#4B2378', '#2C1349'], '#FFB42E', ['eastwest', 'east west', 'komo']),
  B('tonik', 'Tonik', 'tonik', 't', ['#8A3BFF', '#5B1FD6', '#2E0E85'], '#FF5DB1', ['tonik']),
  B('cimb', 'CIMB', 'CIMB', 'C', ['#EE2A30', '#B8161C', '#6E0B0F'], '#FFD0D1', ['cimb']),
  B('shopeepay', 'ShopeePay', 'ShopeePay', 'S', ['#F5593A', '#E23F1F', '#B02A12'], '#FFD9CF', ['shopeepay', 'shoppepay', 'shopee', 'spaylater']),
  B('grabpay', 'GrabPay', 'GrabPay', 'G', ['#14C35E', '#00A04A', '#006B31'], '#D5FFE4', ['grabpay', 'grab']),
  B('wise', 'Wise', 'Wise', 'W', ['#A6EC79', '#8AD65C'], '#163300', ['wise', 'transferwise'], { ink: '#163300' }),
  B('revolut', 'Revolut', 'Revolut', 'R', ['#2B2F36', '#191C1F', '#0B0C0E'], '#DDE3EA', ['revolut']),
  B('paypal', 'PayPal', 'PayPal', 'P', ['#0B3B91', '#062A6B', '#031A45'], '#29B6F6', ['paypal']),
];

export function detectBrand(...names) {
  for (const raw of names) {
    if (!raw) continue;
    const lower = String(raw).toLowerCase();
    const squashed = lower.replace(/[^a-z0-9]/g, '');
    const tokens = new Set(lower.split(/[^a-z0-9]+/).filter(Boolean));
    for (const b of BRANDS) {
      if (b.keywords.some((kw) => { const k = kw.replace(/[^a-z0-9]/g, ''); return k.length <= 4 ? tokens.has(k) : squashed.includes(k); })) return b;
    }
  }
  return null;
}

/** Small rounded-square mark (gradient + monogram) for account rows. */
export const brandMark = (b, size = 36) => {
  const sha = logoSha(b);
  const drawn = `<span class="brand-mark" style="width:${size}px;height:${size}px;background:linear-gradient(135deg,${b.colors.join(',')});color:${b.ink};font-size:${Math.round(size * (b.mono.length > 1 ? 0.36 : 0.48))}px">${esc(b.mono)}</span>`;
  return sha ? `<img class="brand-logo" data-sha="${sha}" data-fallback="${esc(drawn)}" alt="${esc(b.name)}" width="${size}" height="${size}">` : drawn;
};

/** Card network symbol as inline SVG / text. */
export function networkMark(net) {
  switch (String(net || '').toUpperCase()) {
    case 'MASTERCARD':
      return '<svg class="net-mark" viewBox="0 0 42 26" aria-label="Mastercard"><circle cx="13" cy="13" r="13" fill="#EB001B"/><circle cx="29" cy="13" r="13" fill="#F79E1B"/><path d="M21 3.1a13 13 0 0 1 0 19.8a13 13 0 0 1 0-19.8z" fill="#FF5F00"/></svg>';
    case 'VISA': return '<span class="net-mark visa">VISA</span>';
    case 'AMEX': return '<span class="net-mark amex">AMEX</span>';
    default: return `<span class="net-mark">${esc(net || '')}</span>`;
  }
}
