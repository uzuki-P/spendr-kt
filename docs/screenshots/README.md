# Screenshots

Reference screenshots for visual-parity work. `og/` is the ground truth — the
React Native app this project ports (captured on the user's device, dark
theme, custom green seed, real data). `spendr-kt/` is this app (emulator,
default pink seed, the test backup restored). When colors differ, it is the
seed/theme — compare **layout, geometry, spacing, and copy**, not hues.

The restored test data behind the spendr-kt shots: Rp 1.153.568 through Sep 14,
3-mo avg Rp 1.063.783, rows "Tangzu waner SG / Sniper elite 5 / 3 AON 26GB /
Habibi / Bakso / Americano". If your build shows different numbers, the backup
was not restored (see `AGENTS.md` → emulator workflow).

## og/ — the reference implementation

| File | Screen | What to study |
| --- | --- | --- |
| `home.jpg` | Home | Custom header ("Spendr" + search/settings circles), pace card (primaryContainer header, dashed 3-mo avg, gradient area fill, today marker), recent rows (42dp category circles, amount in primary), "Add +" FAB |
| `reports.jpg` | Reports | Top bar + "Sep 2026" month pill, month tabs with 3dp indicator, hero card with hairline-divided stats (label above value), Daily trend bar chart with selection header, "Rp 290rb"-style axis labels |
| `reports-month-picker.jpg` | Month picker sheet | ‹ year › chevrons, "THIS MONTH" filled button, text-only month tiles, selected month as filled pill, future months muted |
| `reports-category-breakdown.jpg` | Reports (scrolled) | Category share-bar rows: icon, name, amount in primary, 6dp colored bar, "24.7% · 1x" caption |
| `transactions.jpg` | Transactions | Back arrow, tabs, elevated summary card ("Total spending", receipt icon + "1 transaction", "Full report"), search pill, day-group headers with per-day totals |
| `transactions-scroll-badge.jpg` | Transactions (scrolling) | Floating day badge: primaryContainer pill, "Mon, 14 Sep", left edge at ~42% height, visible while scrolling only |
| `add-spending.jpg` | Add Spending | Labels ABOVE fields (Date/Note/Merchant (optional)), note placeholder "e.g., Premium coffee", category field with shape icon, keypad pinned above footer, "Next" in grid corner |
| `add-spending-discount.jpg` | Add Spending (discount) | "Amount and discount" header + red "Remove discount" link, Original/Discount/You pay stacked boxes, Rp/% toggle inside the Discount box, active field gets primary border |

## spendr-kt/ — this app, current state

| File | Screen | Notes |
| --- | --- | --- |
| `home.png` | Home | Restored test data loaded; compare against `og/home.jpg` |
| `transactions.png` | Transactions | Day-grouped list + summary card; compare against `og/transactions.jpg` |
| `reports.png` | Reports | Hero + daily trend; compare against `og/reports.jpg` |
| `reports-month-picker.png` | Month picker | Compare against `og/reports-month-picker.jpg` |
| `add-spending.png` | Add Spending | Plain mode; compare against `og/add-spending.jpg` |
| `add-spending-discount.png` | Add Spending (discount) | Compare against `og/add-spending-discount.jpg` |
| `settings.png` | Settings | No og capture — sections ported from `src/features/settings/SettingsScreen.tsx` |

## Refreshing these

Rebuild + reinstall + restore on the emulator, then:

```bash
adb exec-out screencap -p > docs/screenshots/spendr-kt/<screen>.png
```

The test data comes from `reference/og/spendr_backup.zip` (restore via
Settings → Backup & restore). See `AGENTS.md` for the full emulator workflow.
