# cachy setup log — lamadb-android (LAMA-57)

> Personal scratchpad for getting the Android dev environment running on the desktop `cachy`.
> This is a learning/pairing session, not a coding sprint.

---

## Goal

Run Android Developer and Android Tester agents on `cachy` (the local desktop) using Pi / Hermes.
Eventually:
- Agents develop and test the app virtually on the desktop.
- I deploy to my real phone via wireless ADB over Tailscale.
- Bonus: see the emulated app rendered on the desktop somehow.

---

## Current state (2026-07-05)

- Multica CLI installed via Homebrew at `/home/linuxbrew/.linuxbrew/bin/multica`.
- Authenticated as Aleksander Olsen.
- LAMA-57 moved to `in_progress`.
- Project `LamaDB Android Client` (4d89b863) exists with 17 issues.
- Repo at `~/LamaFiles/projects/lamadb-android` currently has **no Android code** — only docs and the setup script.

### Tool audit

| Tool | Status | Notes |
|------|--------|-------|
| Java / JDK | ❌ missing | Need JDK 17 or 21 |
| Android SDK dir | ⚠️ partial | `/opt/android-sdk` exists, mostly empty |
| `adb` | ✅ present | From Arch `android-tools` package at `/usr/bin/adb` |
| `sdkmanager` | ❌ missing | Need Android command-line tools |
| `emulator` | ❌ missing | Need Android Emulator package |
| KVM | ✅ available | `/dev/kvm` exists |
| git / curl / unzip / tailscale | ✅ present | System packages |
| Pi runtime | ✅ present | `/home/messhias/.npm-global/bin/pi` |
| Hermes runtime | ✅ present | `/home/messhias/.local/bin/hermes` |
| Shell env vars | ❌ missing | No `JAVA_HOME`, `ANDROID_HOME`, etc. in `.bashrc`/`.zshrc` |

OS: CachyOS (Arch-based), so `pacman` is the package manager.

---

## Open questions / things to learn

1. Do I need Android Studio at all, or can command-line tools really do everything?
2. What's the best way to see the emulator UI on the desktop? (`-gpu swiftshader_indirect`? remote framebuffer?)
3. Will the Arch `android-tools` `adb` conflict with the SDK's `adb` once installed?
4. Should the setup script support both Ubuntu (apt) and CachyOS/Arch (pacman)?
5. How do I want the agents to report back into Multica issues?

---

## Done

- [x] Install Multica CLI and verify auth.
- [x] Load LAMA-57 and project status.
- [x] Audit existing docs and scripts.
- [x] Audit current toolchain on `cachy`.

## Next

- [ ] Install JDK 17 via `pacman`.
- [ ] Download and install Android command-line tools into `/opt/android-sdk`.
- [ ] Install `platform-tools`, `build-tools;35.0.0`, `platforms;android-35`, `platforms;android-31`, `emulator`, system image.
- [ ] Add `JAVA_HOME`, `ANDROID_HOME`, `PATH` to shell profile.
- [ ] Create AVD `lamadb-test`.
- [ ] Start emulator headless and verify it boots.
- [ ] Update `scripts/setup-dev-env.sh` and/or docs for CachyOS.
- [ ] Test `./gradlew build` on a minimal sample once skeleton exists.

---

## Notes from Ali

(Add thoughts here as we go.)


## 2026-07-05 — Setup approach

Decided: Ali will run `sudo` commands himself while I guide. This is a learning session, so seeing each step is useful. I'll verify outputs after each command.


## 2026-07-05 — JDK installed

- `java -version` reports OpenJDK 17.0.19.
- Path on CachyOS: `/usr/lib/jvm/java-17-openjdk` (note: not the `-amd64` suffix used in Ubuntu docs).
- `JAVA_HOME` still not set; will add to shell profile after SDK is in place.


## 2026-07-05 — SDK installed and emulator verified

Command-line tools downloaded to `/opt/android-sdk/cmdline-tools/latest`.
Installed via `sdkmanager`:
- `platform-tools` (37.0.0)
- `build-tools;35.0.0`
- `platforms;android-35`
- `platforms;android-31`
- `emulator` (36.6.11)
- `system-images;android-35;google_apis;x86_64`

Created AVD `lamadb-test` (Pixel 7, API 35, x86_64).

Started emulator headless:
```bash
emulator -avd lamadb-test -no-window -no-audio -gpu swiftshader_indirect -no-snapshot-save
```
Result: booted in ~23s, `adb devices` showed `emulator-5554 device`, `sys.boot_completed=1`.

