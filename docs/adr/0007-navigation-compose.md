# ADR-0007: Navigation Compose from ticket 02

Status: accepted

## Context

The skeleton is a single screen. Ticket 02 adds the first real screen and
ticket 04 the Add flow, at which point back-stack behavior (Add is pushed over
Home/Transactions; QuickAdds deep-link into Add) starts to matter.

## Decision

Use androidx Navigation Compose. Introduce the `NavHost` with ticket 02, with
routes mirroring the RN app's stack (Home, Add, Transactions, Reports,
Settings) as those screens land.

## Consequences

- Type-safe route definitions can grow per ticket; no up-front navigation
  graph to guess.
- Deep links (ticket 12) plug into the same route definitions.
