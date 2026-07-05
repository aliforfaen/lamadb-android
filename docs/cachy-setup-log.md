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

