# Spec: <ID> <Feature name>

- **Status:** Draft
- **Phase:** <N>
- **PRD:** <FR-NN, PRD section>
- **Depends on:** <features, or "none">
- **Plan:** [plan.md](plan.md)

## Goal

<One or two sentences: what the feature delivers and for whom.>

## Scope

**Includes**

- <...>

**Later**

- <item>: <phase or feature where it comes in>

## Behavior

<Numbered business rules, from the happy path to the edges. Each rule must be able to become a test.>

1. <...>

## API

### `<METHOD> /api/v1/<route>`

- **Access:** <Public | USER | ADMIN>
- **Headers:** <required>

Request:

```json
{}
```

Response `<status>`:

```json
{}
```

| Status | `type` | When |
| --- | --- | --- |
| <...> | <...> | <...> |

## Data model

<New migrations (`V<N>__<name>.sql`), tables, columns, constraints and indexes.>

## Backend

<Packages, main classes and responsibilities.>

## Frontend

<Routes, components, screen states and `data-testid`.>

## Security

<Authorization, ownership checks, sensitive data, what never goes to the log.>

## Tests

| Layer | Scenario |
| --- | --- |
| Unit | <...> |
| Integration | <...> |
| E2E | <...> |

## Acceptance criteria

- [ ] <...>

## Decisions

<Choices made in this spec and why. Long-lived ones become ADRs when the feature is done.>

## Open questions

- <...>
