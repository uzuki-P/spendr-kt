# ADR-0006: Material 3 dynamic color now, ColorSource settings later

Status: accepted

## Context

The RN app resolves the M3 seed from a user setting: `'default'` (Pink 500),
`'user'` (hex), or `'wallpaper'` (Material You via a custom native module).
The theme itself is dynamic-color based (see the RN repo's `docs/theming.md`).

## Decision

The skeleton always uses `dynamicColorScheme()` on API 31+ (the wallpaper
path, previously the expensive custom module) and the framework fallback
scheme below 31. `SeedPink` (Pink 500, the RN default seed) is kept as a
constant for the `'default'` and `'user'` modes. ThemeMode and ColorSource
settings port with ticket 11.

## Consequences

- Dynamic color costs one API call instead of a native module.
- Theme tokens are Compose `MaterialTheme` from day one; do not recreate the
  RN token file, port only what screens need, when they need it.
