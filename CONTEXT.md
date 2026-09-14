# Ubiquitous Language

Copied verbatim from the RN app (`~/projects/_sandbox/spendr/CONTEXT.md`),
which remains the historical source while the migration runs. Spendr is a **spending-only**
personal tracker: no income, no balance, no wallet, no bank sync. The terms below
are the canonical vocabulary for product, design, and code discussion.

## The spending record

| Term | Definition | Aliases to avoid |
| --- | --- | --- |
| **Transaction** | A single recorded spending event — the core entity. | expense, purchase, entry, "spending record" |
| **Spending** | The aggregate activity being tracked ("my spending this month"). Use **Transaction** for an individual record, never "a spending". | (as a singular record) |
| **paidAmount** | The actual money that left the wallet. The authoritative recorded amount; drives all spending reports. | amount, price, cost, "pay"¹ |
| **originalAmount** | The list price before any discount. Acts as the ceiling on **paidAmount** (`paidAmount ≤ originalAmount`). | full price, sticker price |
| **discountAmount** | The savings captured on the transaction. Only stored when `> 0`. | "discount", markdown, savings-on-this-tx |
| **discountType** | How the discount was entered: `'fixed'` (rupiah) or `'percentage'`. | — |
| **Note** | A free-text label the user gives a **Transaction** (e.g. "Kopi premium"). Powers **Note Suggestions**. | description, memo, title |
| **Merchant** | The store/place of a **Transaction**, stored as free text on the row and upserted into the `merchants` table. | store, shop, vendor |
| **Tags** | (Planned) free labels on a **Transaction**. Column exists; no input UI yet. | labels |
| **createdAt** | When a **Transaction** was first saved. This is lifecycle metadata, distinct from the user-selected transaction date. | created date |
| **updatedAt** | When a **Transaction** was most recently saved. This is lifecycle metadata, distinct from the user-selected transaction date. | modified date |

¹ "pay" is acceptable as shorthand inside `amount-fields.md` and the draft
reducer, but in discussion prefer **paidAmount**.

Invariant (see `docs/amount-fields.md`):

```
paidAmount = originalAmount − discountAmount
→ paidAmount ≤ originalAmount, discountAmount ≥ 0
```

Surcharge (`paidAmount > originalAmount`, e.g. tips/tax/surge) is **intentionally
not modeled** — fold it into paidAmount manually.

## Organizing records

| Term | Definition | Aliases to avoid |
| --- | --- | --- |
| **Category** | A user-managed classification for a **Transaction** (Food, Snack & Coffee, Transport, …). Has icon + color + sort order. Before a referenced Category is deleted, its Transactions and QuickAdds must be reassigned to another Category. | group, bucket, tag² |
| **Merchant** (entity) | The deduplicated `merchants` table row (unique name) kept in sync with the **Merchant** field on transactions. No management UI yet. | — |
| **QuickAdd** | A Home-screen tile that opens Add Spending prefilled (category, note, paidAmount, merchant, tags). | quick-add item, shortcut, favorite |

² "Tag" means the **Tags** field on a transaction, not a **Category**.

## Fast entry

| Term | Definition | Aliases to avoid |
| --- | --- | --- |
| **Note Suggestion** | A ranked autocomplete candidate derived from past transactions, shown while typing **and** immediately after **Category** selection. Ranked by `utils/suggestions.ts`. | suggestion, recommendation, hint |
| **note_stats** | Denormalized per-`(note, paidAmount)` usage counters that back **Note Suggestions**. Infrastructure, not a user-facing term. | — |
| **Duplicate** | An action that copies a **Transaction** with `date = now`; available in Add Spending and the Transactions long-press menu. | clone, copy |

## Reporting

| Term | Definition | Aliases to avoid |
| --- | --- | --- |
| **Total** (Monthly total) | Sum of **paidAmount** over a date range, with transaction count. | sum, expense total |
| **Savings** | Sum of **discountAmount** across transactions. Reported separately from spending. **Never** means money set aside. | discounts, "savings account" |
| **Category totals** | Per-**Category** sum + count of **paidAmount**, ordered by total desc. | breakdown |
| **Daily totals** | Per-day sum of **paidAmount** within a range; used for the trend chart. | — |
| **Pace** | Month-to-date cumulative **paidAmount** plotted against the 3-month average on the Home `SpendingPaceCard`. | burn rate |

