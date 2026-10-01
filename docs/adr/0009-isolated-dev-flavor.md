# ADR-0009: Isolated dev flavor

Status: accepted

## Context

The main Android app contains user data. Testing new behavior needs a separate
installation. The earlier no-flavors decision in ADR-0002 predates this need.

## Decision

Keep one module and add `mainApp` and `dev` product flavors. The main flavor
keeps `com.spendr.app.kt`, the Spendr launcher name, icon, and `spendrkt` deep
link. The dev flavor uses `com.spendr.app.kt.dev`, the `Spendr - (dev)` name, a
large DEV badge on the launcher icon, and the `spendrktdev` deep link. Its
shortcut points to the dev package.

## Consequences

Android stores the two variants' databases, preferences, and backups under
different packages. Build, lint, and test tasks now name a flavor.
