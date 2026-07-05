# LamaDB Android Client — Initial Spec Sheet

> Project: `lamadb-android`  
> Issue: LAMA-56  
> Status: Planning / spec complete  
> Target phase: **Phase 1 MVP**  
> Defer to Phase 2: push notifications, additional sensors, native UI replacement, Wear OS

---

## 1. Purpose

The Android client is Ali's primary, always-with-him frontend for LamaDB. It wraps the web dashboard in a WebView and turns the phone into a lightweight Life-OS sensor source, starting with WiFi-based presence detection.

---

## 2. Phase 1 Scope (MVP)

| Feature | What it does | Why in Phase 1 |
|---|---|---|
| **WebView dashboard** | Loads the LamaDB web dashboard (Tailnet URL) | Immediate value; existing UI |
| **WiFi presence sensor** | Detects home/away transitions by WiFi SSID / connection state | Core automation trigger |
| **Offline SQLite queue** | Buffers sensor events when offline; drains when connected | Reliable event delivery despite Samsung kills |
| **Keystore auth** | Stores LamaDB API key in Android Keystore + EncryptedSharedPreferences | Secure, no plain-text keys |
| **QR login** | User scans QR from web dashboard to provision API key | Fastest UX on phone |

---

## 3. Out of Scope for Phase 1

| Feature | Defer reason |
|---|---|
| Push notifications from LamaDB | Needs backend gateway (FCM / ntfy) and routing rules |
| Additional sensors (location, battery, activity, charging) | Battery/permission complexity; WiFi is the 80% win |
| Native Android UI replacing WebView | Web dashboard is good enough for MVP |
| Wear OS companion | Requires Health Connect / Samsung Health research |
| Biometric app lock | Nice-to-have; can be added later |

---

## 4. Architecture

```
┌─────────────────────────────────────────┐
│           Android App (Kotlin)          │
│  ┌──────────────┐  ┌─────────────────┐  │
│  │   WebView    │  │  SensorManager  │  │
│  │  (dashboard) │  │  (WiFi + future)│  │
│  └──────┬───────┘  └────────┬────────┘  │
│         │                   │           │
│  ┌──────┴───────┐  ┌────────┴────────┐  │
│  │ JS Bridge    │  │ Event SQLite    │  │
│  │ (web ↔ app)  │  │ Queue + Worker  │  │
│  └──────┬───────┘  └────────┬────────┘  │
│         │                   │           │
│  ┌──────┴───────────────────┴────────┐  │
│  │        Keystore (API key)          │  │
│  └────────────────────────────────────┘  │
└─────────────────────┬───────────────────┘
                      │ HTTPS / Tailnet
┌─────────────────────┴───────────────────┐
│           LamaDB FastAPI Backend          │
│        POST /api/events (existing)        │
└───────────────────────────────────────────┘
```

---

## 5. Tech Stack

| Layer | Choice | Rationale |
|---|---|---|
| Language | Kotlin | Modern Android standard |
| UI | WebView + Compose for native screens | Dashboard reuse; native screens for login/settings |
| Minimum SDK | Android 12 (API 31) | Ali's primary device; `HIGH_SAMPLING_RATE_SENSORS` if needed later |
| Target SDK | Android 15 (API 35) | Latest stable at time of writing |
| Storage | Room or raw SQLite | Room is simpler; raw SQLite if minimizing deps |
| Crypto | Android Keystore + `EncryptedSharedPreferences` | Standard secure storage |
| Background | Foreground Service + WorkManager | Foreground service for presence; WorkManager for periodic drain |
| HTTP | Ktor client or OkHttp + Kotlinx Serialization | Ktor fits Kotlin coroutines well |
| DI | None for Phase 1 | Keep scaffold small |

---

## 6. Authentication Flow

### QR-code login (primary)

1. Ali is logged into LamaDB web dashboard on desktop.
2. Dashboard shows a QR code containing a short-lived setup token + Tailnet URL.
3. Android app scans QR, exchanges token for API key via `POST /api/android/provision` (new backend endpoint).
4. Backend invalidates the token after one use or expiry (e.g., 5 minutes).
5. App encrypts API key with Keystore and stores in `EncryptedSharedPreferences`.
6. App hits `/api/me` to verify key.

### Fallback: manual API key entry

1. User pastes or types a LamaDB API key.
2. App validates via `/api/me`.
3. Rejected keys are not persisted.

### Security requirements

- API key is never stored in plain text.
- Key is loaded into memory only when needed.
- QR token is single-use, short-lived, scoped only to provisioning.

---

## 7. WiFi Presence Sensor

### Behavior

- Detect when the device connects to / disconnects from the home WiFi SSID.
- Emit a `presence` event to LamaDB with state `home` or `away`.
- Debounce transitions (~30 seconds) to avoid flapping.
- Include metadata: `device_id`, `sensor`, `ssid`, `previous_state`, `confidence`.

### Permissions

- `ACCESS_FINE_LOCATION` — required for WiFi SSID on Android 12+.
- `ACCESS_WIFI_STATE` — read WiFi connection info.
- `POST_NOTIFICATIONS` — required for foreground service notification.
- `FOREGROUND_SERVICE` — keep presence service alive.

### Implementation notes

- Use `ConnectivityManager.NetworkCallback` for connection changes.
- Use `WifiManager.connectionInfo` (or `NetworkCapabilities` on newer Android) to read SSID.
- Run as a foreground service with a persistent notification.
- On Samsung, guide user to add app to "Never sleeping apps".

### Event schema

