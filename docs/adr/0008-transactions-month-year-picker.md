# ADR-0008: Month/year picker on the Transactions page

Status: accepted

## Context

The Transactions page shipped with a 13-month tab pager (current month plus 12
back). Older months are reachable only by editing data or waiting. Reports
already has a calendar-pill in the top app bar opening the RN
`MonthYearPicker` sheet, but Transactions had no equivalent, so the two
month-scoped screens aged differently. This picker does not exist in the OG
app's Transactions screen; it is a spendr-kt addition.

## Decision

Add the same Reports-style month/year pill to the Transactions top app bar.
The existing `MonthYearPickerSheet` moves to shared components and is reused
verbatim by both screens. The month-tab window (`monthWindow`) gains an
`include` parameter: a selected month older than the 12-month window extends
the tabs backwards so the active tab always exists and stays highlighted.

While restructuring the page, the app bar, month tabs, and the search + filter
row are pinned; only the summary card and the transaction list scroll.

## Consequences

- Transactions and Reports share one picker implementation; behavior changes
  (e.g. future months stay disabled) land in both places at once.
- Selecting a very old month grows the tab list; the tabs already
  auto-center on selection, so long lists remain navigable.
- No OG parity break: the OG Transactions screen has no picker to diverge
  from; existing tabs, swipe-paging, and default ranges are unchanged.