## App configuration

| Term | Definition | Aliases to avoid |
| --- | --- | --- |
| **ThemeMode** | User's theme preference: `'system'` \| `'light'` \| `'dark'`. Persisted in `settingsStore`. | theme, appearance |
| **ColorSource** | Where the M3 dynamic-color seed comes from: `'default'` (Pink 500), `'user'` (hex), or `'wallpaper'` (Android 12+ Material You). | seed, palette source |

## Relationships

- A **Transaction** belongs to exactly one **Category**.
- A **Transaction** optionally names one **Merchant** (free text, upserted).
- A **Transaction** always has a **paidAmount**; **originalAmount** and
  **discountAmount** are stored **only when a discount > 0 is active**.
- **Savings** is derived from **discountAmount** across **Transactions**; it is
  never income and never a balance.
- A **QuickAdd** references one **Category** and prefills a future **Transaction**.
- **Note Suggestions** are derived from past `(note, paidAmount)` pairs via
  `note_stats`; selecting one prefills note, category, typical paidAmount,
  merchant, and tags.

## Explicitly out of scope

These are **not** Spendr domain terms and should not creep into vocabulary:
income, balance, wallet, bank sync, debt, loan, net worth, opening/ending
balance. (Source: `plan.md` "Important product decisions".)

## Example dialogue

> **Dev:** "When a user adds an **expense** for Rp 40k with an original price of Rp 50k, where does the Rp 10k go?"
>
> **Domain expert:** "First — call it a **Transaction**, not an 'expense'. The Rp 40k is the **paidAmount** (that's what the reports sum). The Rp 50k is the **originalAmount**, and the Rp 10k difference is the **discountAmount**."
>
> **Dev:** "So the **Savings** report counts that Rp 10k?"
>
> **Domain expert:** "Exactly. **Savings** is just the sum of **discountAmount** — it has nothing to do with income or a savings account. We don't track those."
>
> **Dev:** "And if the **Merchant** is 'Indomaret', is that linked to the **Category**?"
>
> **Domain expert:** "No — a **Transaction** belongs to one **Category** (say, Shopping), and separately names a **Merchant**. The **Merchant** is free text on the row; we also upsert it into the `merchants` table for reuse, but there's no management UI yet."
>
> **Dev:** "What if they tap a **QuickAdd** tile for 'Kopi'?"
>
> **Domain expert:** "That opens Add Spending prefilled — a **Category**, **Note**, typical **paidAmount**, and **Merchant** — ready to save as a new **Transaction** in under five seconds."

## Flagged ambiguities

- **"expense" vs "spending" vs "transaction" vs "entry".** The PRD and UI say
  "spending"/"expense" (e.g. "expense numbers", "expense highlights");
  `amount-fields.md` says "entry"; the data model and code say **Transaction**.
  **Canonical: Transaction** for the record, **Spending** for the aggregate
  domain. Avoid "expense", "purchase", and "entry" as record synonyms — they
  drift meaning and make search/grep harder.
- **Unqualified "amount".** Could mean paid, original, or discount. **Canonical:
  the default "amount" is paidAmount**; always qualify original/discount
  explicitly.
- **"savings".** In general finance this means money put aside; in Spendr it
  means **discounts captured** (sum of discountAmount) and is reported
  separately from spending. Never conflate with income or a balance.
- **"discount" the field vs "discount" the feature.** "discountAmount" is a
  column; "the discount" / "Add discount" is the optional input mode on Add
  Spending. Context usually disambiguates, but prefer **discountAmount** for the
  value.
- **Merchant field vs Merchant entity.** Same domain concept (a store); stored
  redundantly — as free text on each **Transaction** and as a deduplicated row
  in `merchants`. They are kept in sync on save. No need for separate vocabulary;
  just be aware they differ at the data layer.
