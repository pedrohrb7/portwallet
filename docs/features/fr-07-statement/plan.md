# Plan: FR-07 Statement

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [fr-05-transfer](../fr-05-transfer/plan.md), [fr-04-deposit](../fr-04-deposit/plan.md)

## Steps

### 1. Cursor

- [ ] `StatementCursor` with Base64URL and validation
- **Files:** `ledger/domain/StatementCursor.java`
- **Validation:** `StatementCursorTest`

### 2. Query

- [ ] `StatementRepository` with `JdbcClient`, filters and `size + 1`
- [ ] Check with `EXPLAIN` on H2 that the query uses `ix_ledger_entries_statement` and resolve the spec's open question
- **Files:** `ledger/infra/StatementRepository.java`
- **Validation:** `StatementRepositoryTest` (order, filters, midnight edge)

### 3. Endpoint

- [ ] `StatementQuery`, `StatementService`, `StatementController`, `StatementResponse`
- **Files:** `ledger/application/StatementService.java`, `ledger/api/**`
- **Validation:** `StatementApiTest` (full walk without repetition, insertion between pages, 400 errors, isolation per user)

### 4. Frontend

- [ ] `useStatement`, `StatementPage` with filters in the URL and incremental loading
- [ ] `RecentActivity` on the dashboard
- **Files:** `frontend/src/features/statement/**`, `frontend/src/features/account/RecentActivity.tsx`
- **Validation:** `StatementPage.test.tsx`

### 5. E2E

- [ ] Complete scenario 3 in `e2e/transfer.spec.ts`: both users' statements
- [ ] `e2e/statement.spec.ts`: type filter reflected in the URL and kept on reload
- **Validation:** `npm run e2e` green in Chromium and Firefox

### 6. Manual

- [ ] `manual.md`: reading the statement, filters, loading more and the latest transactions on the dashboard
- **Files:** `docs/features/fr-07-statement/manual.md`

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] E2E scenarios 1 to 4 green in Chromium and Firefox (Phase 2 acceptance)
- [ ] Statement and dashboard reviewed on desktop and mobile
- [ ] Spec updated with what changed during implementation
- [ ] READMEs updated with the Phase 2 delivery

## Risks

- Entries with the same `created_at`: the `id` breaks the tie, and the test creates several entries with a stopped `Clock` to prove it.
