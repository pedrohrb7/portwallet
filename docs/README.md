# Documentation

Every relevant change to PortWallet starts as a feature documented here. The [PRD](../PRD.md) says **what** to build; specs say **how**; plans say **in what order**; ADRs record **why** a decision was made.

```text
docs/
├── README.md           this index
├── conventions.md      cross-cutting rules (API, money, errors, code)
├── _templates/         spec and plan templates
├── adr/                architecture decisions (immutable once accepted)
├── backlog/
│   └── refactor/       planned refactors (project-docs skill layout), with their own index
└── features/
    └── <id>-<slug>/
        ├── spec.md     technical specification
        ├── plan.md     implementation plan
        └── manual.md   user manual, for people using the system (features with a UI)
```

Planned refactors live in [`backlog/refactor/`](backlog/refactor/README.md), one dated file per refactor, created from that folder's `TEMPLATE.md`.

All documentation is written in English ([ADR 0006](adr/0006-english-docs-portuguese-ui.md)). The product speaks Brazilian Portuguese (default), English, Spanish and French ([ADR 0007](adr/0007-multilingual-product.md)); UI texts quoted in the docs (labels, messages, e-mails) are in Portuguese, the default language, exactly as the user sees them.

## Workflow

1. **Spec.** Copy `_templates/spec.md` to `features/<id>-<slug>/spec.md` and describe the behavior, the API contract, the data model and the tests that prove the feature. Status `Draft`.
2. **Review.** Doubts go in the "Open questions" section. When there are none left, the spec moves to `Approved`.
3. **Plan.** Copy `_templates/plan.md` and break the spec into small, verifiable steps, each with the files it touches and the test that validates it.
4. **Implementation.** Check off plan steps as they are done. If the implementation changes a decision, update the spec in the same piece of work; an outdated spec is a bug.
5. **Manual.** Features with a UI get a `manual.md` with the step-by-step usage, the messages the person may see and what to do about each one. The manual describes the behavior in the spec and changes along with it.
6. **Closing.** Once the acceptance criteria are met, spec and plan move to `Done`, and long-lived decisions become ADRs.

### Status

| Status | Meaning |
| --- | --- |
| Draft | Being written or with open questions |
| Approved | Ready to implement |
| In progress | Plan being executed |
| Done | Acceptance criteria met and CI green |
| Superseded | Replaced by another feature, named at the top of the document |

### Names

- `fr-NN-<slug>`: functional requirement from the PRD (e.g. `fr-05-transfer`).
- `nfr-<slug>`: non-functional requirement or cross-cutting work (e.g. `nfr-ledger-concurrency`).
- Slugs in English, lower case, kebab-case.

### Spec or ADR?

A spec describes a feature and evolves with it. An ADR records a decision that reaches beyond one feature (library choice, data format, architectural trade-off) and is not edited once accepted. A spec may cite ADRs; when it makes a decision of that size, the decision becomes an ADR at the end.

## Features

| Phase | Feature | Spec | Plan | Status |
| --- | --- | --- | --- | --- |
| 1 | Foundation | [spec](features/nfr-foundation/spec.md) | [plan](features/nfr-foundation/plan.md) | Done |
| 2 | Application base (errors, security, frontend shell) | [spec](features/nfr-application-base/spec.md) | [plan](features/nfr-application-base/plan.md) | Done |
| 2 | API documentation (OpenAPI and Swagger UI) | [spec](features/nfr-api-docs/spec.md) | [plan](features/nfr-api-docs/plan.md) | Done |
| 2 | FR-01 Registration and confirmation ([manual](features/fr-01-registration/manual.md)) | [spec](features/fr-01-registration/spec.md) | [plan](features/fr-01-registration/plan.md) | Done |
| 2 | Internationalization (pt-BR, en, es, fr) | [spec](features/nfr-i18n/spec.md) | [plan](features/nfr-i18n/plan.md) | Done |
| 2 | FR-02 Login and refresh | [spec](features/fr-02-login/spec.md) | [plan](features/fr-02-login/plan.md) | Approved |
| 2 | FR-03 Logout | [spec](features/fr-03-logout/spec.md) | [plan](features/fr-03-logout/plan.md) | Approved |
| 2 | Ledger and concurrency | [spec](features/nfr-ledger-concurrency/spec.md) | [plan](features/nfr-ledger-concurrency/plan.md) | Approved |
| 2 | FR-08 Balance | [spec](features/fr-08-balance/spec.md) | [plan](features/fr-08-balance/plan.md) | Approved |
| 2 | FR-04 Simulated deposit | [spec](features/fr-04-deposit/spec.md) | [plan](features/fr-04-deposit/plan.md) | Approved |
| 2 | FR-17 Recipient lookup | [spec](features/fr-17-recipient-lookup/spec.md) | [plan](features/fr-17-recipient-lookup/plan.md) | Approved |
| 2 | FR-05 Transfer | [spec](features/fr-05-transfer/spec.md) | [plan](features/fr-05-transfer/plan.md) | Approved |
| 2 | FR-06 Transfer details | [spec](features/fr-06-transfer-details/spec.md) | [plan](features/fr-06-transfer-details/plan.md) | Approved |
| 2 | FR-07 Statement | [spec](features/fr-07-statement/spec.md) | [plan](features/fr-07-statement/plan.md) | Approved |

The table order is the Phase 2 implementation order: each feature depends only on the ones above it.

## ADRs

| ADR | Title |
| --- | --- |
| [0001](adr/0001-record-architecture-decisions.md) | Record architecture decisions |
| [0002](adr/0002-foundation-stack-and-structure.md) | Foundation: stack, versions and repository structure |
| [0003](adr/0003-specs-and-plans-per-feature.md) | Specs and plans per feature |
| [0004](adr/0004-money-as-string-in-json.md) | Money as a string in JSON |
| [0005](adr/0005-password-set-on-activation.md) | Password set on activation |
| [0006](adr/0006-english-docs-portuguese-ui.md) | English documentation, Brazilian Portuguese UI |
| [0007](adr/0007-multilingual-product.md) | Multilingual product |
