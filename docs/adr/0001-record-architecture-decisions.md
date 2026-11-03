# 1. Record architecture decisions

Date: 2026-10-30

## Status

Accepted. The language of the ADRs was changed to English by [ADR 0006](0006-english-docs-portuguese-ui.md).

## Context

The project is a portfolio reviewed by recruiters and interviewers. Showing why each choice was made is worth as much as the code, and the PRD's definition of done requires relevant decisions to be recorded.

## Decision

Use Architecture Decision Records in Michael Nygard's format, in `docs/adr/`, numbered in sequence (`NNNN-title-in-kebab-case.md`). An accepted ADR is not edited: when the decision changes, a new ADR replaces it and the old one becomes `Superseded by N`.

## Consequences

- Each roadmap phase records its relevant decisions before it is considered done.
- The decision history is versioned together with the code.
