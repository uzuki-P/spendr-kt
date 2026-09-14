# 06 — Note Suggestions + QuickAdd prefill

**What to build:** the speed features. Port `note_stats` usage counters and the
suggestion ranking (RN `src/utils/suggestions.ts` behavior) to rank candidates
by past (Note, paidAmount) pairs while typing and right after Category
selection; selecting a suggestion prefills Note, Category, typical paidAmount,
Merchant, and Tags. Port QuickAdd tiles that open Add Spending prefilled.

**Blocked by:** 04.

**Status:** done

- [x] `note_stats` counters increment on save and decay/port per RN behavior
- [x] Suggestions appear while typing and after Category selection, ranked
- [x] Selecting a suggestion prefills note, category, paidAmount, merchant, tags
- [x] QuickAdd tiles exist and open Add Spending prefilled
