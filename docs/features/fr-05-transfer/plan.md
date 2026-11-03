# Plan: FR-05 Transfer

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [fr-17-recipient-lookup](../fr-17-recipient-lookup/plan.md), [fr-04-deposit](../fr-04-deposit/plan.md)

## Steps

### 1. Business rules

- [ ] `TransferRequest`, rule exceptions and `TransferService` with the checks in the spec order
- **Files:** `transfer/api/TransferRequest.java`, `transfer/application/TransferService.java`, `transfer/domain/**`
- **Validation:** `TransferServiceTest` (one failure per rule, without changing balances)

### 2. Endpoint

- [ ] `TransferController.create` with `Location` and `TransferResponse`
- **Files:** `transfer/api/TransferController.java`, `transfer/api/TransferResponse.java`
- **Validation:** `TransferApiTest` (201, `Location` header, Problem Details for each error)

### 3. Concurrency through the API

- [ ] `TransferConcurrencyTest`: 100 cross transfers and 20 simultaneous ones from an account with balance for 5
- **Files:** `backend/src/test/java/com/portwallet/transfer/TransferConcurrencyTest.java`
- **Validation:** `LedgerInvariants.assertConsistent` at the end; stable across 20 runs in a row

### 4. Frontend

- [ ] `TransferPage` with the amount, confirmation and result steps, integrating `RecipientStep`
- **Files:** `frontend/src/features/transfer/**`
- **Validation:** `TransferPage.test.tsx`

### 5. E2E

- [ ] `e2e/transfer.spec.ts`: scenario 3 (up to the result, with two browser contexts) and scenario 4
- **Validation:** `npm run e2e` green in Chromium and Firefox

### 6. Manual

- [ ] `manual.md`: the four transfer steps, including the recipient lookup (FR-17), and the error messages
- **Files:** `docs/features/fr-05-transfer/manual.md`

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] E2E green in Chromium and Firefox
- [ ] Every screen step reviewed on desktop and mobile
- [ ] Spec updated with what changed during implementation

## Risks

- With 3 retries, heavy contention on the same account may produce a 409 in the concurrency test. The test accepts `concurrency-conflict` as a valid outcome and checks only the invariants and the number of completed transfers.
