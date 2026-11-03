# Spec: FR-06 Transfer details

- **Status:** Approved
- **Phase:** 2
- **PRD:** FR-06; Security (ownership check)
- **Depends on:** [fr-05-transfer](../fr-05-transfer/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Let the sender or the recipient see the details of a transfer by its ID, and nobody else.

## Scope

**Includes**

- `GET /transfers/{id}`.
- Receipt screen at `/transfers/:id`, used by the transfer result and by the statement. The route is not in the PRD screen table and is added here.

**Later**

- Showing `fraudCheck` and the reversal state: phase 4 and FR-09.

## Behavior

1. The ID must be a UUID; otherwise, 400 `validation-error`.
2. Unknown transaction, or of type `DEPOSIT`: 404 `not-found`. Deposits are only seen through the statement.
3. A transfer that exists, but whose source and target accounts do not belong to the user: 403 `forbidden`, as the PRD defines.
4. For the sender, `direction = SENT` and `counterparty` is the recipient; for the recipient, `direction = RECEIVED` and `counterparty` is the sender. The counterparty name is masked.
5. The response carries no balances, account IDs or user data beyond the masked name.

## API

### `GET /api/v1/transfers/{id}`

- **Access:** USER (sender or recipient)

Response `200`:

```json
{
  "id": "e7b2...",
  "type": "TRANSFER",
  "status": "COMPLETED",
  "direction": "SENT",
  "amount": "150.00",
  "description": "Almoço",
  "counterparty": { "maskedName": "Ana S****" },
  "createdAt": "2026-10-30T15:00:00Z"
}
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Rule 1 |
| 403 | `forbidden` | Rule 3 |
| 404 | `not-found` | Rule 2 |

## Data model

No migration.

## Backend

- `transfer.application.TransferQueryService.get(UUID userId, UUID transferId)`: a single query with the needed accounts and names, without loading entities.
- `TransferController.get`.

## Frontend

- `src/features/transfer/TransferDetailPage.tsx` at `/transfers/:id`: receipt with direction, amount, counterparty, description and date in the São Paulo time zone (`data-testid="transfer-detail"`); not found and forbidden states.

## Security

- The ownership check lives in the service, not in the controller, and applies to any future caller (e.g. the admin panel uses another path).

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | Sender sees `SENT`, recipient sees `RECEIVED`; a third user gets 403; unknown ID and deposit ID get 404; malformed ID gets 400; response without balances or account IDs |
| Unit (frontend) | Receipt for `SENT` and `RECEIVED`; error state |
| E2E | The link in the transfer result opens the receipt |

## Acceptance criteria

- [ ] Only sender and recipient access the transfer
- [ ] IDOR test covering a third user

## Decisions

- **403 for another user's resource:** follows the PRD error table. IDs are random UUIDs, so the difference between 403 and 404 does not help discover transfers.

## Open questions

- None.
