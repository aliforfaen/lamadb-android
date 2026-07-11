#!/usr/bin/env bash
set -euo pipefail
# LamaDB Android — start the test emulator headless

AVD_NAME="${AVD_NAME:-lamadb-test}"
EMULATOR_ARGS="${EMULATOR_ARGS:--no-window -no-audio -gpu swiftshader_indirect -no-snapshot-save}"

echo "=== Starting AVD: $AVD_NAME ==="
emulator -avd "$AVD_NAME" $EMULATOR_ARGS &
EMULATOR_PID=$!

echo "Waiting for emulator to boot..."
adb wait-for-device

# Wait for boot to complete (up to 120s)
for i in $(seq 1 120); do
    status=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [ "$status" = "1" ]; then
        echo "Emulator booted in ${i}s"
        break
    fi
    sleep 1
done

# Install debug APK first (runtime grants require the package to exist)
echo "Installing debug APK..."
cd "$(dirname "$0")/.."
./gradlew installDebug 2>&1 | tail -1

# Grant runtime permissions
echo "Granting permissions..."
adb shell pm grant com.lamadb.android android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell pm grant com.lamadb.android android.permission.CAMERA 2>/dev/null || true
adb shell pm grant com.lamadb.android android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
adb shell pm grant com.lamadb.android android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true

echo ""
echo "=== Emulator ready ==="
adb devices | grep -v "List of devices"
echo ""
echo "PID: $EMULATOR_PID"
echo "To stop: kill $EMULATOR_PID  or  ./scripts/stop-emulator.sh"
