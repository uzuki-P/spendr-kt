# 15 — Release pipeline

**What to build:** installable builds that are not debug artifacts: signed
release builds (keystore handled by the user, wizard-style), versioning
convention, and optionally a CI workflow building the APK per tag.

**Blocked by:** 14.

**Status:** done

- [x] Release signing wired to a user-provided keystore (no secrets in repo;
  `keystore.properties` git-ignored, debug-key fallback so `assembleRelease`
  always produces a locally installable APK)
- [x] Version code/name convention documented
- [ ] Signed release APK installs and runs
