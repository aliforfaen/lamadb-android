# LamaDB Android — Development Environment Setup

> No Android Studio. Command-line only. Build on `cachy`, deploy to your phone via ADB over WiFi through Tailscale.

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

## What `cachy` needs installed

These are the only tools required to build and test the Android client from the command line:

1. **JDK 17 or 21** — Android Gradle Plugin requires a modern JDK.
2. **Android SDK Command Line Tools** — the IDE-free SDK manager.
3. **Android SDK platform / build tools** — the compiler, `aapt`, `adb`, etc.
4. **Gradle wrapper** — usually committed in the repo; downloads Gradle automatically.
5. **ADB** — Android Debug Bridge, comes with platform-tools.
6. **Tailscale** — already on `cachy` and your phone; used to reach your phone over WiFi.

---

## Step-by-step setup on `cachy`

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

If `cachy` has no display, every command above still works. The build is entirely headless.

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

### 3. Pair and connect from `cachy`

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

### 4. Install and run from `cachy`

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n "com.lamadb.android/.MainActivity"
```

### 5. Watch logs from `cachy`

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
- Trigger install/logcat from `cachy`.
- Physically verify sensor behavior (WiFi disconnect, notification, etc.).

---

## Optional: leave a device permanently reachable

If you have an old Android phone, you can leave it plugged into `cachy` via USB and set ADB to TCP mode:

```bash
# While phone is USB-connected to cachy
adb tcpip 5555
adb connect localhost:5555
```

Then unplug. The phone stays reachable over WiFi as long as it stays on the same network. With Tailscale, it can be reachable from anywhere.

This is the most reliable way to let agents run instrumented tests without your manual pairing step.

---

## Optional: emulator on `cachy` (if KVM is available)

If `cachy` supports KVM, you can run an emulator for fully automated testing:

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

Add to `~/.bashrc` or `~/.zshrc` on `cachy`:

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

- Phone and `cachy` must both be on Tailscale and reachable (`ping PHONE_IP`).
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

## Backend deployment (LamaDB LXC)

The dashboard is served by the LamaDB backend running in an LXC container (`lamadb`) on Tailscale. The Android client loads it at `https://lamadb.tail91ec23.ts.net`.

### Connecting to the LXC

```bash
ssh -i ~/.ssh/messhias-master messhias@lamadb
# or
ssh -i ~/.ssh/messhias-master root@lamadb
```

Use the `messhias` user for normal work. Avoid running commands as `root` unless necessary.

### Backend layout

- Code lives at `/home/messhias/projects/lamadb`.
- The API runs as a Docker container named `lamadb_api`.
- PostgreSQL runs as `lamadb_postgres`.
- Static dashboard assets are copied into the Docker image at build time.

### Updating the backend after a dashboard JS fix

1. SSH into the LXC and pull the branch:

   ```bash
   ssh -i ~/.ssh/messhias-master messhias@lamadb
   cd projects/lamadb
   git pull github android-dashboard-hooks
   ```

   > Note: the local `origin` remote points to a non-existent local path; use the `github` remote.

2. Copy the updated static files into the running API container:

   ```bash
   docker cp static/js/app.js lamadb_api:/app/static/js/app.js
   docker cp static/js/pages/home.js lamadb_api:/app/static/js/pages/home.js
   # copy any other changed static assets
   ```

3. Verify the deployed files:

   ```bash
   curl -s https://lamadb.tail91ec23.ts.net/js/app.js | head -5
   ```

If you need to restart the API container instead:

```bash
docker restart lamadb_api
```

To fully rebuild the image (slower, required if Python dependencies or Dockerfile change):

```bash
cd projects/lamadb
docker compose up -d --build api
```

---

## On-device testing workflow

### Test credentials

A test admin API key is stored in `.env.test` (gitignored, never commit it):

```bash
source .env.test
```

Use these values in the Android login screen:

- URL: `https://lamadb.tail91ec23.ts.net`
- API key: value of `LAMADB_TEST_API_KEY`

The key is rotated regularly; update `.env.test` when Ali provides a new one.

### Physical device (preferred)

1. Ensure the phone is on Tailscale and ADB over WiFi is enabled.
2. Connect from `cachy`:

   ```bash
   adb connect PHONE_IP:PORT
   adb devices
   ```

3. Build, install, and launch:

   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.lamadb.android/.MainActivity
   ```

4. Watch dashboard JS logs:

   ```bash
   adb logcat -s LamaDB:D
   ```

### Emulator (limited)

The emulator on `cachy` can run the app for UI smoke tests, but it usually cannot reach the Tailnet backend URL. Use it for:

- Verifying the app builds and launches.
- Checking onboarding / login screen rendering.
- Running instrumented tests that do not require the backend.

```bash
# Start the existing AVD
emulator -avd lamadb-test -no-window -no-audio -gpu swiftshader_indirect -no-snapshot-save

# In another shell
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.lamadb.android/.MainActivity
```

### Capturing state from the device

```bash
# Screenshot
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png /tmp/screen.png

# App logs
adb logcat --pid=$(adb shell pidof -s com.lamadb.android)

# Clear app data (forces re-login)
adb shell pm clear com.lamadb.android
```

---

## WebView debugging

The debug APK enables WebView DevTools. While the app is running:

```bash
adb shell cat /proc/net/unix | grep webview_devtools_remote
# Forward the socket (replace PID)
adb forward tcp:9222 localabstract:webview_devtools_remote_<PID>
curl http://localhost:9222/json/list
```

JS console messages are also forwarded to logcat via the `DashboardJS` tag.

---

## Next steps

1. Run the setup script on `cachy` (or follow the steps above).
2. Verify `./gradlew build` works on a sample project.
3. Pair your phone via ADB over WiFi through Tailscale.
4. Hand the environment off to the Android agent to begin implementing the spec.

