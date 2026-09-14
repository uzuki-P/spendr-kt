# 09 — Reports

**What to build:** reporting parity: monthly Total (sum of paidAmount over a
range with Transaction count), Savings (sum of discountAmount — discounts
captured, never income or a balance), Category totals (per-Category sum + count
of paidAmount, ordered by total desc), and Daily totals for the trend chart.

**Blocked by:** 02.

**Status:** done

- [x] Range selection (month and custom range)
- [x] Total + count; Savings reported separately from spending
- [x] Category totals ordered by total desc
- [x] Daily trend chart drawn with Compose (no chart library unless justified)
- [x] Aggregations covered by DAO/unit tests against seeded rows
