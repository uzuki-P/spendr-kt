# AGENTS.md

Guidance for AI agents (and humans) working in this repository.

## What this project is

Spendr KT is a native Android app built with Kotlin and Jetpack Compose. The
React Native source repository has been deleted. Use `CONTEXT.md` for domain
vocabulary, the code and tests for current behavior, and `docs/screenshots/og/`
for historical visual reference. `docs/plan.md` records the migration history.

The main app uses `com.spendr.app.kt`. The dev flavor uses
`com.spendr.app.kt.dev` and keeps its own Android data. Build it with
`./gradlew :app:assembleDevDebug`.

## Reference material in this repo

| Path | What it is |
| --- | --- |
| `docs/screenshots/og/` | Historical React Native screenshots for visual comparison |
| `docs/screenshots/spendr-kt/` | Current spendr-kt screens (emulator, restored test data) |
| `docs/screenshots/README.md` | What each screenshot shows and what to compare |
| `reference/og/spendr-og-live-release.apk` | Archived React Native APK for emulator comparison (git-ignored) |
| `reference/og/spendr_backup.zip` | Private backup archive for restore testing (git-ignored) |

The archived APK and backup zip are private local artifacts. The deleted React
Native source cannot recreate the APK; use the archive when it is present.

## Emulator testing workflow

A headless AVD `Spendr_Headless_API_36` exists on this machine.

```bash
# Boot (takes ~1 min):
~/Android/Sdk/emulator/emulator -avd Spendr_Headless_API_36 -no-window \
  -no-audio -no-boot-anim -gpu swiftshader_indirect &
~/Android/Sdk/platform-tools/adb wait-for-device shell \
  'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'

# Install the main and dev variants side by side:
adb install -r app/build/outputs/apk/mainApp/debug/app-mainApp-debug.apk
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk

# Import test data into either app: push the backup, then use the app's
# Settings → Backup & restore → Restore backup and pick the file in the
# system picker (navigate: hamburger → Downloads → the zip):
adb push reference/og/spendr_backup.zip /sdcard/Download/

# Screen capture (screenshots are 760x1999; the display is 945x2400 —
# scale tap coordinates by ~1.243 x / ~1.201 y from screenshots):
adb exec-out screencap -p > screen.png
adb shell uiautomator dump /sdcard/ui.xml   # then grep bounds for taps

# Shut down when done:
adb emu kill
```

Handy facts learned the hard way:

- The first app launch seeds the DB **asynchronously** — give it a few seconds
  or force-stop + relaunch before concluding data is missing.
- Reinstalling the APK resets navigation to Home; re-navigate after install.
- Room enforces foreign keys. Historical backups can contain orphan rows, so
  `restoreBackup` lifts FK enforcement while replaying them.

## Validation

```bash
./gradlew :app:assembleMainAppDebug :app:assembleDevDebug :app:lintMainAppDebug :app:lintDevDebug :app:testMainAppDebugUnitTest :app:testDevDebugUnitTest
```

All three must pass before finishing a ticket. JVM tests use Robolectric
(`app/src/test`); Room schema JSON is exported to `app/schemas/`.

## Conventions

- Domain vocabulary: `CONTEXT.md` (Transaction/Spending/paidAmount/... — do
  not say "expense" for a record).
- For visual changes, compare the archived screenshots in `docs/screenshots/`.
- Versioning: `versionCode` increments per released build; `versionName` is
  `MAJOR.MINOR.PATCH` (see README).
