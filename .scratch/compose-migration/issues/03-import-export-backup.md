# 03 — Import/export + backup port

**What to build:** the data bridge from the RN app. Port the spendr CSV
export/import, the MoneyLover CSV import, and the backup archive create/restore
flows, using the Storage Access Framework for file picking and sharing (replaces
expo-document-picker / expo-sharing). Port the RN repo's CSV tests and fixtures
(the RN repo at `~/projects/_sandbox/spendr` is the fixture source). This is
the cutover mechanism, so it lands before UI parity work peaks.

**Blocked by:** 02.

**Status:** done

- [x] Export a spendr CSV of all Transactions via SAF
- [x] Import a spendr CSV, preserving paidAmount / originalAmount /
      discountAmount / discountType / Category / Merchant / Note
- [x] Import a MoneyLover CSV
- [x] Restore a backup archive produced by the RN app into this app
- [x] Produce a backup archive this app (and ideally the RN app) can restore
- [x] CSV parsers and archive logic covered by unit tests against the ported
      fixtures
