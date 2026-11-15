# Server sync & web app

My Finance can sync between the Android app, the web app and any number of devices through a small
self-hosted server (`server/`). The web app (`web/`) is served by the same server.

## Pieces

| Path | What it is |
|---|---|
| `server/server.js` | Sync server + static web host. Node ≥ 22.13, **no npm dependencies** (uses `node:sqlite`). |
| `web/` | Web app (plain ES modules, no build step). Same features as the phone, minus Android-only ones. |
| `app/.../data/sync/ServerSyncManager.kt` | Android sync engine. |
| `app/.../ui/components/ServerSyncCard.kt` | Settings → *Data Vault, Sync & Integrity* → **Server Sync** card. |

## Running the server

```bash
FF_PORT=5006 FF_HOST=127.0.0.1 FF_DATA_DIR=/path/to/data node server/server.js
```

On first start it prints a **one-time setup code** (also written to `$FF_DATA_DIR/SETUP_CODE.txt`).
Open the web app, enter the code, and create the account. Then sign in from the phone
(Settings → Server Sync) with the same username and password.

Put it behind HTTPS (Cloudflare Tunnel, Tailscale serve, nginx…): Android blocks plain-HTTP traffic.

## How sync works

* **Change tracking (Android):** SQLite triggers on every synced table write changed row ids into
  `ff_sync_log`. Triggers are muted while remote changes are applied, so nothing echoes back.
  No Room entities, DAOs or migrations were changed.
* **IDs:** every row has a global id `deviceNo × 2^40 + localId`. Rows created on a device keep normal
  positive autoincrement ids; rows received from other devices are stored locally with **negative** ids
  (`-globalId`), so SQLite AUTOINCREMENT never wanders into another device's range.
* **Conflicts:** last write wins, by the timestamp of the edit.
* **Balances:** an account balance is always `initial_balance + cleared transactions`, recomputed after
  every pull, so concurrent offline edits on several devices can't corrupt balances.
* **Integrity:** if a synced row references a record that no longer exists, nullable references are
  cleared and rows with a missing required parent are dropped instead of failing the whole sync.
* **First sign-in on a phone:** if the server is empty the phone uploads its data; if the phone is empty
  it downloads; if both have data the user is asked to replace the phone's data with the server's.
* **Synced settings:** base currency, theme palette, light/dark mode and hero style, in the `prefs` record
  (`base_currency`, `app_theme_preset`, `app_theme_mode`, `hero_background_style`, same values as the Android
  DataStore). The web shows Emerald Aurora for Android-only *Material You*. Privacy mask and app lock stay per device.
* **Photos:** receipt/proof photos are uploaded content-addressed (`image_sha256` / `proof_sha256`) and downloaded
  by the other devices.
* **Auto sync (Android):** a few seconds after any data or settings change, every 45 s while the app is open, and
  about every 15 minutes in the background (`ServerSyncJobService`, platform JobScheduler, needs a network).
  The web app syncs after each change, every 20 s and when the tab regains focus.

## JSON backup (export / import)

Both apps export one JSON file with everything: all tables (including assets, planned payouts and the receipt
inbox), `settings` (currency, theme, dashboard layout; never the app-lock PIN) and `images`
(`{ "<sha256>": "<base64>" }`, referenced by `proofSha256` / `imageSha256`). Older v1 files still import.
Each app imports the other's export: the phone maps the web's global ids the same way sync does.

## Building the APK

No extra dependencies were added (OkHttp, coroutines and Room were already in the project).
The manifest declares `android.permission.INTERNET` and `RECEIVE_BOOT_COMPLETED` (keeps the background sync job
scheduled after a reboot).

```bash
./gradlew assembleDebug
```
