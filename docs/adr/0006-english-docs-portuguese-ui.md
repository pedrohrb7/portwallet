# 6. English documentation, Brazilian Portuguese UI

Date: 2026-11-03

## Status

Accepted. Amends [ADR 0001](0001-record-architecture-decisions.md) (ADRs were written in Portuguese) and [ADR 0003](0003-specs-and-plans-per-feature.md) (Portuguese file names and `rf-`/`rnf-` prefixes).

## Context

The project is a portfolio presented to both the Brazilian and the international job markets. The documentation (PRD, specs, plans, ADRs, conventions) was written in Portuguese, while code, tests and commits were already in English. Keeping two full copies of the documentation, one per language, would double the maintenance cost, and the process treats an outdated spec as a bug: one of the copies would inevitably fall behind.

The product itself is Brazilian: CPF, BRL amounts, the `America/Sao_Paulo` time zone and users who speak Portuguese.

## Decision

- All documentation is in **English**: `PRD.md`, everything under `docs/`, including user manuals.
- File and folder names follow: `docs/conventions.md`, `fr-NN-<slug>` for functional requirements and `nfr-<slug>` for non-functional ones, with English slugs. PRD requirement IDs became `FR-NN` and `NFR`.
- The root README is the only document kept in both languages: `README.md` (English) and `README.pt-BR.md` (Portuguese), linked to each other and updated together. It is what a reviewer reads first, and it is small enough to keep in sync.
- The **UI stays in Brazilian Portuguese**, along with everything that can reach its users: API error titles, e-mails and OpenAPI descriptions. Documentation quotes those texts verbatim, in Portuguese, with a translation when it helps.
- The existing ADRs were translated without changing their decisions; this ADR records the translation.

## Consequences

- Any reviewer, Brazilian or not, can read the decisions and specs.
- Two languages coexist on purpose: English for engineering artifacts, Portuguese for the product. The rule is in `docs/conventions.md`.
- Every change to the README must be made in both files.
