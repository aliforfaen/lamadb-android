# LamaDB Android — Development Board Backlog

> This is the initial backlog for the `lamadb-android` project in Multica. Items are ordered by dependency: environment first, then scaffolding, then features.

---

## Issue 1: Set up working Android development environment on desktop `cachy`

**Identifier:** `LAMA-57` (or next available)  
**Status:** Backlog  
**Priority:** P0 / Critical  
**Assignee:** *Ali (human) + Android Tester*  
**Labels:** `setup`, `environment`, `cachy`, `android`

### Description

The Android Developer and Android Tester agents will run on the local desktop `cachy` (via Pi). This issue tracks installing and verifying the minimal Android toolchain needed for command-line builds and emulator tests.

### Acceptance criteria

- [ ] OpenJDK 17 installed (`java -version` reports 17 or 21).
- [ ] Android SDK command-line tools installed at `/opt/android-sdk`.
- [ ] `platform-tools`, `build-tools;35.0.0`, `platforms;android-35`, `platforms;android-31` installed.
- [ ] `adb` and `sdkmanager` are in `$PATH` and work.
- [ ] KVM available (`/dev/kvm` exists).
- [ ] Android Emulator + x86_64 system image installed.
- [ ] AVD `lamadb-test` created successfully.
- [ ] Emulator starts headless: `emulator -avd lamadb-test -no-window -no-audio`.
- [ ] `git`, `curl`, `unzip`, `tailscale` installed and working.
- [ ] Pi / Hermes runtime is installed and can run the Android agents.
- [ ] Environment variables added to shell profile (`JAVA_HOME`, `ANDROID_HOME`, `PATH`).
- [ ] The Android Tester agent can run `./gradlew build` successfully on a sample project.

### Notes

- Ali will join the chat for this issue to walk through the setup and learn the toolchain.
- This is intentionally a pairing issue, not a heavy coding task.
- CachyOS is Arch-based, so packages are installed with `pacman`.

### Related files

- `docs/desktop-setup.md`
- `scripts/setup-dev-env.sh`
- `docs/development.md`

---

## Issue 2: Create minimal Android project skeleton

**Identifier:** `LAMA-58` (or next available)  
**Status:** Done  
**Priority:** P0  
**Assignee:** Android Developer  
**Labels:** `scaffold`, `android`, `gradle`, `kotlin`

### Description

Create a working Android project that compiles from the command line and can be installed on the emulator.

### Acceptance criteria

- [x] Project structure follows modern Android conventions (`app/src/main/...`).
- [x] `build.gradle.kts` (project and app) configured for Kotlin and Compose.
- [x] `minSdk = 31`, `targetSdk = 35`, `compileSdk = 35`.
- [x] Gradle wrapper committed.
- [x] `./gradlew build` passes.
- [x] `./gradlew assembleDebug` produces an APK.
- [x] `./gradlew installDebug` installs on the emulator.
- [x] `MainActivity` launches and shows a placeholder screen.
- [x] `.gitignore` excludes `local.properties`, build dirs, `.idea`, etc.
- [x] Basic README in `app/` explains how to build.

### Notes

- No Android Studio; everything must work from CLI.
- Keep dependencies minimal in Phase 1.

---

## Issue 3: Implement WebView dashboard with pull-to-refresh

**Identifier:** `LAMA-59` (or next available)  
**Status:** Backlog  
**Priority:** P0  
**Assignee:** Android Developer  
**Labels:** `webview`, `ui`, `android`

### Description

Add a full-screen WebView that loads the LamaDB dashboard. Include pull-to-refresh to reload the page.

### Acceptance criteria

- [ ] WebView occupies the full screen.
- [ ] Loads a configurable Tailnet URL from settings.
- [ ] Supports JavaScript.
- [ ] Pull-to-refresh gesture reloads the page.
- [ ] Handles SSL errors gracefully for internal/Tailnet certs.
- [ ] Dark mode is passed to the web dashboard via CSS/media query or JS bridge.
- [ ] Unit test for URL parsing and refresh action.

