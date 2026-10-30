CREATE TABLE accounts (
    id           UUID          PRIMARY KEY,
    user_id      UUID,
    kind         VARCHAR(10)   NOT NULL,
    balance      DECIMAL(19,2) NOT NULL,
    daily_limit  DECIMAL(19,2) NOT NULL,
    version      BIGINT        NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_accounts_user UNIQUE (user_id),
    CONSTRAINT ck_accounts_kind CHECK (kind IN ('USER', 'SYSTEM')),
    -- User accounts belong to exactly one user; the system account (EXTERNAL_FUNDS) belongs to no one.
    CONSTRAINT ck_accounts_owner CHECK ((kind = 'USER' AND user_id IS NOT NULL) OR (kind = 'SYSTEM' AND user_id IS NULL)),
    -- Only the system account may go negative: it is the counterpart of every simulated deposit.
    CONSTRAINT ck_accounts_balance CHECK (kind = 'SYSTEM' OR balance >= 0)
);
