# Spec: Ledger and concurrency

- **Status:** Approved
- **Phase:** 2
- **PRD:** Concurrency and consistency; Data model (invariants); FR-08
- **Depends on:** [fr-01-registration](../fr-01-registration/spec.md) (`accounts` table)
- **Plan:** [plan.md](plan.md)

## Goal

Guarantee that every money movement is recorded in double entry, that each account's balance always matches the ledger and that no balance goes negative, even with many simultaneous operations on the same account and across several instances.

## Scope

**Includes**

- Tables `transactions` and `ledger_entries` and the `EXTERNAL_FUNDS` system account.
- `LedgerService`, the only place that changes balances.
- Optimistic locking with retries and 409 once they run out.
- Test utility that checks the ledger invariants.

**Later**

- Reversal (inverse entries): FR-09, phase 5 or later.
- Filled `idempotency_key` column: phase 3. It is created here already, optional.
- Filled `fraud_check`: phase 4. Also created here, optional.

## Behavior

1. Every movement creates one row in `transactions` and exactly two entries in `ledger_entries`: a `DEBIT` on the source account and a `CREDIT` on the target, with the same amount, in the same database transaction.
2. Deposits debit the `EXTERNAL_FUNDS` system account, so there is no exception to double entry.
3. Each entry stores `balance_after`, the account balance right after it.
4. The account `balance` is updated in the same transaction as the entries and always equals credits minus debits in the ledger.
5. A `USER` account never has a negative balance: the service validates before debiting (`insufficient-balance`) and the database enforces it with a `CHECK`.
6. `USER` accounts use optimistic locking through the `version` column. On conflict, the whole operation is repeated in a new transaction, up to 3 retries, with a short random wait (10 to 50 ms) between them. Once they run out, 409 `concurrency-conflict`.
7. The system account is updated with an atomic `UPDATE ... SET balance = balance - ?`, without checking `version`. It takes part in every deposit; with optimistic locking, simultaneous deposits by different users would conflict with each other for no reason. Its `balance_after` is read after the `UPDATE`, with the row already locked by the transaction itself.
8. Entries are immutable: the entity is `@Immutable` and the repository exposes no update or delete.
9. Transaction amounts are always positive, and source and target are different accounts (`CHECK` in the database).

## API

No endpoints of its own. The balance is exposed by [FR-08](../fr-08-balance/spec.md) and the entries by the [statement](../fr-07-statement/spec.md).

## Data model

`V7__ledger.sql`

```sql
CREATE TABLE transactions (
    id                 UUID          PRIMARY KEY,
    type               VARCHAR(10)   NOT NULL CHECK (type IN ('DEPOSIT', 'TRANSFER', 'REVERSAL')),
    status             VARCHAR(10)   NOT NULL CHECK (status IN ('COMPLETED', 'REVERSED')),
    amount             DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    source_account_id  UUID          NOT NULL REFERENCES accounts (id),
    target_account_id  UUID          NOT NULL REFERENCES accounts (id),
    fraud_check        VARCHAR(10)   CHECK (fraud_check IN ('APPROVED', 'SKIPPED', 'DENIED')),
    idempotency_key    UUID,
    description        VARCHAR(140),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_transactions_distinct_accounts CHECK (source_account_id <> target_account_id)
);
CREATE INDEX ix_transactions_source_created ON transactions (source_account_id, created_at);

CREATE TABLE ledger_entries (
    id              UUID          PRIMARY KEY,
    transaction_id  UUID          NOT NULL REFERENCES transactions (id),
    account_id      UUID          NOT NULL REFERENCES accounts (id),
    direction       VARCHAR(6)    NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount          DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    balance_after   DECIMAL(19,2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_ledger_entries_statement ON ledger_entries (account_id, created_at DESC, id DESC);
CREATE INDEX ix_ledger_entries_transaction ON ledger_entries (transaction_id);

INSERT INTO accounts (id, user_id, kind, balance, daily_limit, version, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 'SYSTEM', 0.00, 0.00, 0, CURRENT_TIMESTAMP);
```

The `EXPLAIN` of the indexes, before and after, is recorded in an ADR in phase 6, as the PRD asks.

## Backend

Package `com.portwallet.ledger`:

- `domain.Transaction`, `domain.LedgerEntry` (`@Immutable`), `domain.TransactionType`, `domain.Direction`.
- `application.LedgerService.post(TransactionType type, UUID sourceAccountId, UUID targetAccountId, Money amount, String description)`: must be called inside a transaction opened by the use case; loads the accounts, applies rules 5 and 7, writes the transaction and the entries, and returns a `PostedTransaction`.
- `application.OptimisticRetry.execute(Supplier<T>)`: runs the supplier in a new `TransactionTemplate` on each attempt and handles `ObjectOptimisticLockingFailureException` according to rule 6. Use cases (deposit, transfer) go through it, and never call `LedgerService` outside it.

Package `com.portwallet.account`:

- `domain.Account` with `@Version`, `debit(Money)` and `credit(Money)`, which throw `InsufficientBalanceException` (422 `insufficient-balance`).
- `infra.SystemAccountRepository.applyDelta(UUID, BigDecimal)` with the atomic `UPDATE` of rule 7.

## Frontend

No changes.

## Security

- No endpoint changes `balance` directly; only `LedgerService` does.
- The system account never shows up in recipient lookups nor as a user account.

## Tests

| Layer | Scenario |
| --- | --- |
| Unit | `Account.debit` rejects an amount larger than the balance; `Account.credit` adds with scale 2 |
| Integration | Deposit and transfer produce two entries with the same amount and correct `balance_after`; the database `CHECK` refuses a negative balance even when bypassing the service; an attempt to change an entry fails |
| Concurrency | 100 cross transfers among 10 accounts on 16 threads, started together by a `CountDownLatch`: valid invariants, no negative balance, sum of every balance (including the system account) equal to zero; 50 simultaneous deposits into different accounts finish without any 409 |
| Retry | With a conflict forced on the first attempt, the operation completes on the second; with conflict on every attempt, 409 `concurrency-conflict` |

`LedgerInvariants.assertConsistent(JdbcTemplate)` lives in `src/test` and is called at the end of every concurrency test, in this and the following features.

## Acceptance criteria

- [ ] Test with 100 concurrent transfers green, with the invariants checked
- [ ] Balance never negative, enforced in the service and in the database
- [ ] 409 `concurrency-conflict` after 3 retries

## Decisions

- **System account outside optimistic locking:** the atomic `UPDATE` serializes deposits only for the time of the row write, instead of forcing retries on all of them.
- **Retry with a new transaction:** repeating inside the same transaction would reuse stale entities from the persistence context.
- **Random wait between attempts:** prevents the same requests from colliding again in sync.

## Open questions

- None.
