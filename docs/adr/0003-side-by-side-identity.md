# ADR-0003: Side-by-side identity, original ids return at cutover

Status: accepted

## Context

The rewrite must install next to the RN app for day-to-day parity checking and
real-data rehearsal. The RN app holds `com.spendr.app` / "Spendr" and the
`spendr` deep-link scheme.

## Decision

During the migration this app uses applicationId `com.spendr.app.kt`, launcher
name "Spendr KT", and (from ticket 12) the deep-link scheme `spendrkt`. It does
not claim the `spendr` scheme while side by side.

At ticket 14 (cutover), this app takes over `com.spendr.app`, the "Spendr"
name, and the `spendr` scheme, and the RN app is uninstalled. Changing the
applicationId at cutover is safe precisely because data moved in via the
backup-archive import, not via Android's preserve-package upgrade path.

## Consequences

- Both launchers coexist unambiguously.
- Cutover includes an id/name/scheme flip; the checklist lives in ticket 14.
