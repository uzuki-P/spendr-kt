# 05 — Amount fields: discount mode

**What to build:** optional discount entry on Add Spending, porting the
amount-field rules from the RN repo's `docs/amount-fields.md`: originalAmount
as the ceiling, discountAmount in `'fixed'` (rupiah) or `'percentage'` mode,
auto-calc of the third field from the other two, and the invariant
paidAmount = originalAmount − discountAmount with paidAmount ≤ originalAmount
and discountAmount ≥ 0. discountAmount/originalAmount are stored only when a
discount > 0 is active.

**Blocked by:** 04.

**Status:** done

- [x] Fixed and percentage discount modes with two-way auto-calc
- [x] Invariant enforced at the input layer and re-validated before persist
- [x] Fields stored only when a discount > 0 is active
- [x] Unit tests ported from the RN amount-fields cases (edge cases included)
