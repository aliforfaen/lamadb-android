#!/usr/bin/env bash
set -euo pipefail
# LamaDB Android — stop the test emulator

echo "Stopping emulator..."
adb emu kill 2>/dev/null || true

# Also kill any lingering emulator processes
pkill -f "emulator.*lamadb-test" 2>/dev/null || true

echo "Emulator stopped."
