# LamaDB Android — Desktop (cachy) Setup Checklist

> For the `cachy` desktop where the Android Developer and Tester agents will run under Pi.

## Install these tools

| # | Package | Why needed | Install notes (CachyOS / Arch) |
|---|---|---|---|
| 1 | **OpenJDK 17** | Required by Android Gradle Plugin | `pacman -S jdk17-openjdk` |
| 2 | **Android SDK command-line tools** | `sdkmanager`, `adb`, `aapt`, build tools | `scripts/setup-dev-env.sh` (run on `cachy`) |
| 3 | **Android SDK platform-tools** | `adb`, `fastboot`, `systrace` | `sdkmanager "platform-tools"` |
| 4 | **Android SDK build-tools 35.0.0** | Compiler, zipalign, apksigner | `sdkmanager "build-tools;35.0.0"` |
| 5 | **Android SDK platform 35 + 31** | Compile SDK and minSdk | `sdkmanager "platforms;android-35" "platforms;android-31"` |
| 6 | **Android Emulator + system image** | Headless emulator for the Tester | `sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"` |
| 7 | **KVM / QEMU** | Emulator acceleration | `pacman -S qemu-full` (or `qemu-base` + `qemu-emulators-full`) |
| 8 | **curl, unzip** | SDK download | `pacman -S curl unzip` |
| 9 | **Git** | Clone and commit | `pacman -S git` |
| 10 | **Tailscale** | ADB over WiFi to phone | `pacman -S tailscale` + `systemctl enable --now tailscaled` |
| 11 | **Pi (Hermes runtime)** | Agent runs here | `pip install pi` or `python -m pip install --user pi` (check Hermes docs for latest) |

## Verify after install

```bash
java -version
adb --version
sdkmanager --list_installed
ls /dev/kvm
```

## Emulator creation (one-time)

```bash
avdmanager create avd -n lamadb-test -k "system-images;android-35;google_apis;x86_64" -d "pixel_7"
```

## Start emulator headless

```bash
emulator -avd lamadb-test -no-window -no-audio -gpu swiftshader_indirect
```

## ADB over WiFi (for phone testing)

```bash
adb pair PHONE_IP:PAIR_PORT
adb connect PHONE_IP:DEBUG_PORT
adb devices
```

## Environment variables in `~/.bashrc` or `~/.zshrc`

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=/opt/android-sdk
export ANDROID_USER_HOME=$ANDROID_HOME
export ANDROID_AVD_HOME=$ANDROID_HOME/avd
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

## Notes

- Pi keeps the agent lean; the heavy lifting is done by the Android SDK and emulator on `cachy`.
- The Android Developer agent can write code and run JVM tests without the emulator.
- The Android Tester agent starts the emulator and runs `./gradlew connectedCheck`.
- If `cachy` has limited RAM, lower the emulator RAM to 4 GB in `~/.android/avd/lamadb-test.avd/config.ini`.
