# Spendr KT

Native Android rewrite of [Spendr](https://github.com/uzuki-P/spendr) in Kotlin +
Jetpack Compose. The React Native app lives at `~/projects/_sandbox/spendr` and
is **frozen for new features** while this rewrite catches up; fixes only.

Both apps install side by side:

| | RN app | this app |
| --- | --- | --- |
| Application id | `com.spendr.app` | `com.spendr.app.kt` |
| Launcher name | Spendr | spendr-kt |

At cutover (ticket 14) this app takes over the original id and name. See
`docs/adr/0003-side-by-side-identity.md`.

## Build

```bash
./gradlew :app:assembleDebug     # debug APK
./gradlew :app:lintDebug         # lint
./gradlew :app:testDebugUnitTest # JVM tests (Robolectric)
./gradlew :app:assembleRelease   # release APK (see signing below)
```

Requires JDK 17 and an Android SDK with platform 36 (`local.properties` or
`ANDROID_HOME`).

### Signing

Release builds sign with a user-provided keystore: create `keystore.properties`
at the repo root (git-ignored) with `storeFile`, `storePassword`, `keyAlias`,
`keyPassword`; `storeFile` is resolved relative to the repo root. Without it,
release builds fall back to the debug key so `assembleRelease` still produces a
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
