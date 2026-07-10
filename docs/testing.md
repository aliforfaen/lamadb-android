# LamaDB Android — Testing Harness & Smoke Tests

This document describes the debug-only testing harness added in LAMA-96. Everything here is gated on `BuildConfig.DEBUG` and is unreachable in release builds.

---

## Requirements

- A debug APK (`./gradlew assembleDebug`).
- `.env.test` in the project root with valid credentials:
  ```bash
  LAMADB_TEST_URL=https://lamadb.tail91ec23.ts.net
  LAMADB_TEST_API_KEY=your-test-key
  ```
- A device or emulator reachable via `adb`.

---

## ADB launch flags

All flags are intent extras passed to `MainActivity`.

| Extra | Type | Effect |
|---|---|---|
| `SKIP_ONBOARDING` | boolean | Mark onboarding as completed so the login screen appears immediately. |
| `SKIP_PRESENCE_SETUP` | boolean | Mark presence setup as complete so the dialog does not appear after login. |
| `USE_TEST_ACCOUNT` | boolean | Auto-log in using `BuildConfig.LAMADB_TEST_URL` and `BuildConfig.LAMADB_TEST_API_KEY`. |
| `START_SCREEN` | string | Open a specific bottom-nav destination: `dashboard`, `wiki`, `tasks`, `health`, `settings`. |
| `SEED_DATA` | boolean | Insert fake events, wiki pages, and a test notification after login. |
| `RESET_FIRST_LAUNCH` | boolean | Clear all app data and restart from a blank first-launch state. |

### Examples

Skip onboarding and open the Wiki tab after auto-login:

```bash
adb shell am start -n com.lamadb.android/.MainActivity \
  --ez SKIP_ONBOARDING true \
  --ez SKIP_PRESENCE_SETUP true \
  --ez USE_TEST_ACCOUNT true \
  --es START_SCREEN wiki
```

Reset to first-launch state:

```bash
adb shell am start -n com.lamadb.android/.MainActivity \
  --ez RESET_FIRST_LAUNCH true
```

Seed fixtures for offline screens:

```bash
adb shell am start -n com.lamadb.android/.MainActivity \
  --ez SKIP_ONBOARDING true \
  --ez SKIP_PRESENCE_SETUP true \
  --ez USE_TEST_ACCOUNT true \
  --ez SEED_DATA true \
  --es START_SCREEN wiki
```

### Permissions

The app cannot grant its own runtime permissions. Grant them via adb before testing:

```bash
adb shell pm grant com.lamadb.android android.permission.POST_NOTIFICATIONS
adb shell pm grant com.lamadb.android android.permission.CAMERA
adb shell pm grant com.lamadb.android android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.lamadb.android android.permission.ACCESS_COARSE_LOCATION
```

---

## Smoke-test script

`scripts/smoke-test.sh` wraps the common adb commands.

```bash
./scripts/smoke-test.sh wiki       # fresh install, skip onboarding, login, open Wiki
./scripts/smoke-test.sh dashboard  # open Dashboard
./scripts/smoke-test.sh settings   # open Settings
./scripts/smoke-test.sh tasks      # open Scheduled tasks
./scripts/smoke-test.sh health     # open Health data
./scripts/smoke-test.sh seed       # install, login, and seed fixtures
./scripts/smoke-test.sh widget     # install and prepare to add the home-screen widget
./scripts/smoke-test.sh clean      # clear app data
```

The script builds the debug APK if it is missing, installs it, grants permissions, and launches with the right flags.

---

## In-app Debug menu

In debug builds the Settings screen contains a **Debug tools** card (above the version footer). Actions:

- **Skip onboarding on next launch** — sets onboarding completed.
- **Seed fake events / wiki pages** — runs `TestDataSeeder.seedAll()`.
- **Reset to first-launch state** — clears auth, presence, onboarding, and Room data, then restarts the app.
- **Trigger workers now** — enqueues one-off runs of `WikiSyncWorker`, `EventWidgetRefreshWorker`, and `NtfyPushWorker`.
- **Crash app** — throws a `RuntimeException` to verify crash reporting.

The card is not compiled into release builds because the entire branch is guarded with `BuildConfig.DEBUG`.

---

## Returning to a known baseline

To force the full onboarding → login → presence setup flow again:

```bash
adb shell pm clear com.lamadb.android
adb shell am start -n com.lamadb.android/.MainActivity
```

---

## Screenshot and log conventions for agent reports

When filing smoke-test results, include:

1. The exact command used.
2. `adb logcat -s LamaDB:D` output for the session.
3. A screenshot of the final screen:
   ```bash
   adb shell screencap -p /sdcard/lamadb-smoke.png
   adb pull /sdcard/lamadb-smoke.png /tmp/lamadb-smoke.png
   ```
4. Whether the test was run on a physical device or the `lamadb-test` emulator.

---

## Security note

Every debug backdoor is wrapped in `BuildConfig.DEBUG`. Release builds ignore all intent extras, do not show the Debug tools card, and never call `TestDataSeeder`. No new permissions or exported components were added.