Environment variables added to `~/.zshrc`:
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk`
- `ANDROID_HOME=/opt/android-sdk`
- `ANDROID_USER_HOME=$ANDROID_HOME`
- `ANDROID_AVD_HOME=$ANDROID_HOME/avd`
- Updated `PATH`

### Seeing the emulator on the desktop

For a rendered window (not headless), drop `-no-window`:
```bash
emulator -avd lamadb-test -no-audio -gpu swiftshader_indirect
```
This will open a Qt window on `cachy` so you can see the virtual device as agents work.

### Updated files

- `scripts/setup-dev-env.sh` — now detects Arch/CachyOS (`pacman`) vs Ubuntu/Debian (`apt-get`) and uses the right JDK path.
- `docs/desktop-setup.md` — updated env vars and JAVA_HOME path.
- `docs/development.md` — updated env vars and added distro note.

## What's left for LAMA-57

- [ ] Re-source `~/.zshrc` in a fresh terminal to confirm env vars load cleanly.
- [ ] Create a minimal Android project skeleton (LAMA-58).
- [ ] Run `./gradlew build` on that skeleton to prove the toolchain end-to-end.
- [ ] Pair/test wireless ADB to your phone over Tailscale.

## 2026-07-05 — Skeleton complete

- Created minimal Android project skeleton (LAMA-58).
- `./gradlew build`, `./gradlew test`, `./gradlew assembleDebug`, and `./gradlew installDebug` verified on `cachy`.
- Next: LAMA-59 WebView dashboard with pull-to-refresh.

---

## 2026-07-08 — Dashboard blank-page debugging handoff

**Context:** Ali is testing the LamaDB Android app on his physical device (SM-S721B). The app opens, login works, but the dashboard home page is blank — only the native bottom nav and the "Online" chip are visible. This session moved troubleshooting to the emulator so it could be done unattended, and added tooling to make repeated login less painful.

**Goal for the next session:** make the web dashboard actually render inside the WebView. The WebView loads the URL, auth succeeds, and the home-page JS fetches data (1 attention note, 20 activity items). The content DOM exists and has height, but it is not visible because the root layout chain is collapsed.

---

### Repositories and branches

**Android client:** `~/LamaFiles/projects/lamadb-android`  
Current branch: `lama-polish`  
Uncommitted changes:
- `app/build.gradle.kts` — debug build reads `LAMADB_TEST_URL` and `LAMADB_TEST_API_KEY` from `.env.test` and exposes them as `BuildConfig` fields.
- `app/src/main/kotlin/.../ui/login/LoginScreen.kt` — shows a **"Debug log in"** button on debug builds when those `BuildConfig` fields are non-empty. Tapping it fills the test URL/key and logs in automatically. This is for dev/testing only and is stripped from release builds.
- `app/src/main/kotlin/.../ui/dashboard/DashboardScreen.kt` — had a temporary `injectDiagnostics()` function during this session; it was removed before handoff. The WebView console-message logging (commit `ac75106`) is still present and very useful.

**Backend:** `~/LamaFiles/projects/lamadb`  
Current branch: `android-dashboard-hooks`  
Uncommitted change:
- `static/css/shell.css` — changed `html, body` to `height: 100%; margin: 0;` and `.app-root` from `height: 100vh` to `min-height: 100%; height: 100%; box-sizing: border-box;`. This fix is already deployed to the LXC.

**Neither repo has been committed or pushed yet.** Ali should review and decide what to keep.

---

### Backend / LXC access

The LamaDB backend runs in an LXC container reachable as `lamadb` on the Tailnet.

- Host: `lamadb` / `lamadb.tail91ec23.ts.net` (`100.91.221.107`)
- User: `messhias`
- SSH key: `~/.ssh/messhias-master`
- Direct SSH (not Tailscale SSH):
  ```bash
  ssh -i ~/.ssh/messhias-master messhias@lamadb
  ```
- Backend source on the LXC: `/home/messhias/projects/lamadb/`
- Docker containers:
  ```bash
  docker ps
  # lamadb_api     -> API on port 8000
  # lamadb_postgres -> Postgres on port 5432
  ```

**How to update the backend CSS:**
```bash
# From cachy, copy the local fix to the LXC
scp -i ~/.ssh/messhias-master \
  /home/messhias/LamaFiles/projects/lamadb/static/css/shell.css \
  messhias@lamadb:/home/messhias/projects/lamadb/static/css/shell.css

