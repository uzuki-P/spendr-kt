# ADR-0005: Manual dependency injection

Status: accepted

## Context

The app is small and single-module. Hilt brings annotation processing, a
codegen plugin, and convention overhead; the dependency graph today is a
handful of singletons (database, repositories, future backup/import services).

## Decision

Manual constructor injection: an `AppContainer` created in `Application`,
passed down through ViewModels. No DI framework for now.

## Consequences

- Zero DI tooling in the build.
- If the graph or ViewModel test setup gets painful, revisit Hilt as its own
  ADR rather than growing the container indefinitely.
