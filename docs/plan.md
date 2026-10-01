# Migration plan: Spendr (RN) → Spendr KT (Compose)

Spendr is being rewritten as a native Android app in Kotlin + Jetpack Compose.
The React Native source repository has been deleted. This file records the
original migration plan; current behavior lives in this repository's code and
tests. Archived screenshots and an APK remain for comparison.

## Strategy

- **Parallel rewrite, not in-place.** Spendr KT is a separate project. Both
  install side by side until cutover.
- **Domain-first.** The data layer (schema, repositories, import/export,
  backup) ports early with tests against the RN fixtures; screens follow.
- **Cutover via backup archive.** Real data moves by exporting a backup from
  the RN app and restoring it here. The SQLite file itself is not copied, so
  the RN migration history does not have to be carried over.
- **Tickets are tracer bullets.** Each one cuts a vertical path (data → UI →
  build) and is demoable on its own. Work the frontier: any ticket whose
  blockers are all closed.

## Ticket index

Blocked-by references ticket numbers in
`.scratch/compose-migration/issues/`.

| # | Ticket | Blocked by | Status |
| --- | --- | --- | --- |
| 01 | Compose app skeleton + architecture ADRs | — | done |
| 02 | Read-only Transactions list on the real schema | 01 | done |
| 03 | Import/export + backup port | 02 | done |
| 04 | Add Spending, basic flow | 02 | done |
| 05 | Amount fields: discount mode | 04 | done |
| 06 | Note Suggestions + QuickAdd prefill | 04 | done |
| 07 | Home screen | 04, 06 | done |
| 08 | Transactions polish: detail, edit, duplicate, filters | 02 | done |
| 09 | Reports | 02 | done |
| 10 | Category manage | 02 | done |
| 11 | Settings + theming | 02 | done |
| 12 | Android entry points: QSTile + deep links | 04 | done |
| 13 | Debug screen + dev tooling parity | 02 | done |
| 14 | Data cutover + RN retirement | 03–13 | blocked-on-user |
| 15 | Release pipeline | 14 | done |

## Working a ticket from a fresh thread

1. Read `CONTEXT.md` for vocabulary, then the ticket file and the ADRs it
   touches.
2. Implement the ticket end to end. Keep slices vertical; do not build ahead
   for tickets that come later.
3. Validate both flavors with the commands in `AGENTS.md`. Installing on
   a device or emulator is the user's call, not the agent's.
4. Tick the acceptance boxes in the ticket file and update the Status line.

## Done when

Ticket 14 closes: the RN app is uninstalled, this app holds the real data and
the original application id, and no Expo/RN code remains to maintain.
