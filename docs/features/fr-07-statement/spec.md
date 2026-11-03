# Spec: FR-07 Statement

- **Status:** Approved
- **Phase:** 2
- **PRD:** FR-07; US-05; Performance (cursor pagination)
- **Depends on:** [fr-05-transfer](../fr-05-transfer/spec.md), [fr-04-deposit](../fr-04-deposit/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Show the movements of the user's account, newest first, with period and type filters and cursor pagination, which keeps the cost per page constant and never skips nor repeats items when new transactions come in.

## Scope

**Includes**

- `GET /statement`.
- `/statement` screen with infinite scroll and filters in the URL.
- Latest transactions on the dashboard.

**Later**

- ETag and 304 on the first page: phase 6.
- Query ceiling with datasource-proxy and `EXPLAIN` recorded in an ADR: phase 6.

## Behavior

1. The statement lists the entries (`ledger_entries`) of the user's account, ordered by `created_at DESC, id DESC`.
2. `size` goes from 1 to 100, default 20. Otherwise, 400 `validation-error`.
3. `from` and `to` are dates (`YYYY-MM-DD`), inclusive, interpreted in the `America/Sao_Paulo` time zone: `from` becomes 00:00 of that day and `to` becomes 00:00 of the following day, exclusive. `from` after `to`: 400 `validation-error`.
4. `type` filters by transaction type: `DEPOSIT`, `TRANSFER` or `REVERSAL`.
5. The cursor is opaque: Base64URL of `<created_at in microseconds since the epoch>:<entry id>`. The next page uses `WHERE (created_at, id) < (:createdAt, :id)`, never `OFFSET`.
6. A cursor that does not decode, or with an invalid format: 400 `invalid-cursor`.
7. The server fetches `size + 1` rows; if the extra row comes, it returns `nextCursor` from the last item shown; otherwise, `nextCursor` is `null`.
8. Filters are not part of the cursor. The client sends the same filters again on each page; changing a filter starts over from the beginning.
9. Each item carries the counterparty with the masked name. Deposits have no counterparty; the item shows the description "Depósito simulado" ("Simulated deposit").
10. Everything in a single query per page, with projection and joins, no N+1.
11. On the screen, filters live in the URL (`/statement?type=TRANSFER&from=2026-10-01`), so reloading or sharing the link keeps the view. The next page loads when the end of the list is reached and also through a "Carregar mais" ("Load more") button, reachable by keyboard.
12. The dashboard shows the 5 most recent entries (`size=5`) with a link to the full statement.

## API

### `GET /api/v1/statement?size=20&type=TRANSFER&from=2026-10-01&to=2026-10-30&cursor=...`

- **Access:** USER

Response `200`:

```json
{
  "items": [
    {
      "entryId": "0f3a...",
      "transactionId": "e7b2...",
      "type": "TRANSFER",
      "direction": "DEBIT",
      "amount": "150.00",
      "balanceAfter": "1100.00",
      "description": "Almoço",
      "counterparty": { "maskedName": "Ana S****" },
      "createdAt": "2026-10-30T15:00:00Z"
    }
  ],
  "nextCursor": "MTc2MTgzNjQwMDAwMDAwMDo..."
}
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Rules 2, 3 and 4 |
| 400 | `invalid-cursor` | Rule 6 |

## Data model

No migration. The query uses `ix_ledger_entries_statement (account_id, created_at DESC, id DESC)`, created in [nfr-ledger-concurrency](../nfr-ledger-concurrency/spec.md).

## Backend

- `ledger.domain.StatementCursor`: encodes and decodes the cursor.
- `ledger.application.StatementService.page(UUID userId, StatementQuery query)`.
- `ledger.infra.StatementRepository`: query with `JdbcClient` (explicit SQL, to control the tuple comparison and the index use) and direct projection into `StatementItem`.
- `ledger.api.StatementController` and `StatementQuery` with Bean Validation.

## Frontend

- `src/features/statement/useStatement.ts`: `useInfiniteQuery` with key `['statement', filters]`.
- `src/features/statement/StatementPage.tsx`: filters (type, period) bound to the search params; list (`data-testid="statement-list"`, items `statement-item`) with date, description, counterparty, amount colored by direction and balance after; `IntersectionObserver` and a "Carregar mais" button (`data-testid="statement-load-more"`); empty state.
- `src/features/account/RecentActivity.tsx` on the dashboard, using `size=5`.
- Transfer items open the receipt (`/transfers/:id`).

## Security

- The account comes from the token; there is no account parameter.
- The cursor is not signed: tampering with it only changes the starting point within the user's own account, which stays filtered by the token.

## Tests

| Layer | Scenario |
| --- | --- |
| Unit | `StatementCursor` round trip; invalid input |
| Integration | Descending order; pagination goes through everything without repeating or skipping; inserting new transactions between two pages does not change the second; type and period filters (with the São Paulo midnight edge); `size` out of range; invalid cursor; deposit item without counterparty; a user only sees their own entries |
| Unit (frontend) | Filters in sync with the URL; the "Carregar mais" button fetches the next page |
| E2E | Full PRD scenario 3: the transfer shows up in both users' statements, with opposite directions |

## Acceptance criteria

- [ ] Cursor pagination without repeated or skipped items
- [ ] Filters reflected in the URL
- [ ] Full E2E scenario 3 green

## Decisions

- **Explicit SQL with `JdbcClient`:** the tuple comparison and the index use stay visible and testable, which JPQL would hide.
- **Filters outside the cursor:** the cursor stays small and the client already needs to know the filters to build the screen.

## Open questions

- Confirm during implementation that H2 uses the index with the tuple comparison `(created_at, id) < (?, ?)`. If it does not, switch to the equivalent form `created_at < ? OR (created_at = ? AND id < ?)` and record the difference.
