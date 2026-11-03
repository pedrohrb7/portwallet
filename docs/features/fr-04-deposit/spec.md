# Spec: FR-04 Simulated deposit

- **Status:** Approved
- **Phase:** 2
- **PRD:** FR-04; US-03
- **Depends on:** [nfr-ledger-concurrency](../nfr-ledger-concurrency/spec.md), [fr-08-balance](../fr-08-balance/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Let the user add simulated balance to their own account, so they have money to transfer.

## Scope

**Includes**

- `POST /deposits`.
- `/deposit` screen.

**Later**

- Required `Idempotency-Key` and replay: phase 3. Until then, a resend creates a new deposit.
- Rate limit on the authenticated API: phase 5.

## Behavior

1. The amount must be greater than zero, have at most two decimal places and be at most R$ 10,000.00 per operation. Otherwise, 400 `validation-error`.
2. The deposit debits the `EXTERNAL_FUNDS` account and credits the user's account, through `LedgerService` inside `OptimisticRetry`.
3. The transaction has `type = DEPOSIT`, `status = COMPLETED` and the fixed description "Depósito simulado" ("Simulated deposit").
4. A `BLOCKED` user gets 403 `account-blocked`: a blocked account does not receive money.
5. The response is 201 with the transaction ID and the new balance. Deposits have no detail endpoint; they show up in the statement.
6. There is no daily limit for deposits.
7. On the screen, the button is disabled while submitting. On success, the screen shows the new balance returned by the API and invalidates the account query; on error, it shows the message for the `type`.

## API

### `POST /api/v1/deposits`

- **Access:** USER

```json
{ "amount": "250.00" }
```

Response `201`:

```json
{
  "transactionId": "c3f1...",
  "amount": "250.00",
  "balance": "1500.00",
  "createdAt": "2026-10-30T15:00:00Z"
}
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Rule 1 |
| 403 | `account-blocked` | Rule 4 |
| 409 | `concurrency-conflict` | Retries exhausted |

## Data model

No migration.

## Backend

- `transfer.api.DepositController` and `DepositRequest` (`@NotNull`, `@DecimalMin(value = "0.01")`, `@DecimalMax("10000.00")`, `@Digits(integer = 17, fraction = 2)`).
- `transfer.application.DepositService`: loads the user's account, applies rule 4 and calls the ledger.

## Frontend

- `src/features/deposit/DepositPage.tsx`: amount field with BRL mask (`data-testid="deposit-amount"`), "Depositar" ("Deposit") button (`data-testid="deposit-submit"`), success message with the new balance (`data-testid="deposit-success"`).
- `src/components/MoneyInput.tsx`: BRL amount input that produces a string with two decimal places; reused by the transfer.
- Zod schema with the same rules as the backend.

## Security

- The credited account always comes from the token.

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | A valid deposit updates the balance and produces two entries; zero, negative, three decimal places and 10,000.01 give 400; a blocked user gives 403; ledger invariants valid at the end |
| Unit (frontend) | `MoneyInput` turns typing into a string with two decimal places; the Zod schema rejects above 10,000 |
| E2E | PRD scenario 2: a deposit updates the balance on the dashboard |

## Acceptance criteria

- [ ] Deposit respects the amount limits
- [ ] Balance and ledger consistent after the deposit
- [ ] E2E scenario 2 green

## Decisions

- **Deposit in the `transfer` package:** both are money movement use cases that go through the ledger, and in phase 3 they share idempotency.

## Open questions

- None.
