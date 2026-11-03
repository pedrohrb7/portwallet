# Plan: FR-06 Transfer details

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [fr-05-transfer](../fr-05-transfer/plan.md)

## Steps

### 1. Backend

- [ ] Transfer projection with accounts and names; `TransferQueryService.get` with the ownership check
- [ ] `TransferController.get` and `TransferDetailResponse`
- **Files:** `transfer/application/TransferQueryService.java`, `transfer/api/**`, `ledger/infra/TransactionRepository.java`
- **Validation:** `TransferDetailTest` (sender, recipient, third user, unknown, deposit, malformed)

### 2. Frontend

- [ ] `TransferDetailPage` and route `/transfers/:id`; link in the transfer result
- **Files:** `frontend/src/features/transfer/TransferDetailPage.tsx`, `frontend/src/routes/router.tsx`
- **Validation:** `TransferDetailPage.test.tsx`

### 3. E2E

- [ ] Open the receipt from the result in `e2e/transfer.spec.ts`
- **Validation:** `npm run e2e` green in Chromium and Firefox

### 4. Manual

- [ ] "Receipt" section in the FR-05 manual
- **Files:** `docs/features/fr-05-transfer/manual.md`

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] E2E green in Chromium and Firefox
- [ ] Receipt reviewed on desktop and mobile
- [ ] Spec updated with what changed during implementation
