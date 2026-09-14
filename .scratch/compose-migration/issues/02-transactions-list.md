# 02 — Read-only Transactions list on the real schema

**What to build:** the first vertical path through the new stack. Port the
SQLite schema to Room (ADR-0004) — `transactions`, `categories`, `merchants`,
`quick_add`, `note_stats` — verbatim from the RN app's `docs/database.md`, and
show a Transactions screen listing saved Transactions newest-first: paidAmount,
Note, Category, date. Data can come from a debug seed for now; ticket 03 brings
the real-data path.

**Blocked by:** 01.

**Status:** done

- [x] Room entities + DAOs cover all five tables with column-for-column parity
- [x] Transactions screen renders real rows from Room, newest Transaction first
- [x] DAO round-trip test passes (insert → query → assert paidAmount/discount fields)
- [x] Navigation Compose `NavHost` introduced (ADR-0007)
- [x] `./gradlew :app:assembleDebug :app:testDebugUnitTest` green