### Notes

- Use `androidx.webkit.WebView` features where helpful.
- URL should be editable in settings (Phase 1: manual entry; QR comes later).

---

## Issue 4: Implement QR-code login and Keystore API key storage

**Identifier:** `LAMA-60` (or next available)  
**Status:** Backlog  
**Priority:** P0  
**Assignee:** Android Developer  
**Labels:** `auth`, `keystore`, `qr`, `android`

### Description

Allow the user to log in by scanning a QR code from the LamaDB dashboard. Store the API key securely in Android Keystore + EncryptedSharedPreferences.

### Acceptance criteria

- [ ] CameraX-based QR scanner screen.
- [ ] QR code contains a short-lived setup token.
- [ ] App calls `POST /api/android/provision` to exchange token for API key.
- [ ] API key stored in `EncryptedSharedPreferences`.
- [ ] Keystore-backed encryption key for the SharedPreferences.
- [ ] App validates the key with `GET /api/me`.
- [ ] Fallback: manual API key entry screen.
- [ ] API key is never logged or shown in UI.
- [ ] Unit tests for token exchange and storage logic (with mocked backend).

### Notes

- Requires new backend endpoint `POST /api/android/provision` (see Issue 9).
- QR token is single-use and short-lived (5 minutes).

---

## Issue 5: Implement WiFi presence sensor

**Identifier:** `LAMA-61` (or next available)  
**Status:** Backlog  
**Priority:** P0  
**Assignee:** Android Developer  
**Labels:** `sensor`, `wifi`, `presence`, `android`

### Description

Detect home/away transitions based on the connected WiFi SSID and emit events to LamaDB.

### Acceptance criteria

- [ ] Foreground service keeps presence detection alive.
- [ ] Detects WiFi connect/disconnect events via `ConnectivityManager.NetworkCallback`.
- [ ] Reads SSID with appropriate permissions (`ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`).
- [ ] Debounces transitions by 30 seconds to avoid flapping.
- [ ] Emits `presence` event with `state: home` or `state: away`.
- [ ] Event includes `device_id`, `sensor`, `ssid`, `previous_state`, `confidence`.
- [ ] Persistent notification shows current presence state.
- [ ] Unit tests for state machine and debounce logic.

### Notes

- Samsung battery optimizations will kill the service; the foreground notification and `ignoreBatteryOptimizations` request are necessary.
- WiFi permission must be requested at runtime.

---

## Issue 6: Implement offline SQLite event queue

**Identifier:** `LAMA-62` (or next available)  
**Status:** Backlog  
**Priority:** P0  
**Assignee:** Android Developer  
**Labels:** `queue`, `sqlite`, `room`, `offline`, `android`

### Description

Buffer sensor events in a local SQLite queue when offline and drain them to LamaDB when connected.

### Acceptance criteria

- [ ] Room database with `QueuedEvent` entity.
- [ ] Sensor writes events to queue immediately.
- [ ] WorkManager periodically drains queue to `POST /api/events`.
- [ ] Queue drains immediately when app opens.
- [ ] Exponential backoff on failures; max 5 retries.
- [ ] Old events dropped after 7 days or 1000 queued items.
- [ ] Deduplication key computed where possible.
- [ ] Unit tests for queue insert, drain, retry, and drop logic.

### Notes

- Schema is documented in `docs/spec.md` section 8.
- Queue must survive app kills and reboots.

---

## Issue 7: Implement WebView ↔ native app bridge

**Identifier:** `LAMA-63` (or next available)  
**Status:** Backlog  
**Priority:** P1  
**Assignee:** Android Developer  
**Labels:** `bridge`, `webview`, `javascript`, `android`

### Description

Allow the web dashboard and the native app to communicate: pass auth token, theme, and presence state to the web; request QR scan and presence state from the web.

### Acceptance criteria

