# Plan: Ledger and concurrency

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [fr-01-registration](../fr-01-registration/plan.md)

## Steps

### 1. Migration and entities

- [ ] `V7__ledger.sql` with the system account
- [ ] `Transaction`, `LedgerEntry`, enums and repositories
- **Files:** `db/migration/V7__ledger.sql`, `ledger/domain/**`, `ledger/infra/**`
- **Validation:** `./mvnw verify`

### 2. Account rules

- [ ] `Account.debit` and `Account.credit`, `InsufficientBalanceException`
- [ ] `SystemAccountRepository.applyDelta`
- **Files:** `account/domain/Account.java`, `account/infra/SystemAccountRepository.java`
- **Validation:** `AccountTest`

### 3. LedgerService

- [ ] `LedgerService.post` with double entry and `balance_after`
- **Files:** `ledger/application/LedgerService.java`
- **Validation:** `LedgerServiceTest` (entries, balance, negative `CHECK`, immutable entry)

### 4. Optimistic retry

- [ ] `OptimisticRetry` with up to 3 retries, random wait and `ConcurrencyConflictException`
- **Files:** `ledger/application/OptimisticRetry.java`, `shared/error/ConcurrencyConflictException.java`
- **Validation:** `OptimisticRetryTest` (success on the second attempt; 409 once they run out)

### 5. Invariants and concurrency

- [ ] `LedgerInvariants` in `src/test`
- [ ] `LedgerConcurrencyTest`: 100 cross transfers and 50 simultaneous deposits, calling the service directly
- **Files:** `backend/src/test/java/com/portwallet/ledger/**`
- **Validation:** the test passes 20 times in a row, run locally with `@RepeatedTest(20)`, without intermittent failures

## Final verification

- [ ] `./mvnw verify` green
- [ ] Concurrency test stable across repeated runs
- [ ] Spec updated with what changed during implementation

## Risks

- Flaky concurrency test: use a `CountDownLatch` for a simultaneous start and check the final state, never the order.
- H2 locking behaving differently from PostgreSQL: the balance rule is enforced by the `CHECK`, which works the same in both.
