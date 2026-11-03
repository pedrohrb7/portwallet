-- The password is now chosen when the account is activated, not at registration.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Pending registrations keep no password chosen before the e-mail was verified.
UPDATE users SET password_hash = NULL WHERE status = 'PENDING';

ALTER TABLE users ADD CONSTRAINT ck_users_password_when_active
    CHECK (status = 'PENDING' OR password_hash IS NOT NULL);
