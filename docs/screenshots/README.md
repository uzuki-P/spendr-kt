# Screenshots

Reference screenshots for visual-parity work. `og/` and `spendr-kt/` are
mirrored 1:1 — same 17 screens, same emulator (`Spendr_Headless_API_36`),
same restored test data, light theme, default pink seed. `og/` is the ground
truth (the React Native app this project ports); compare the folders side by
side, screen for screen.

The restored test data behind both sets: Rp 1.153.568 through Sep 14,
3-mo avg Rp 1.063.783, rows "Tangzu waner SG / Sniper elite 5 / 3 AON 26GB /
Habibi / Bakso / Americano". If your build shows different numbers, the backup
was not restored (see `AGENTS.md` → emulator workflow).

Files named `og-*-user-device.jpg` are earlier captures from the user's own
device (dark theme, custom green seed), kept only as hue reference for dark
mode.

## Screen inventory (both folders)

| File | Screen | What to study |
| --- | --- | --- |
| `home.png` | Home | Header, pace card + chart, recent rows, "Add +" FAB |
| `transactions.png` | Transactions | Summary card, search pill + filter icon, month tabs, day headers |
| `transactions-scroll-badge.png` | Transactions (scrolling) | Floating day badge at left, ~42% height, visible while scrolling |
| `transaction-detail.png` | Transaction detail | Hero card (category + paid amount), details rows, record history, Duplicate/Edit footer |
| `transaction-filter.png` | Sort & filter dialog | Pill sort tiles + caption, category field, discount checkbox, Cancel/Apply |
| `transaction-filter-categories.png` | Multi-select categories sheet | Search pill, 3-column tiles, "All categories" tile, count + Done |
| `reports.png` | Reports | Hero card + stats, daily trend chart with y-axis labels |
| `reports-category-breakdown.png` | Reports (scrolled) | Category share-bar rows with caption below the bar |
| `reports-month-picker.png` | Month picker sheet | ‹ year › chevrons, THIS MONTH pill, month grid, selected pill, muted future months |
| `reports-month-swiped.png` | Reports (after swipe) | Month pager swipe landed on "Aug 2026" |
| `add-spending.png` | Add Spending | Amount card, Date/Category/Note, suggestions, Save above keypad, keypad safe area |
| `add-spending-discount.png` | Add Spending (discount) | Original/Discount/You pay, Rp/% toggle, Remove discount |
| `add-spending-calendar.png` | Date calendar sheet | Month header + chevrons, weekday row, day grid, TODAY pill |
| `add-spending-category-picker.png` | Category picker sheet | Drag handle, title, search pill, 3-column grid, recents first, Manage categories |
| `settings.png` | Settings | Uppercase group labels, theme tiles, compact rows with chevrons |
| `settings-backup-restore.png` | Backup & restore | Folder/automatic/rotation rows, back up now, CSV transfer |
| `settings-vibration-dialog.png` | Vibration strength dialog | Radio options with ms values, custom slider |

## Refreshing these

Rebuild + reinstall + restore the backup on the emulator, then:

```bash
adb exec-out screencap -p > docs/screenshots/spendr-kt/<screen>.png
```

Capture the `og/` set the same way against `com.spendr.app` (install
`reference/og/spendr-og-live-release.apk`, restore the same backup). Verify
each screen with a `uiautomator dump` text assertion before saving — it is
easy to capture the previous screen during a transition.
