-- Language of the e-mails sent to the user, the one the registration was made in (docs/features/nfr-i18n).
-- Users registered before this migration get the default language.
ALTER TABLE users ADD COLUMN language VARCHAR(5) DEFAULT 'PT_BR' NOT NULL;

ALTER TABLE users ADD CONSTRAINT ck_users_language CHECK (language IN ('PT_BR', 'EN', 'ES', 'FR'));