- [ ] `@JavascriptInterface` class exposes native methods to web.
- [ ] Web can call `openScanner()` to start native QR scan.
- [ ] Web can call `requestPresence()` to get current state.
- [ ] Native app injects `setAuthToken(token)` after page load.
- [ ] Native app calls `setPresence(state)` when state changes.
- [ ] Native app calls `setTheme(darkMode)` on theme change.
- [ ] Validate all JS payloads; reject malformed calls.
- [ ] Unit tests for bridge message parsing.

### Notes

- Keep the bridge surface small and explicit.
- Future: `shareEvent(json)` from web to native queue.

---

## Issue 8: Set up Android Tester verification pipeline

**Identifier:** `LAMA-64` (or next available)  
**Status:** Backlog  
**Priority:** P1  
**Assignee:** Android Tester  
**Labels:** `testing`, `ci`, `emulator`, `android`

### Description

Establish a repeatable verification loop: build, install, run tests, capture logs, report results.

### Acceptance criteria

- [ ] Documented command sequence for build + test + install.
- [ ] `./gradlew build` passes before any install.
- [ ] `./gradlew test` passes.
- [ ] `./gradlew connectedCheck` runs on the emulator.
- [ ] `adb logcat -s LamaDB:D` capture script.
- [ ] Screenshot capture script using `adb shell screencap`.
- [ ] Test results posted back to the assigned issue.
- [ ] Failure reports include exact command, output, and device/emulator state.

### Notes

- This is the Tester's onboarding issue; it verifies the entire toolchain end-to-end.
- The pipeline will be reused for every subsequent feature issue.

---

## Issue 9: Add backend QR provisioning endpoint

**Identifier:** `LAMA-65` (or next available)  
**Status:** Backlog  
**Priority:** P0  
**Assignee:** Android Developer (with backend review)  
**Labels:** `backend`, `api`, `auth`, `qr`, `android`

### Description

Create the backend support needed for the Android QR login flow.

### Acceptance criteria

- [ ] New endpoint `POST /api/users/qr-token` generates a short-lived, single-use token.
- [ ] New endpoint `POST /api/android/provision` exchanges the token for an API key.
- [ ] Token is bound to the authenticated user and expires after 5 minutes.
- [ ] Token is invalidated after one use or on expiry.
- [ ] API key returned has appropriate scopes for the Android client.
- [ ] Endpoint accessible only over Tailnet.
- [ ] Tests for token generation, exchange, expiry, and reuse.

### Notes

- This is the only backend work required for Phase 1.
- The dashboard UI to show the QR code can be minimal in Phase 1.

---

## Issue 10: First end-to-end smoke test

**Identifier:** `LAMA-66` (or next available)  
**Status:** Backlog  
**Priority:** P1  
**Assignee:** Android Tester  
**Labels:** `testing`, `e2e`, `smoke`, `android`

### Description

Run a full smoke test: install the app, log in, verify the dashboard loads, trigger WiFi presence events, and confirm they land in LamaDB.

### Acceptance criteria

- [ ] Fresh APK install on emulator.
- [ ] Log in via manual API key (QR UI may not be ready yet).
- [ ] Dashboard loads and shows LamaDB UI.
- [ ] Toggle WiFi on/off in emulator; presence events emitted.
- [ ] Events visible in LamaDB `/api/events` or dashboard event feed.
- [ ] No crashes in `adb logcat`.
- [ ] Screenshot of dashboard and settings attached to issue.

### Notes

- This confirms Phase 1 is functional before moving to polish or Phase 2.
- May need to simulate WiFi SSID changes via `adb shell svc wifi` or emulator extended controls.

---

## Phase 2 — Deferred

These are intentionally out of scope for Phase 1 but listed for visibility:

- Push notifications for notification-worthy events (Tautulli/Plex failures, etc.).
- Additional sensors: charging, battery, activity recognition, location geofencing.
- Wear OS companion with Health Connect.
- Native Android UI replacing WebView.
- Home screen widgets.
- Biometric app lock.

