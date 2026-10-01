# Spendr KT

Native Android app in Kotlin and Jetpack Compose. The earlier React Native
source repository has been deleted; archived screenshots and an APK remain for
reference.

The main and dev variants install side by side:

| | Main | Dev |
| --- | --- | --- |
| Application id | `com.spendr.app.kt` | `com.spendr.app.kt.dev` |
| Launcher name | Spendr | Spendr - (dev) |
| Deep link scheme | `spendrkt` | `spendrktdev` |

Each variant has separate Android storage. Installing the dev APK does not
replace or clear the main app.

## Build

```bash
./gradlew :app:assembleMainAppDebug :app:assembleDevDebug
./gradlew :app:lintMainAppDebug :app:lintDevDebug
./gradlew :app:testMainAppDebugUnitTest :app:testDevDebugUnitTest
./gradlew :app:assembleMainAppRelease # release APK (see signing below)
```

Requires JDK 17 and an Android SDK with platform 36 (`local.properties` or
`ANDROID_HOME`).

### Signing

Release builds sign with a user-provided keystore: create `keystore.properties`
at the repo root (git-ignored) with `storeFile`, `storePassword`, `keyAlias`,
`keyPassword`; `storeFile` is resolved relative to the repo root. Without it,
release builds fall back to the debug key so `assembleMainAppRelease` still produces a
locally installable APK — do not ship that one.

### Versioning

`versionCode` increments per released build (positive integer, never reused);
`versionName` is `MAJOR.MINOR.PATCH` — MINOR bumps when the migration lands new
tickets, PATCH for fixes. The version lives in `app/build.gradle.kts`.

## Working on it

- `CONTEXT.md` — domain vocabulary. Canonical for all naming and discussion.
- `docs/plan.md` — the migration plan and the ticket index.
- `.scratch/compose-migration/issues/` — one file per ticket, with blocking
  edges and acceptance criteria. Pick a ticket whose blockers are all done.
- `docs/adr/` — architecture decision records.

## Receipt scanning

Open Home → Add receipt to choose an image or use the system camera. Configure
the API address, token, provider, model, and reasoning effort in Settings →
Receipt scanner. Empty scan configuration fields use the API's defaults.
Image controls stay disabled until an authenticated connection succeeds.
Manual receipt entry works without a network connection.

Scans use the `vision-api` job endpoint. WorkManager checks the saved job in
the background, retains its result, and sends a completion notification when
notifications are allowed. Tap it to review the receipt before saving. Android
may delay background checks. Unsaved scan results also appear in Add receipt.

The dev flavor reads `API_TOKEN` from `../vision-api/.env` during the Gradle
build and uses it as the initial scanner token. The token is embedded in the
dev APK, so keep that APK private and rotate the token if it is shared. The
main flavor starts with an empty token. Both flavors allow changing it in
scanner settings, and retain separate Android data.
