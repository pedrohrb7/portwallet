# Spec: FR-05 Transfer

- **Status:** Approved
- **Phase:** 2
- **PRD:** FR-05; US-04; Concurrency and consistency
- **Depends on:** [fr-17-recipient-lookup](../fr-17-recipient-lookup/spec.md), [fr-04-deposit](../fr-04-deposit/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Let the user transfer balance to another active user, checked through the recipient lookup, respecting balance, daily limit and account status, with consistency under concurrency.

## Scope

**Includes**

- `POST /transfers`.
- Amount, confirmation and result steps of the `/transfer` screen.
- Test with 100 concurrent transfers through the API.

**Later**

- Required `Idempotency-Key`, key generated when the confirmation opens, double click and retry: phase 3.
- Antifraud with circuit breaker and fallback: phase 4.
- Sender and recipient notification (FR-10): phase 4.
- Rate limit of 10 transfers per minute: phase 5.
- TOTP code above R$ 1,000 (`X-TOTP-Code`): phase 6.

## Behavior

The rules are checked in this order, and the first one that fails sets the response:

1. Valid body: `recipientId` present; `amount` greater than zero with up to two decimal places; optional `description` with up to 140 characters, trimmed. Otherwise, 400 `validation-error`.
2. `BLOCKED` sender: 403 `account-blocked`.
3. Destination equal to the sender's account: 422 `self-transfer`.
4. Unknown destination, system account, or destination user not `ACTIVE`: 422 `recipient-unavailable`.
5. Amount greater than the balance: 422 `insufficient-balance`.
6. `usedToday + amount` greater than the daily limit (see [FR-08](../fr-08-balance/spec.md)): 422 `daily-limit-exceeded`, with `availableToday` in the error body.
7. With everything passing, `LedgerService` debits the source and credits the destination in a `TRANSFER`, `COMPLETED` transaction, inside `OptimisticRetry`. Rules 2 to 6 are re-evaluated on every attempt, since balance and limit may have changed.
8. Response 201 with `Location: /api/v1/transfers/{id}`, the transfer details and the sender's new balance.

Two simultaneous transfers from the same account cannot, together, exceed the balance or the daily limit: both change the source account's `version`, so the second one repeats the checks with the updated values.

On the screen:

9. The flow has four steps: recipient ([FR-17](../fr-17-recipient-lookup/spec.md)), amount and description, confirmation and result. The amount step shows the balance and the limit available today.
10. The confirmation shows the masked name, the amount in BRL and the description. The "Confirmar" ("Confirm") button is disabled while submitting.
11. 422 errors go back to the matching step with the message for the `type` (recipient, amount). The balance shown only changes after the server response.
12. The result shows the receipt with a link to the details (FR-06) and shortcuts to a new transfer and to the statement.

## API

### `POST /api/v1/transfers`

- **Access:** USER

```json
{ "recipientId": "6a1d...", "amount": "150.00", "description": "Almoço" }
```

Response `201`:

```json
{
  "id": "e7b2...",
  "status": "COMPLETED",
  "amount": "150.00",
  "description": "Almoço",
  "recipient": { "maskedName": "Ana S****" },
  "balance": "1100.00",
  "createdAt": "2026-10-30T15:00:00Z"
}
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Rule 1 |
| 403 | `account-blocked` | Rule 2 |
| 409 | `concurrency-conflict` | Retries exhausted |
| 422 | `self-transfer` | Rule 3 |
| 422 | `recipient-unavailable` | Rule 4 |
| 422 | `insufficient-balance` | Rule 5 |
| 422 | `daily-limit-exceeded` | Rule 6 |

The PRD shows an example with `targetEmail`. This spec uses the `recipientId` returned by the lookup, as decided in [FR-17](../fr-17-recipient-lookup/spec.md).

## Data model

No migration.

## Backend

- `transfer.api.TransferController.create` and `TransferRequest`.
- `transfer.application.TransferService`: rules 2 to 7; gets `usedToday` from the same service as FR-08.
- Exceptions `SelfTransferException`, `RecipientUnavailableException`, `DailyLimitExceededException`.

## Frontend

- `src/features/transfer/TransferPage.tsx`: step machine with local state.
- `AmountStep.tsx` (`data-testid="transfer-amount"`, `transfer-description`), `ConfirmStep.tsx` (`data-testid="transfer-confirm"`), `ResultStep.tsx` (`data-testid="transfer-success"`).
- TanStack Query mutation that, on success, invalidates `['account', 'me']` and `['statement']`.

## Security

- The source always comes from the token.
- Error messages do not reveal why a recipient is unavailable.

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | Success with correct balance and entries; one failure for each rule from 1 to 6, without changing any balance; daily limit adding up earlier transfers of the day; destination blocked between the lookup and the transfer gives `recipient-unavailable` |
| Concurrency | 100 cross transfers through the API (MockMvc) among 10 users, in parallel: valid invariants and no negative balance; 20 simultaneous transfers from an account with balance for only 5: at most 5 complete, the others get `insufficient-balance` or `concurrency-conflict`, and the final balance is the initial one minus the completed ones |
| Unit (frontend) | Navigation between steps; button disabled while submitting; a 422 error goes back to the right step |
| E2E | PRD scenario 3 (transfer part, with two browser contexts); scenario 4: insufficient balance shows the error and does not change the balance |

## Acceptance criteria

- [ ] Every business rule with a test
- [ ] Test with 100 concurrent transfers green
- [ ] E2E scenarios 3 and 4 green (the full scenario 3 depends on the statement)

## Decisions

- **Fixed check order:** the response to the same situation is always the same, which keeps tests and messages predictable.
- **`availableToday` in the limit error:** the frontend shows how much can still be transferred without a second call.

## Open questions

- None.
