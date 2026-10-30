-- Baseline migration. The wallet schema (users, accounts, ledger, ...) arrives in phase 2.
-- Keep SQL portable between H2 (MODE=PostgreSQL) and PostgreSQL.
CREATE TABLE app_metadata (
    meta_key   VARCHAR(64)  PRIMARY KEY,
    meta_value VARCHAR(255) NOT NULL
);

INSERT INTO app_metadata (meta_key, meta_value) VALUES ('schema_baseline', '1');
