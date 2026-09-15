# reference/og — OG app artifacts for emulator testing

Git-ignored (APK exceeds GitHub's 100 MB file limit; the backup contains
private spending data). See `AGENTS.md` for the full emulator workflow.

## spendr-og-live-release.apk

The OG React Native app, `com.spendr.app` ("Spendr"), live flavor,
debug-signed. Built from the OG repo:

```bash
cd ~/projects/_sandbox/spendr/android && ./gradlew :app:assembleLiveRelease
cp app/build/outputs/apk/live/release/app-live-release.apk \
   reference/og/spendr-og-live-release.apk
```

## spendr_backup.zip

A real-data backup archive exported from the OG app (Settings → Backup &
restore → Back up now). Restore it into either app for parity testing:
Settings → Backup & restore → Restore backup → pick the zip via SAF
(push it first: `adb push reference/og/spendr_backup.zip /sdcard/Download/`).

Both apps restore this file identically (1,233 transactions, 13 categories,
including 7 intentionally-kept orphan rows — see `restoreBackup` in
`app/src/main/java/com/spendr/app/kt/data/backup/BackupArchive.kt`).
