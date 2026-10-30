-- Demo inbox of the simulated notification service. Kept in the database, not in memory,
-- so it keeps working with several API instances (phase 7).
CREATE TABLE mock_inbox_messages (
    id          UUID          PRIMARY KEY,
    -- Insertion order, so messages created in the same instant still list deterministically.
    seq         BIGINT        GENERATED ALWAYS AS IDENTITY,
    recipient   VARCHAR(254)  NOT NULL,
    subject     VARCHAR(200)  NOT NULL,
    body        VARCHAR(4000) NOT NULL,
    link        VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_mock_inbox_recipient ON mock_inbox_messages (recipient, seq DESC);
