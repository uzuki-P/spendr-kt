# AGENTS.md

Guidance for AI agents (and humans) working in this repository.

## What this project is

Spendr KT is a **native Android rewrite (Kotlin + Jetpack Compose)** of the
original Spendr app — the "OG" — which is a React Native / Expo app kept at
`~/projects/_sandbox/spendr`. The OG is **frozen for new features**; it exists
as the reference implementation this port copies from.

- **OG repo (read-only reference):** `~/projects/_sandbox/spendr`
  - Schema + repository SQL: `src/db/migrations.ts`, `src/db/repositories/`
  - Amount-field rules (canonical spec): `docs/amount-fields.md`
  - Design system: `src/theme/`, `src/components/`
  - Screens: `src/features/{home,add,transactions,reports,settings}/`
- **This repo:** the Kotlin port. When implementing or changing behavior,
  **port the OG's logic verbatim** — same SQL, same business rules, same edge
  cases. Read the OG source before inventing behavior. `CONTEXT.md` is the
  domain vocabulary; `docs/plan.md` holds the migration plan and ticket index.

Both apps install side by side: OG is `com.spendr.app` ("Spendr"), this app is
`com.spendr.app.kt` ("spendr-kt"). At cutover (ticket 14) this app takes over
the OG identity — see `docs/adr/0003-side-by-side-identity.md`.

## Reference material in this repo

| Path | What it is |
| --- | --- |
| `docs/screenshots/og/` | OG app screenshots (user's device, dark theme, real data) — the visual ground truth |
| `docs/screenshots/spendr-kt/` | Current spendr-kt screens (emulator, restored test data) |
| `docs/screenshots/README.md` | What each screenshot shows and what to compare |
| `reference/og/spendr-og-live-release.apk` | Installable OG APK for emulator testing (git-ignored, see below) |
| `reference/og/spendr_backup.zip` | Real-data backup archive for restore testing (git-ignored, private data) |

The OG APK and backup zip are **git-ignored** (the APK is ~117 MB, over
GitHub's file limit; the backup contains private data). If missing, recreate:

```bash
# OG APK — build from the OG repo (live flavor, debug-signed):
cd ~/projects/_sandbox/spendr/android && ./gradlew :app:assembleLiveRelease
cp app/build/outputs/apk/live/release/app-live-release.apk \
   ~/projects/_sandbox/spendr-kt/reference/og/spendr-og-live-release.apk

# Test backup — export from the OG app: Settings → Backup & restore →
# Back up now (needs a backup folder), or grab an existing export.
```

## Emulator testing workflow

A headless AVD `Spendr_Headless_API_36` exists on this machine.

```bash
# Boot (takes ~1 min):
~/Android/Sdk/emulator/emulator -avd Spendr_Headless_API_36 -no-window \
  -no-audio -no-boot-anim -gpu swiftshader_indirect &
~/Android/Sdk/platform-tools/adb wait-for-device shell \
  'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'

# Install both apps:
adb install -r app/build/outputs/apk/debug/app-debug.apk          # spendr-kt
adb install -r reference/og/spendr-og-live-release.apk            # OG Spendr

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
- Room enforces foreign keys; the OG's connection did not, so OG databases can
  contain orphan rows. `restoreBackup` lifts FK enforcement around the restore
  for this reason — do not "fix" that away.
- The OG's restore connection also ran with FKs off; both apps replay backups
  verbatim instead of failing.

## Validation

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

All three must pass before finishing a ticket. JVM tests use Robolectric
(`app/src/test`); Room schema JSON is exported to `app/schemas/`.

## Conventions

- Domain vocabulary: `CONTEXT.md` (Transaction/Spending/paidAmount/... — do
  not say "expense" for a record).
- Design tokens and component grammar are ported from the OG `src/theme/` —
  when adding UI, find the OG component first and copy geometry, colors, and
  copy text verbatim. `docs/screenshots/` is the visual acceptance reference.
- Versioning: `versionCode` increments per released build; `versionName` is
  `MAJOR.MINOR.PATCH` (see README).
