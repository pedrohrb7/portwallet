# Plan: FR-17 Recipient lookup

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [fr-01-registration](../fr-01-registration/plan.md), [nfr-ledger-concurrency](../nfr-ledger-concurrency/plan.md)

## Steps

### 1. Name mask

- [x] `NameMasker` (brought forward by the password on activation refactor, in `shared.privacy`)
- **Files:** `shared/privacy/NameMasker.java`
- **Validation:** `NameMaskerTest`

### 2. Lookup

- [ ] `RecipientService.lookup` (e-mail, or CPF through the blind index) and `RecipientController`
- **Files:** `account/application/RecipientService.java`, `account/api/RecipientController.java`
- **Validation:** `RecipientLookupTest`

### 3. Recent recipients

- [ ] Query for distinct recipients with projection and `RecipientService.recent`
- **Files:** `account/application/RecipientService.java`, `ledger/infra/TransactionRepository.java`
- **Validation:** `RecentRecipientsTest`

### 4. Frontend

- [ ] `RecipientStep` with lookup and recent recipients
- **Files:** `frontend/src/features/transfer/**`
- **Validation:** `RecipientStep.test.tsx`

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] Spec updated with what changed during implementation
