# 11 — Settings + theming

**What to build:** configuration parity. ThemeMode (`'system'` | `'light'` |
`'dark'`) and ColorSource (`'default'` Pink 500 seed | `'user'` hex |
`'wallpaper'` Material You) persisted across launches, replacing the skeleton's
always-dynamic behavior (ADR-0006). Wallpaper mode is a direct
`dynamicColorScheme()` call; no custom module needed. Include the backup &
restore entry point if the UI needs a home before ticket 03's screen lands.

**Blocked by:** 02.

**Status:** done

- [x] ThemeMode applies immediately and persists
- [x] ColorSource default/user/wallpaper all resolve a color scheme correctly
- [x] User hex input produces a full M3 scheme (seeded palette)
- [x] Settings persist across process death
