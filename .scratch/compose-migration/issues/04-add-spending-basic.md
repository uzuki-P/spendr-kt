# 04 — Add Spending, basic flow

**What to build:** the core "add a Transaction in under five seconds" flow:
a keypad for paidAmount, plus Category, Note, Merchant, and date inputs, saving
a Transaction row. This is the product's reason to exist; optimize for speed of
entry, not feature completeness — discount mode (05) and suggestions (06) come
next.

**Blocked by:** 02.

**Status:** done

- [x] Keypad entry for paidAmount
- [x] Category picker; Merchant and Note as free text
- [x] Date defaults to now, user-adjustable
- [x] Save persists a Transaction via Room and returns to the previous screen
- [x] Add route is pushable over other screens (ADR-0007 route exists)
