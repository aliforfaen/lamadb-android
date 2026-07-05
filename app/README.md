# LamaDB Android — `app` module

This is the single Android application module for the LamaDB client.

## Build

From the project root:

```bash
./gradlew build
./gradlew assembleDebug
```

## Test

```bash
./gradlew test
./gradlew lint
```

## Install and run

With the `lamadb-test` emulator running (or a device connected via ADB):

```bash
./gradlew installDebug
adb shell am start -n "com.lamadb.android/.MainActivity"
```

Watch logs:

```bash
adb logcat -s LamaDB:D AndroidRuntime:E
```

## Notes

- `minSdk = 31`, `targetSdk = 35`, `compileSdk = 35`
- UI is Jetpack Compose + Material3.
- `local.properties` is required at the project root and must contain `sdk.dir=/opt/android-sdk`. It is ignored by Git.