```json
{
  "source": "android_life_os",
  "type": "presence",
  "severity": "info",
  "title": "Ali arrived home",
  "body": "Connected to WiFi 'Valhalla-5G'",
  "metadata": {
    "device_id": "samsung-s24-abc123",
    "sensor": "wifi",
    "state": "home",
    "ssid": "Valhalla-5G",
    "previous_state": "away",
    "confidence": 0.95
  }
}
```

---

## 8. Offline SQLite Queue

### Purpose

Samsung Device Care will kill the app. Sensor events must survive app restarts.

### Schema

```kotlin
data class QueuedEvent(
    val id: Long = 0,
    val source: String,
    val type: String,
    val severity: String,
    val title: String,
    val body: String?,
    val metadata: String,         // JSON string
    val createdAt: Long,
    val retryCount: Int = 0
)
```

### Behavior

- Sensor writes to SQLite immediately.
- WorkManager periodically drains the queue to `POST /api/events`.
- On app open, flush queue immediately.
- Exponential backoff on failures; max retry count (e.g., 5).
- Drop old events if queue exceeds max age (e.g., 7 days) or max size (e.g., 1000).
- Compute a stable `dedup_key` where possible to avoid duplicates on retry.

---

## 9. WebView ↔ App Bridge

### Native → Web

- `setAuthToken(token)` — inject API key after page load (avoid manual login).
- `setTheme(darkMode)` — pass system theme to dashboard.
- `setPresence(state)` — reflect current presence state in dashboard UI.

### Web → Native

- `openScanner()` — web dashboard requests native QR scanner for API key provision.
- `requestPresence()` — web asks app for current presence state.
- `shareEvent(json)` — future: web can queue an event to be posted via app.

### Implementation

- Use `@JavascriptInterface` on a Kotlin class.
- Validate all JS payloads; never execute arbitrary JS from the dashboard.

---

## 10. Backend Work Required

| Endpoint | Purpose | New? |
|---|---|---|
| `POST /api/events` | Submit sensor events | Existing |
| `GET /api/me` | Validate API key | Existing |
| `POST /api/android/provision` | Exchange QR token for API key | **New** |
| `GET /api/dashboard/overview` | Loaded by WebView | Existing |

### New endpoint: `POST /api/android/provision`

**Request:**

```json
{
  "token": "qr-setup-token"
}
```

**Response:**

```json
{
  "api_key": "lamadb_...",
  "user_id": "...",
  "expires_at": "2026-07-06T12:00:00Z"
}
```

### Backend considerations

- Token generation endpoint on dashboard (e.g., `POST /api/users/qr-token`).
- Token is single-use, short-lived, tied to authenticated user.
- API key returned should have appropriate role (agent/read/write) and module scopes.

---

## 11. UI/UX Requirements

### Screens

1. **Splash / login screen** — QR scan button + manual key entry.
2. **QR scanner** — CameraX-based QR scanner.
3. **Dashboard WebView** — Fullscreen WebView, pull-to-refresh optional.
4. **Settings** — URL, API key reset, sensor toggle, notification channel setup (Phase 2).
5. **Foreground service notification** — Persistent notification showing home/away state and last sync.

### WebView behavior

- Load `https://lamadb.tailnet/...` (Tailnet-only).
- Handle SSL errors gracefully (Tailnet uses internal certs).
- Support dark mode via `setForceDark` or CSS.
- Cache aggressively to reduce load time.
- Optional `?mobile=1` query param to hide desktop chrome if needed.

---

## 12. Future-Proofing Additions (Phase 2+)

| Feature | Notes |
|---|---|
| **Push notifications** | Server sends `notification-worthy` events (e.g., Tautulli issues, Plex failures) via FCM or self-hosted ntfy. |
| **Charging sensor** | Event on plug/unplug; useful for sleep/shower heuristics. |
| **Battery sensor** | Low-battery automations, health tracking. |
| **Activity recognition** | Walking/driving/idle for smarter presence. |
| **Location geofencing** | Backup presence when WiFi unavailable. |
| **Wear OS companion** | Heart rate, sleep, steps, SpO2 via Health Connect. |
| **Native widgets** | Home screen widget for status / quick actions. |
| **Biometric lock** | Require fingerprint/face before app opens. |

---

## 13. Development Approach

1. **Scaffold** — Kotlin project, single activity, WebView, dependencies.
2. **Auth** — Keystore + QR scanner + `/api/android/provision`.
3. **WebView bridge** — inject token, handle basic JS calls.
4. **WiFi sensor** — permissions, foreground service, debounce.
5. **Offline queue** — SQLite schema, WorkManager drain.
6. **Integration test** — Install APK, verify presence events land in LamaDB events table.
7. **Document** — Update README, wiki, and LAMA-56.

---

## 14. Decisions Made

| # | Question | Decision |
|---|---|---|
| 1 | Use Room or raw SQLite for queue? | Room |
| 2 | Ktor or OkHttp for HTTP? | Ktor client |
| 3 | Pull-to-refresh in WebView? | Include in Phase 1 |
| 4 | Should the dashboard generate the QR token? | Yes, via new backend endpoint |
| 5 | Should the app support multiple LamaDB URLs? | No, single URL per install |
| 6 | Target minimum SDK? | API 31 (Android 12) |
| 7 | Push notifications? | Defer to Phase 2 |
| 8 | Extra sensors? | Defer to Phase 2 |

---

## 15. Links

- Project repo: `~/LamaFiles/projects/lamadb-android/`
- Backend repo: `~/LamaFiles/projects/lamadb/`
- Wiki: `~/Basecamp/wiki/projects/lamadb/android-life-os.md`
- Kanban: LAMA-56
