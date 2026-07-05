#!/usr/bin/env bash
set -euo pipefail

# LamaDB Android — headless dev environment setup on Linux
# Supports Debian/Ubuntu (apt) and Arch/CachyOS (pacman).
# No Android Studio. Command-line tools only.

SDK_DIR="${ANDROID_HOME:-/opt/android-sdk}"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDLINE_TOOLS_ZIP="/tmp/cmdline-tools.zip"

# Detect package manager and JDK paths.
JDK_PKG=""
JAVA_HOME_PATH=""
PKG_MANAGER=""

if command -v pacman &>/dev/null; then
    PKG_MANAGER="pacman"
    JDK_PKG="jdk17-openjdk"
    JAVA_HOME_PATH="/usr/lib/jvm/java-17-openjdk"
elif command -v apt-get &>/dev/null; then
    PKG_MANAGER="apt"
    JDK_PKG="openjdk-17-jdk"
    JAVA_HOME_PATH="/usr/lib/jvm/java-17-openjdk-amd64"
else
    echo "ERROR: No supported package manager found (pacman or apt-get)."
    exit 1
fi

print_step() {
    echo
    echo "==> $1"
    echo
}

print_step "Installing base packages ($PKG_MANAGER)"
if [ "$PKG_MANAGER" = "pacman" ]; then
    sudo pacman -S --needed --noconfirm "$JDK_PKG" curl unzip git tailscale android-tools
else
    sudo apt-get update
    sudo apt-get install -y "$JDK_PKG" curl unzip git tailscale adb
fi

print_step "Verifying JDK"
if ! java -version 2>&1 | grep -q '17\|21'; then
    echo "ERROR: JDK 17/21 not found after install."
    exit 1
fi
java -version

print_step "Creating Android SDK directory"
if [ ! -d "$SDK_DIR" ]; then
    sudo mkdir -p "$SDK_DIR"
fi
sudo chown -R "$(id -u):$(id -g)" "$SDK_DIR"

print_step "Downloading Android command-line tools"
if [ ! -d "$SDK_DIR/cmdline-tools/latest" ]; then
    curl -L "$CMDLINE_TOOLS_URL" -o "$CMDLINE_TOOLS_ZIP"
    mkdir -p "$SDK_DIR/cmdline-tools"
    rm -rf "$SDK_DIR/cmdline-tools-tmp"
    unzip -q "$CMDLINE_TOOLS_ZIP" -d "$SDK_DIR/cmdline-tools-tmp"
    mv "$SDK_DIR/cmdline-tools-tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -rf "$SDK_DIR/cmdline-tools-tmp" "$CMDLINE_TOOLS_ZIP"
else
    echo "Command-line tools already present."
fi

print_step "Adding environment variables to shell profile"
SHELL_PROFILE="$HOME/.bashrc"
if [ -n "${ZSH_VERSION:-}" ] || [ -f "$HOME/.zshrc" ]; then
    SHELL_PROFILE="$HOME/.zshrc"
fi

if ! grep -q 'ANDROID_HOME=' "$SHELL_PROFILE" 2>/dev/null; then
    {
        echo
        echo "# Android SDK environment for lamadb-android"
        echo "export JAVA_HOME=$JAVA_HOME_PATH"
        echo "export ANDROID_HOME=$SDK_DIR"
        echo "export ANDROID_USER_HOME=\$ANDROID_HOME"
        echo "export ANDROID_AVD_HOME=\$ANDROID_HOME/avd"
        echo 'export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"'
    } >> "$SHELL_PROFILE"
    echo "Updated $SHELL_PROFILE"
else
    echo "Android environment already present in $SHELL_PROFILE"
fi

# Export for the current shell run.
export JAVA_HOME="$JAVA_HOME_PATH"
export ANDROID_HOME="$SDK_DIR"
export ANDROID_USER_HOME="$ANDROID_HOME"
export ANDROID_AVD_HOME="$ANDROID_HOME/avd"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

print_step "Accepting SDK licenses and installing required packages"
if [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    yes | sdkmanager --licenses
    sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" "platforms;android-31" "emulator" "system-images;android-35;google_apis;x86_64"
else
    echo "ERROR: sdkmanager not found at $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
    exit 1
fi

print_step "Creating AVD 'lamadb-test'"
echo "no" | avdmanager create avd -n lamadb-test -k "system-images;android-35;google_apis;x86_64" -d "pixel_7" --force

print_step "Verification"
echo "ADB version:"
adb --version
echo
echo "Installed SDK packages:"
sdkmanager --list_installed | grep -E 'build-tools|platform-tools|platforms|emulator|system-images'
echo
echo "AVDs:"
avdmanager list avd

print_step "Done"
echo "Android SDK is installed at: $ANDROID_HOME"
echo "Run 'source $SHELL_PROFILE' or open a new terminal to refresh environment variables."
echo "To start the emulator: emulator -avd lamadb-test -no-window -no-audio -gpu swiftshader_indirect"
echo "To pair your phone: adb pair PHONE_IP:PORT  (then adb connect PHONE_IP:PORT)"
