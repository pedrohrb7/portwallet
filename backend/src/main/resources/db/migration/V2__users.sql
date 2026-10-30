CREATE TABLE users (
    id               UUID         PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    email            VARCHAR(254) NOT NULL,
    password_hash    VARCHAR(60)  NOT NULL,
    document_enc     VARCHAR(255) NOT NULL,
    document_hash    VARCHAR(64)  NOT NULL,
    totp_secret_enc  VARCHAR(255),
    totp_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    role             VARCHAR(10)  NOT NULL,
    status           VARCHAR(10)  NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_document_hash UNIQUE (document_hash),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED'))
);

CREATE TABLE user_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL,
    purpose     VARCHAR(20) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_user_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_user_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_user_tokens_purpose CHECK (purpose IN ('EMAIL_CONFIRMATION', 'PASSWORD_RESET'))
);

CREATE INDEX ix_user_tokens_user_purpose ON user_tokens (user_id, purpose);
