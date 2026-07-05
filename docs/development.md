# LamaDB Android — Development Environment Setup

> No Android Studio. Command-line only. Build on `norheim`, deploy to your phone via ADB over WiFi through Tailscale.

---

## The Android pipeline in web/docker terms

If you know how Docker containers work, Android builds are similar:

| Docker / web | Android equivalent |
|---|---|
| `Dockerfile` | `build.gradle(.kts)` + manifest |
| `docker build` | `./gradlew assembleDebug` |
| `docker run` | `adb install app.apk` + `adb shell am start` |
| Container image | `.apk` file |
| `docker logs` | `adb logcat` |
| Environment variables | `local.properties` + `~/.gradle/gradle.properties` |
| Container registry | Play Store (not needed here) |

You write Kotlin source, Gradle packages it into an APK, and `adb` pushes that APK to your Android device and installs it.

---

## What `norheim` needs installed

These are the only tools required to build and test the Android client from the command line:

1. **JDK 17 or 21** — Android Gradle Plugin requires a modern JDK.
2. **Android SDK Command Line Tools** — the IDE-free SDK manager.
3. **Android SDK platform / build tools** — the compiler, `aapt`, `adb`, etc.
4. **Gradle wrapper** — usually committed in the repo; downloads Gradle automatically.
5. **ADB** — Android Debug Bridge, comes with platform-tools.
6. **Tailscale** — already on `norheim` and your phone; used to reach your phone over WiFi.

---

## Step-by-step setup on `norheim`

### 1. Install a JDK

```bash
# Ubuntu / Debian
sudo apt update
sudo apt install openjdk-17-jdk

# Verify
java -version
javac -version
```

### 2. Create the Android SDK directory

```bash
sudo mkdir -p /opt/android-sdk
sudo chown "$USER:$USER" /opt/android-sdk
export ANDROID_HOME=/opt/android-sdk
export ANDROID_USER_HOME=$ANDROID_HOME
```

Add these to `~/.bashrc` (or `~/.zshrc`) so they persist:

```bash
export ANDROID_HOME=/opt/android-sdk
export ANDROID_USER_HOME=$ANDROID_HOME
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

### 3. Download command-line tools

```bash
cd /opt/android-sdk
mkdir -p cmdline-tools
curl -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip cmdline-tools.zip -d cmdline-tools-tmp
mv cmdline-tools-tmp/cmdline-tools cmdline-tools/latest
rm -rf cmdline-tools-tmp cmdline-tools.zip
```

### 4. Accept licenses and install required packages

```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

For Phase 1 you also need:

```bash
sdkmanager "platforms;android-31"  # minSdk
```

### 5. Verify tools

```bash
adb --version
aapt version
sdkmanager --list_installed
```

---

## Project build workflow

Once the repo has a Gradle project, the agent/CI will run:

> **Note:** The minimal Android skeleton now exists in this repo. The commands below have been verified on `cachy`.

```bash
cd ~/LamaFiles/projects/lamadb-android

# Compile everything
./gradlew build

# Build a debug APK
./gradlew assembleDebug

# Run JVM unit tests
./gradlew test

# Lint
./gradlew lint

# Install to a connected device
./gradlew installDebug

# Or directly with adb
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n "com.lamadb.android/.MainActivity"
```

If `norheim` has no display, every command above still works. The build is entirely headless.

---

## Deploying to your phone via Tailscale + ADB over WiFi

### 1. Put your phone on the Tailscale network

Already done. Note the Tailscale IP of your phone:

```bash
# From your phone or the Tailscale admin panel
# e.g. 100.99.88.77
```

### 2. Enable ADB over WiFi on the phone

On the phone:

- Go to **Settings → About phone → Build number**, tap 7 times to enable Developer Options.
- **Settings → System → Developer options → Wireless debugging** → Enable.
- Note the port and pairing code (Android 11+ requires pairing).

### 3. Pair and connect from `norheim`

```bash
# Pair first (Android 11+)
adb pair PHONE_IP:PORT
# Enter the pairing code

# Connect
adb connect PHONE_IP:NEW_PORT

# Verify
adb devices
```

Example:

```bash
adb pair 100.99.88.77:42073
adb connect 100.99.88.77:38679
adb devices
```

### 4. Install and run from `norheim`

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n "com.lamadb.android/.MainActivity"
```

### 5. Watch logs from `norheim`

```bash
adb logcat -s LamaDB:D
```

You can leave this open in a `tmux` or `screen` session.

---

## Testing without Android Studio

| Test type | Command | Needs device? |
|---|---|---|
| Compile | `./gradlew build` | No |
| Unit tests | `./gradlew test` | No |
| Lint | `./gradlew lint` | No |
| Install APK | `./gradlew installDebug` or `adb install` | Yes |
| Instrumented tests | `./gradlew connectedCheck` | Yes |
| View logs | `adb logcat` | Yes (or emulator) |
| Screenshot | `adb shell screencap -p /sdcard/screen.png` then `adb pull` | Yes |

### What agents can do without you

- Write code and tests.
- Run `./gradlew build` and `./gradlew test`.
- Run `./gradlew lint`.

### What only you can do

- Pair the phone for ADB over WiFi (unless you leave it paired).
- Trigger install/logcat from `norheim`.
- Physically verify sensor behavior (WiFi disconnect, notification, etc.).

---

## Optional: leave a device permanently reachable

If you have an old Android phone, you can leave it plugged into `norheim` via USB and set ADB to TCP mode:

```bash
# While phone is USB-connected to norheim
adb tcpip 5555
adb connect localhost:5555
```

Then unplug. The phone stays reachable over WiFi as long as it stays on the same network. With Tailscale, it can be reachable from anywhere.

This is the most reliable way to let agents run instrumented tests without your manual pairing step.

---

## Optional: emulator on `norheim` (if KVM is available)

If `norheim` supports KVM, you can run an emulator for fully automated testing:

```bash
# Check KVM
ls /dev/kvm
# If the file exists, KVM is available

sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"

avdmanager create avd -n test -k "system-images;android-35;google_apis;x86_64" -d "pixel_7"

emulator -avd test -no-window -no-audio

# Then in another shell:
adb devices
./gradlew connectedCheck
```

If `/dev/kvm` does not exist, agents are limited to compile + unit tests. Your phone becomes the real test environment.

---

## Environment variables summary

Add to `~/.bashrc` or `~/.zshrc` on `norheim`:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=/opt/android-sdk
export ANDROID_USER_HOME=$ANDROID_HOME
export ANDROID_AVD_HOME=$ANDROID_HOME/avd
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

> On Ubuntu/Debian the JDK path is `/usr/lib/jvm/java-17-openjdk-amd64` instead.

---

## Troubleshooting

### `sdkmanager: command not found`

`cmdline-tools/latest/bin` is not in `$PATH`. Re-source your shell profile or use the full path.

### `adb devices` shows no device

- Phone and `norheim` must both be on Tailscale and reachable (`ping PHONE_IP`).
- Wireless debugging must be enabled.
- Pairing code must be entered correctly.
- Some Samsung phones require disabling "Disable USB debugging" or similar security settings.

### Build fails with "SDK location not found"

Create `local.properties` in the project root:

```properties
sdk.dir=/opt/android-sdk
```

### Gradle downloads are slow

Add a `gradle.properties` in the project root or `~/.gradle/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.parallel=true
```

---

## Next steps

1. Run the setup script on `norheim` (or follow the steps above).
2. Verify `./gradlew build` works on a sample project.
3. Pair your phone via ADB over WiFi through Tailscale.
4. Hand the environment off to the Android agent to begin implementing the spec.

