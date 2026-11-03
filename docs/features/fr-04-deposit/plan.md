# Plan: FR-04 Simulated deposit

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [nfr-ledger-concurrency](../nfr-ledger-concurrency/plan.md), [fr-08-balance](../fr-08-balance/plan.md)

## Steps

### 1. Backend

- [ ] `DepositRequest`, `DepositResponse`, `DepositService`, `DepositController`
- [ ] `AccountBlockedException` (403 `account-blocked`)
- **Files:** `transfer/api/**`, `transfer/application/DepositService.java`, `account/domain/AccountBlockedException.java`
- **Validation:** `DepositTest` (success, amount limits, blocked, invariants)

### 2. Frontend

- [ ] `MoneyInput` and `DepositPage`; link in the menu and on the dashboard
- **Files:** `frontend/src/components/MoneyInput.tsx`, `frontend/src/features/deposit/**`, `frontend/src/routes/router.tsx`
- **Validation:** `MoneyInput.test.tsx`, `DepositPage.test.tsx`

### 3. E2E

- [ ] `e2e/deposit.spec.ts` (scenario 2)
- [ ] Helper `deposit(request, token, amount)` to prepare balance for the next tests
- **Validation:** `npm run e2e` green in Chromium and Firefox

### 4. Manual

- [ ] `manual.md`: how to deposit, the amount limits and the error messages
- **Files:** `docs/features/fr-04-deposit/manual.md`

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] E2E green in Chromium and Firefox
- [ ] Screen reviewed on desktop and mobile
- [ ] Spec updated with what changed during implementation
