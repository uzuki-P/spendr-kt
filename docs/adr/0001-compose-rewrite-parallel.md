# ADR-0001: Rewrite in Kotlin + Jetpack Compose as a separate project

Status: accepted

## Context

Spendr is Android-first, leans hard on Material Design 3 dynamic color, and
already needed hand-written Kotlin Expo modules (Quick Settings tile, wallpaper
colors) because React Native could not reach those APIs. Android-only focus
removes React Native's main advantage. Rewriting in place would drag Expo
tooling along for the whole migration.

## Decision

Rewrite as a native Kotlin + Jetpack Compose app in its own repository/project
(`spendr-kt`), developed in parallel with the RN app. The RN app is frozen for
new features during the migration; bug fixes only.

## Consequences

- No bridging layer; M3 dynamic color, tile service, and background work use
  platform APIs directly.
- Two apps to maintain briefly; cutover is an explicit ticket (14), not a
  gradual drift.
- Existing RN code remains the reference for schema, behavior, and fixtures.
