# 12 — Android entry points: QSTile + deep links

**What to build:** the outside-the-app entry points, ported from the RN app's
Kotlin modules: the Quick Settings tile (`AddSpendingTileService`) that opens
Add Spending, and deep links into the app using the `spendrkt` scheme
(ADR-0003; the RN app keeps `spendr` until cutover). Routes plug into the
Navigation Compose graph from ticket 04.

**Blocked by:** 04.

**Status:** done

- [x] QS tile toggles open Add Spending directly
- [x] `spendrkt://` deep links resolve to the right routes
- [x] Tile works when the app is not running (cold start path)
