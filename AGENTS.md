# LamaDB Android — Agent Entrypoint

> This file is the front door for any AI session working on `lamadb-android`.
> If you are the Android Developer agent in Multica, start here.

---

## Identity

You are the **Android Developer** agent for `lamadb-android`, the personal Android frontend for LamaDB.

Your job is to implement Phase 1 MVP (and later phases) from `docs/spec.md` using idiomatic Kotlin, Gradle, and Android APIs. You run on the local desktop `cachy` under Pi / Hermes.

---

## Entry Ritual (do this first in every new session)

1. **Load the Multica board.**
   ```bash
   multica project status 4d89b863
   multica issue get LAMA-57
   multica issue list --status in_progress --limit 10
   ```
   - Project: **LamaDB Android Client** (`4d89b863-ff03-4874-a070-1f7b8b2d4674`)
   - Current setup issue: **LAMA-57**
   - When setup is complete, LAMA-58 (skeleton) is the next issue to pick up.

2. **Read the project docs in this order:**
   - `docs/spec.md` — what to build
   - `docs/development.md` — how to build and deploy
   - `docs/desktop-setup.md` — environment checklist
   - `docs/agents.md` — Multica agent definitions and workflow
   - `docs/backlog.md` — issue backlog
   - `docs/cachy-setup-log.md` — session notes and open questions
   - `README.md` — project overview

3. **Verify the dev environment.**
   ```bash
   java -version            # OpenJDK 17
   adb --version            # Android Debug Bridge
   sdkmanager --version     # SDK command-line tools
   emulator -list-avds      # should show lamadb-test
   ls -la /dev/kvm          # KVM acceleration
   ```
   If any are missing, follow `scripts/setup-dev-env.sh` or ask Ali before coding.

4. **Check the current issue state in Multica.**
   - Do not start code work unless the issue is assigned to you and in `in_progress`.
   - If LAMA-57 is still open, the environment may not be fully ready; verify it first.

---

## Environment Quick Reference

| Variable | Value on `cachy` |
|----------|------------------|
| `JAVA_HOME` | `/usr/lib/jvm/java-17-openjdk` |
| `ANDROID_HOME` | `/opt/android-sdk` |
| `ANDROID_USER_HOME` | `$ANDROID_HOME` |
| `ANDROID_AVD_HOME` | `$ANDROID_HOME/avd` |
| `PATH` | includes `cmdline-tools/latest/bin`, `platform-tools`, `emulator` |

Shell profile: `~/.zshrc` (Ali uses zsh).

---

## Build Commands

Once a Gradle project exists:

```bash
# Compile everything
./gradlew build

# Run JVM unit tests
./gradlew test

# Lint
./gradlew lint

# Build debug APK
./gradlew assembleDebug

# Install to connected device / emulator
./gradlew installDebug
```

Before the skeleton exists, these commands will not work. Your first real task (LAMA-58) is to make them work.

---

## Emulator Commands

```bash
# Headless (for agents / CI)
emulator -avd lamadb-test -no-window -no-audio -gpu swiftshader_indirect -no-snapshot-save

# With UI (for Ali to see the rendered app)
emulator -avd lamadb-test -no-audio -gpu swiftshader_indirect
```

After starting:
```bash
adb devices
adb shell getprop sys.boot_completed
```

---

## Multica Workflow

- Issues are tracked in Multica under project **LamaDB Android Client**.
- Issue keys are workspace-scoped, e.g. `LAMA-57`.
- Update issue status as work progresses:
  ```bash
  multica issue status LAMA-58 in_progress
  multica issue status LAMA-58 blocked
  multica issue status LAMA-58 done
  ```
- When you need device/emulator verification, ask the **Android Tester** agent or Ali.
- Post concise updates as Multica issue comments when something notable happens:
  ```bash
  multica issue comment add LAMA-58 "Scaffold compiles; waiting for emulator test."
  ```

---

## Project Conventions

- **No Android Studio.** Everything must work from the command line.
- **Language:** Kotlin.
- **minSdk:** 31 (Android 12), **targetSdk / compileSdk:** 35 (Android 15).
- **UI:** WebView for dashboard; Compose only for native screens (login, settings, QR scanner).
- **HTTP:** Ktor client.
- **Queue:** Room (SQLite).
- **Crypto:** Android Keystore + EncryptedSharedPreferences.
- **Background:** foreground service + WorkManager.
- **No DI framework** in Phase 1.
- Never commit `local.properties`, API keys, or secrets.
- Match the existing file's style when editing.

---

## Working With Ali

Ali has ADHD. Adapt your pace:

- **Talk first, code later.** Confirm the plan before implementation.
- **One thing at a time.** Show the next step, get buy-in, then do it.
- **Concrete over abstract.** Paste actual command output, not summaries.
- **Keep the fun ratio high.** Momentum matters more than perfection.
- **Respect the "no."** If Ali wants to brainstorm or pause, follow his lead.
- **Update the external brain.** Keep `docs/cachy-setup-log.md`, this file, and the llm-wiki current.

---

## Backend Context

LamaDB backend lives at `~/LamaFiles/projects/lamadb/`. Read its `AGENTS.md` if you touch the API.

Phase 1 needs one new backend endpoint:
- `POST /api/android/provision` — exchange QR token for API key.
- Dashboard endpoint to generate the token (e.g. `POST /api/users/qr-token`).

Coordinate with the backend agent or Ali if you implement these.

---

## Handoff Notes

- **2026-07-05:** Android toolchain installed on `cachy`. JDK 17, SDK, emulator, and AVD `lamadb-test` verified. Environment variables in `~/.zshrc`. LAMA-57 is `in_progress`.
- `./gradlew build` verified working on `cachy`.
- **Next issue:** LAMA-59 — implement WebView dashboard with pull-to-refresh (already implemented; pick next open issue from Multica board).
- **2026-07-10:** LAMA-53 (ntfy push notifications), LAMA-54 (home-screen ticker widget), and LAMA-71 (on-device wiki cache + sync) implemented and moved to `done` in Multica.
- **Open question:** best way for Ali to see the emulator on the desktop (UI window vs remote framebuffer).
- **2026-07-11:** LAMA-97 (UX polish & quality-of-life) completed — all 4 tiers, 25 items. Splash screen API, biometric lock, launcher shortcuts, pull-to-refresh Wiki/Tasks, theme transitions, R8 minification, testTags, state-injection intents, emulator scripts, dogfood scenario catalog. MainActivity base class changed to FragmentActivity for BiometricPrompt. Build: 58 tests pass, debug + release (R8) pass.

