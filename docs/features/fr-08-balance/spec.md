# Spec: FR-08 Balance

- **Status:** Approved
- **Phase:** 2
- **PRD:** FR-08; Frontend (Dashboard)
- **Depends on:** [nfr-ledger-concurrency](../nfr-ledger-concurrency/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Show the user the current balance and how much of the daily transfer limit is still available, on the home screen.

## Scope

**Includes**

- `GET /accounts/me`.
- Dashboard at `/` with balance and daily limit.

**Later**

- Latest transactions on the dashboard: [FR-07](../fr-07-statement/spec.md), which creates the endpoint they use.
- ETag and 304 with `If-None-Match`: phase 6.

## Behavior

1. `GET /accounts/me` returns the authenticated user's account; there is no ID parameter, so there is no way to query another account.
2. `usedToday` is the sum of the `COMPLETED` transfers sent by the account since 00:00 today in `America/Sao_Paulo`. Deposits do not count.
3. `availableToday = max(0, dailyLimit - usedToday)`.
4. `blocked` is `true` when the user is `BLOCKED`.
5. The dashboard shows the balance formatted in BRL, the used limit in an accessible progress bar (`role="progressbar"` with `aria-valuenow`) and a notice when the account is blocked.
6. The dashboard fetches the account every 15 s while the tab is visible and right after any completed deposit or transfer. The new balance only shows after the server response, never optimistically.

## API

### `GET /api/v1/accounts/me`

- **Access:** USER

Response `200`:

```json
{
  "accountId": "6a1d...",
  "balance": "1250.00",
  "dailyLimit": "5000.00",
  "usedToday": "300.00",
  "availableToday": "4700.00",
  "blocked": false
}
```

| Status | `type` | When |
| --- | --- | --- |
| 401 | `unauthorized` | No valid token |

## Data model

No migration. `usedToday` uses the `ix_transactions_source_created` index.

## Backend

- `account.application.AccountQueryService.me(UUID userId)`: an account projection and an aggregate query for `usedToday`.
- `account.api.AccountController`.

## Frontend

- `src/features/account/useAccount.ts`: query `['account', 'me']`, with a 15 s `refetchInterval`, paused while the tab is hidden.
- `src/features/account/DashboardPage.tsx`: balance card (`data-testid="balance"`), limit bar (`data-testid="daily-limit"`), blocked account notice (`data-testid="account-blocked"`), shortcuts to deposit, transfer and see the statement.
- Loading states with a skeleton and error states with retry.

## Security

- The account always comes from the token's `sub`, never from a parameter.

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | A new account returns balance 0.00 and limit 5,000.00; `usedToday` ignores deposits and yesterday's transfers (with a controlled `Clock` near São Paulo midnight); `blocked` reflects the status |
| Unit (frontend) | Balance formatted in BRL; limit bar with the ARIA attributes |
| E2E | PRD scenario 1 (end): after login, the dashboard shows R$ 0,00 |

## Acceptance criteria

- [ ] Correct balance and daily limit on the dashboard
- [ ] "Today" computed in the São Paulo time zone

## Decisions

- **No optimistic update:** a PRD decision for financial operations; becomes an ADR in phase 6 together with the other performance decisions.

## Open questions

- None.
