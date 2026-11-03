# Plan: FR-08 Balance

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [nfr-ledger-concurrency](../nfr-ledger-concurrency/plan.md)

## Steps

### 1. Backend

- [ ] `AccountQueryService.me` with projection and the `usedToday` sum
- [ ] `AccountController` and `AccountResponse`
- **Files:** `account/application/AccountQueryService.java`, `account/api/**`
- **Validation:** `AccountMeTest` (new account, limit with a `Clock` near midnight, `blocked`)

### 2. Frontend

- [ ] `useAccount` and `DashboardPage` with balance, limit bar and blocked notice
- [ ] Route `/` points to the dashboard
- **Files:** `frontend/src/features/account/**`, `frontend/src/routes/router.tsx`
- **Validation:** `DashboardPage.test.tsx`

### 3. E2E

- [ ] `e2e/register.spec.ts` now ends on the dashboard with R$ 0,00 (full scenario 1)
- **Validation:** `npm run e2e` green in Chromium and Firefox

### 4. Manual

- [ ] `manual.md` for the dashboard: balance, daily limit bar and blocked account notice
- **Files:** `docs/features/fr-08-balance/manual.md`

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] E2E green in Chromium and Firefox
- [ ] Dashboard reviewed on desktop and mobile
- [ ] Spec updated with what changed during implementation
