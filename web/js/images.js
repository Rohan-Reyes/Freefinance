// Photos for receipts / transaction proofs, stored content-addressed (SHA-256) on the sync server.
// <img> can't send an Authorization header, so photos are fetched with the token and shown as blob: URLs.
import { store } from './store.js';

const cache = new Map(); // sha -> Promise<blobURL|null>
export const isSha = (s) => typeof s === 'string' && /^[a-f0-9]{64}$/.test(s);

export function photoUrl(sha) {
  if (!isSha(sha)) return Promise.resolve(null);
  if (!cache.has(sha)) {
    cache.set(sha, fetch(`/api/files/${sha}`, { headers: { Authorization: `Bearer ${store.auth?.token}` } })
      .then((r) => (r.ok ? r.blob() : null)).then((b) => (b ? URL.createObjectURL(b) : null)).catch(() => null));
  }
  return cache.get(sha);
}

/** Fills every <img data-sha> inside root. */
export function hydratePhotos(root) {
  root.querySelectorAll('img[data-sha]:not([src])').forEach(async (img) => {
    const url = await photoUrl(img.dataset.sha);
    if (url) img.src = url;
    else if (img.dataset.fallback) img.outerHTML = img.dataset.fallback;
    else img.replaceWith(Object.assign(document.createElement('span'), { className: 'photo-missing', textContent: 'Photo unavailable' }));
  });
}

async function sha256Hex(buf) {
  const d = await crypto.subtle.digest('SHA-256', buf);
  return [...new Uint8Array(d)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

/** Downscale to max 1600px JPEG (keeps uploads small); falls back to the original file. */
async function compress(file) {
  try {
    const bmp = await createImageBitmap(file);
    const scale = Math.min(1, 1600 / Math.max(bmp.width, bmp.height));
    const canvas = Object.assign(document.createElement('canvas'), { width: Math.round(bmp.width * scale), height: Math.round(bmp.height * scale) });
    canvas.getContext('2d').drawImage(bmp, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise((r) => canvas.toBlob(r, 'image/jpeg', 0.85));
    return blob && blob.size < file.size ? blob : file;
  } catch { return file; }
}

/** Uploads an image File; returns its sha256. */
export async function uploadPhoto(file) {
  if (!file.type.startsWith('image/')) throw new Error('Please choose an image file');
  const blob = await compress(file);
  const buf = await blob.arrayBuffer();
  const sha = await sha256Hex(buf);
  const auth = { Authorization: `Bearer ${store.auth.token}` };
  // The server de-duplicates by hash, so a plain PUT is idempotent.
  const r = await fetch(`/api/files/${sha}`, { method: 'PUT', headers: { ...auth, 'Content-Type': 'application/octet-stream' }, body: buf });
  if (!r.ok) throw new Error((await r.json().catch(() => ({}))).error || `Upload failed (${r.status})`);
  cache.set(sha, Promise.resolve(URL.createObjectURL(blob)));
  return sha;
}

export function viewPhoto(sha, title = 'Photo') {
  photoUrl(sha).then((url) => {
    if (!url) return;
    const w = document.createElement('div');
    w.className = 'modal-wrap photo-view';
    w.innerHTML = `<figure><img src="${url}" alt=""><figcaption></figcaption></figure>`;
    w.querySelector('figcaption').textContent = title;
    w.addEventListener('click', () => w.remove());
    document.body.appendChild(w);
  });
}

// ---- full JSON backups carry the photos themselves: { "<sha256>": "<base64>" } ----
function toBase64(buf) {
  const bytes = new Uint8Array(buf); let bin = '';
  for (let i = 0; i < bytes.length; i += 0x8000) bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
  return btoa(bin);
}
function fromBase64(b64) {
  const bin = atob(b64), out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}
/** Downloads every photo referenced by the given rows. Photos the server doesn't have are skipped. */
export async function exportPhotos(shas) {
  const out = {};
  const auth = { Authorization: `Bearer ${store.auth.token}` };
  for (const sha of new Set([...shas].filter(isSha))) {
    try {
      const r = await fetch(`/api/files/${sha}`, { headers: auth });
      if (r.ok) out[sha] = toBase64(await r.arrayBuffer());
    } catch { /* offline or missing: the record is still exported */ }
  }
  return out;
}
/** Uploads photos from a backup (verifying each hash). Returns how many are now on the server. */
export async function importPhotos(images) {
  let n = 0;
  const auth = { Authorization: `Bearer ${store.auth.token}` };
  for (const [sha, b64] of Object.entries(images || {})) {
    if (!isSha(sha) || typeof b64 !== 'string') continue;
    try {
      const bytes = fromBase64(b64);
      if ((await sha256Hex(bytes)) !== sha) continue; // corrupted entry
      const r = await fetch(`/api/files/${sha}`, { method: 'PUT', headers: { ...auth, 'Content-Type': 'application/octet-stream' }, body: bytes });
      if (r.ok) n++;
    } catch { /* keep importing the rest */ }
  }
  return n;
}
