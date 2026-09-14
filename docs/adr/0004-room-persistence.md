# ADR-0004: Room for persistence, schema ported verbatim

Status: accepted

## Context

The RN app stores data in SQLite (tables `transactions`, `categories`,
`merchants`, `quick_add`, plus the `note_stats` counter table) through
hand-written migrations and repository functions. The rewrite needs the same
schema so backup archives stay compatible. Candidates: Room, SQLDelight,
raw SQLite.

## Decision

Use Room (introduced with ticket 02). Entities map 1:1 onto the existing
tables and columns; no schema redesign during the port. The RN app's
`docs/database.md` is the schema reference.

## Consequences

- Compile-time query verification and DAO-level tests without a device.
- Backup-archive compatibility is straightforward: same tables, same columns.
- Schema evolution restarts here: the new app's baseline is the RN app's final
  schema, and no RN migration history is replayed.
