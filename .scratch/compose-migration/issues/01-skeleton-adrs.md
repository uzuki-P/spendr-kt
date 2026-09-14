# 01 — Compose app skeleton + architecture ADRs

**What to build:** a runnable native Kotlin + Jetpack Compose app with the
build baseline in place: Gradle version catalog, M3 dynamic-color theme
(ADR-0006), edge-to-edge single activity, adaptive launcher icon. Recorded
ADRs for the decisions the rewrite will lean on.

**Blocked by:** None.

**Status:** done — delivered by the initial skeleton.

- [x] `./gradlew :app:assembleDebug` produces an installable APK
- [x] Application id `com.spendr.app.kt`, launcher name "Spendr KT" (ADR-0003)
- [x] Single module, no product flavors (ADR-0002)
- [x] Theme uses `dynamicColorScheme()` on API 31+ (ADR-0006)
- [x] ADRs 0001–0007 written

Delivered by the initial skeleton. Deviations from the original draft:
navigation (ADR-0007) and DI (ADR-0005) are decided but not wired, since the
skeleton is a single static screen.