# On the LXC, copy it into the running API container and restart
ssh -i ~/.ssh/messhias-master messhias@lamadb '
  docker cp /home/messhias/projects/lamadb/static/css/shell.css lamadb_api:/app/static/css/shell.css
  docker restart lamadb_api
'
```

**How to restart the backend entirely:**
```bash
ssh -i ~/.ssh/messhias-master messhias@lamadb 'cd /home/messhias/projects/lamadb && docker compose restart'
# or just the API:
ssh -i ~/.ssh/messhias-master messhias@lamadb 'docker restart lamadb_api lamadb_postgres'
```

The public endpoint Ali uses is `https://lamadb.tail91ec23.ts.net/` (Tailscale serve).

---

### Android test credentials

Stored in `/home/messhias/LamaFiles/projects/lamadb-android/.env.test` (gitignored):
```bash
LAMADB_TEST_URL=https://lamadb.tail91ec23.ts.net
LAMADB_TEST_API_KEY=lamadb_live_gmSTrngb0XgpFYqm6-AfXeiJW5T0SCfLHQRW0sp0Rrg
```
This key is rotated regularly; do not commit it.

---

### Device setup

**Physical device (Ali's phone):**
- Model: Samsung Galaxy S24 FE (`SM-S721B`)
- ADB transport: wireless over Tailnet
- Address: `100.73.138.28:36511`
- Status: was connected during this session; Ali may disconnect it when going out.

**Emulated device (for agent testing):**
- AVD: `lamadb-test` (Pixel 7-like, API 35 x86_64)
- ADB: `emulator-5554`
- Start headless:
  ```bash
  emulator -avd lamadb-test -no-window -no-audio -gpu swiftshader_indirect -no-snapshot-save
  ```
- Start with UI:
  ```bash
  emulator -avd lamadb-test -no-audio -gpu swiftshader_indirect
  ```
- The emulator can reach `https://lamadb.tail91ec23.ts.net/` (verified with `adb shell ping`).

**Check devices:**
```bash
adb devices -l
```

---

### Build / install / test loop on the emulator

```bash
cd /home/messhias/LamaFiles/projects/lamadb-android

# Build
./gradlew assembleDebug --no-daemon

# Install on emulator
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk

# Clear data (optional; also clears granted permissions)
adb -s emulator-5554 shell pm clear com.lamadb.android

# Grant permissions the app needs for presence service
adb -s emulator-5554 shell pm grant com.lamadb.android android.permission.ACCESS_FINE_LOCATION
adb -s emulator-5554 shell pm grant com.lamadb.android android.permission.ACCESS_COARSE_LOCATION
adb -s emulator-5554 shell pm grant com.lamadb.android android.permission.POST_NOTIFICATIONS

# Launch
adb -s emulator-5554 shell am start -n com.lamadb.android/.MainActivity
```

On a fresh install the flow is:
1. Onboarding → tap **Skip** (bottom of screen, around y≈2265 on 1080×2400).
2. Login → tap **Debug log in** (only in debug builds with `.env.test`).
3. "Set up home WiFi" dialog → either enter an SSID and tap **Enable presence**, or tap **Skip**.
   - **Skip currently crashes the app** unless location permissions were granted beforehand (see known issues below).
4. Dashboard loads. Home tab is selected by default.

**Useful log filtering:**
```bash
adb -s emulator-5554 logcat -d | grep -E "LamaDB|DashboardJS|FATAL EXCEPTION|AndroidRuntime" | tail -60
```

---

### Current app state

- The app builds and installs cleanly on both emulator and physical device.
- Login works (manual entry, QR, or debug button).
- The WebView loads `https://lamadb.tail91ec23.ts.net/`, auth succeeds, and the dashboard JS fetches real data.
- The native UI renders: bottom nav, "Online/Offline" chip, pull-to-refresh.
- **The web content itself is not visible on the Home tab.** The screen is the body background color with no cards/text.
- Other tabs (Tasks, Health, Settings) were not fully verified.

---

### Diagnostic findings

WebView console logging shows:
```
[home] load success, attentionNotes: 1 activityItems: 20
[home] section class: nav-item active x-cloak: false
```

A temporary DOM probe (now removed) showed the real problem:
```json
{
  "window": { "innerWidth": 412, "innerHeight": 759, "dpr": 2.625 },
  "html":   { "h": 0, "computedHeight": "0px", "computedMinHeight": "0px" },
  "body":   { "h": 0, "computedHeight": "0px", "computedMinHeight": "0px" },
  ".app-root": { "h": 0, "computedHeight": "0px", "computedMinHeight": "100%" },
  ".app-body": { "h": 0, "computedHeight": "0px", "computedMinHeight": "0px" },
  ".app-main": { "h": 76, "computedHeight": "76px" },
  ".app-page.active": { "h": 2318, "computedHeight": "2318.1px" },
  ".home-page": { "h": 2318, "computedHeight": "2318.1px" }
}
```

**Interpretation:** the dashboard DOM and data are fine, but `html`, `body`, `.app-root`, and `.app-body` are all `0px` high. `.app-main` only gets `76px` of visible height, and its overflowing content is clipped by `.app-body { overflow: hidden }`. The content exists; it is just not being laid out into the viewport.

**Why the CSS fix in `shell.css` did not fully work:**
- `shell.css` now sets `html, body { height: 100%; }` and `.app-root { height: 100%; }`.
- `tokens.css` (loaded before `shell.css`) sets `html, body { min-height: 100vh; }`.
- `dashboard.css` (loaded after `shell.css`) also sets `html, body { height: 100%; }` and `body { overflow: hidden; }`.
- Despite all of this, in the Android WebView `html.computedHeight` and `html.computedMinHeight` both resolve to `0px`. That suggests either:
  1. The WebView's initial containing block / viewport height is `0` at CSS computation time, so `100%` and `100vh` both resolve to `0`.
  2. Some other CSS or JS is overriding the height after load.
  3. The WebView needs an explicit `layout_height` / viewport setting to give the page a non-zero root height.

The WebView in Compose uses `AndroidView(factory = { webViewRef }, modifier = Modifier.fillMaxSize())`, so the native view should fill the screen. The background color fills the screen, which proves the WebView itself has size. The problem is specifically the CSS root-height computation inside the page.

**Next things to try:**
1. Force a reflow/recalc after the WebView has size (e.g. inject JS that sets `document.documentElement.style.height = window.innerHeight + 'px'` and see if content appears).
2. Replace percentage/vh heights with `100dvh` / `100svh` and see if the WebView respects them.
3. Inspect via Chrome DevTools remote debugging (forward `localabstract:webview_devtools_remote_<pid>` to a port; the WebView rejects non-Chrome origins, so use a Chrome browser or the exact devtoolsFrontend origin).
4. Check whether `tokens.css` or `dashboard.css` order/rules are overriding `shell.css` in unexpected ways.
5. Verify the viewport `<meta name="viewport" ...>` is not causing a zero-height ICB.

---

### Known issues discovered

1. **Dashboard home page is blank** (primary blocker).
2. **Presence service crash on Skip WiFi setup:** if location runtime permissions are not granted, `PresenceService.startForeground()` throws a `SecurityException` for `foregroundServiceType="location"`. The app shows "LamaDB keeps stopping".
   - Workaround: grant permissions via adb before skipping, or enter an SSID and tap **Enable presence** (which seems to avoid the crash path in current code).
   - Proper fix: do not start `PresenceService` unless location permission is granted, or request the permission first, or degrade gracefully.
3. **Random pull-to-refresh on physical device:** Ali reported this earlier. It may be the swipe-to-refresh gesture conflicting with WebView scrolling. Consider disabling pull-to-refresh until the page scrolls correctly, or tuning the pull-refresh threshold.
4. **16 KB page-size warning** appears on first launch on the physical device. The app still runs, but Google Play will eventually require 16 KB alignment.

---

### Files that changed this session

**Android repo (`lama-polish`):**
- `app/build.gradle.kts`
- `app/src/main/kotlin/com/lamadb/android/ui/login/LoginScreen.kt`
- `app/src/main/kotlin/com/lamadb/android/ui/dashboard/DashboardScreen.kt` (diagnostics removed; console logging remains)

**Backend repo (`android-dashboard-hooks`):**
- `static/css/shell.css` (deployed to LXC)

---

### Recommended next steps

1. Fix the root-height/layout issue so the dashboard content is visible.
2. Fix the `PresenceService` crash when the user skips WiFi setup without location permission.
3. Re-test on the emulator end-to-end (login → dashboard content visible → navigation tabs).
4. Push the CSS fix to `android-dashboard-hooks` and the Android changes to `lama-polish`.
5. Install on Ali's physical device and verify over Tailscale.
6. Address the pull-to-refresh sensitivity if it still triggers randomly.

---

### People / agents

- Ali — product owner / tester.
- Huginn — was assigned Multica tasks around backend deployment and repo cleanup (per earlier conversation).
- Muninn — knows the LXC setup; ask if SSH/connection details change.

