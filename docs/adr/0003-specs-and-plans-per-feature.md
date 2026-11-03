# 3. Specs and plans per feature

Date: 2026-10-30

## Status

Accepted. File and folder names were translated to English by [ADR 0006](0006-english-docs-portuguese-ui.md) (`fr-NN-<slug>`, `nfr-<slug>`, `docs/conventions.md`).

## Context

The PRD describes the whole product but does not detail API contracts, data model or implementation order. Without a place for that detail, decisions end up scattered across the code and the portfolio reviewer does not see the reasoning behind them.

## Decision

- Each PRD requirement becomes a feature in `docs/features/<id>-<slug>/`, with a `spec.md` (how it works) and a `plan.md` (implementation steps). Functional requirements use `fr-NN-<slug>`; cross-cutting work uses `nfr-<slug>`.
- The documents start from the templates in `docs/_templates/` and follow the status cycle described in `docs/README.md`.
- Rules that apply to every feature live in `docs/conventions.md`, not repeated in each spec.
- The spec is updated together with the code when the implementation changes a decision.

## Consequences

- Each roadmap phase maps to a group of features, listed in the `docs/README.md` index.
- Small, focused specs make review easier, but require discipline to keep cross-references correct.
- Long-lived decisions made in a spec still have to become ADRs, because the spec evolves and the ADR does not.
