# ADR-0002: Single module, no product flavors

Status: accepted

## Context

The RN app had no build flavors and the user does not want a dev flavor in the
rewrite. Personal-scale project; flavor matrix adds Gradle complexity for no
gain.

## Decision

One Gradle module (`:app`) and no product flavors. Standard `debug` and
`release` build types only. Module splits (e.g. a `:core-data` module) can be
revisited if the project outgrows a single module, but the default is to stay
single-module until something forces the split.

## Consequences

- Simpler build config and faster builds.
- Package structure inside `:app` carries the layering (data / ui / domain)
  until a module split is justified.
