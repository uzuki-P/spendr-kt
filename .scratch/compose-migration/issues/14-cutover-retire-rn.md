# 14 — Data cutover + RN retirement

**What to build:** the flip. Run a side-by-side parity checklist against the
RN app on a real device, move real data via the backup-archive path
(export from RN → restore here), then take over the original identity per
ADR-0003: applicationId `com.spendr.app`, name "Spendr", scheme `spendr`.
Uninstall the RN app. No Expo/RN code remains to maintain afterwards.

**Blocked by:** 03, 04, 05, 06, 07, 08, 09, 10, 11, 12, 13.

**Status:** blocked-on-user (all code-side work done; on-device steps remain)

- [ ] Parity checklist walked on device (add, suggestions, reports, backup,
      tile) with the user
- [ ] Real data restored and spot-checked against the RN app's reports
- [ ] Identity flip landed: id, name, scheme
- [ ] RN app uninstalled; this repo's README side-by-side table updated
