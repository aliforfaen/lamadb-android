#!/usr/bin/env bash
#
# Smoke-test helper for the LamaDB Android client.
#
# Usage:
#   ./scripts/smoke-test.sh wiki       # fresh start, skip onboarding, log in, open Wiki
#   ./scripts/smoke-test.sh settings   # open Settings directly
#   ./scripts/smoke-test.sh dashboard  # open Dashboard directly
#   ./scripts/smoke-test.sh widget     # install and prompt to add the home-screen widget
#   ./scripts/smoke-test.sh clean      # clear app data only
#
# This script only operates on debug builds and never touches release artifacts.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_ID="com.lamadb.android"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

command -v adb >/dev/null 2>&1 || { echo "adb not found in PATH"; exit 1; }

ensure_apk() {
    if [[ ! -f "$APK" ]]; then
        echo "Debug APK not found; building..."
        (cd "$PROJECT_DIR" && ./gradlew assembleDebug)
    fi
}

grant_permissions() {
    # Permissions that the app declares and that are grantable via adb.
    # POST_NOTIFICATIONS is runtime on Android 13+ but pm grant works on emulators
    # and some devices. Location must usually be granted while using the app, so
    # we grant the closest available level.
    adb shell pm grant "$APP_ID" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
    adb shell pm grant "$APP_ID" android.permission.CAMERA 2>/dev/null || true
    adb shell pm grant "$APP_ID" android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
    adb shell pm grant "$APP_ID" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
}

launch() {
    local screen="${1:-dashboard}"
    local extras="${2:-}"
    adb shell am start -n "$APP_ID/.MainActivity" \
        --ez SKIP_ONBOARDING true \
        --ez SKIP_PRESENCE_SETUP true \
        --ez USE_TEST_ACCOUNT true \
        --es START_SCREEN "$screen" \
        $extras
}

case "${1:-}" in
    wiki|dashboard|tasks|health|settings)
        ensure_apk
        adb install -r "$APK"
        grant_permissions
        launch "$1"
        ;;
    widget)
        ensure_apk
        adb install -r "$APK"
        grant_permissions
        echo "To add the widget, run on the device:"
        echo "  long-press home screen -> Widgets -> LamaDB events"
        adb shell am start -n "$APP_ID/.MainActivity" \
            --ez SKIP_ONBOARDING true \
            --ez SKIP_PRESENCE_SETUP true \
            --ez USE_TEST_ACCOUNT true
        ;;
    clean)
        adb shell pm clear "$APP_ID"
        echo "App data cleared."
        ;;
    seed)
        ensure_apk
        adb install -r "$APK"
        grant_permissions
        adb shell am start -n "$APP_ID/.MainActivity" \
            --ez SKIP_ONBOARDING true \
            --ez SKIP_PRESENCE_SETUP true \
            --ez USE_TEST_ACCOUNT true \
            --ez SEED_DATA true
        ;;
    *)
        echo "Usage: $0 {wiki|dashboard|tasks|health|settings|widget|clean|seed}"
        exit 1
        ;;
esac
