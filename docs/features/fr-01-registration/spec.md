# Spec: FR-01 Registration and confirmation

- **Status:** Done
- **Phase:** 2
- **PRD:** FR-01; Advanced security (anti-enumeration); US-01
- **Depends on:** [nfr-application-base](../nfr-application-base/spec.md)
- **Plan:** [plan.md](plan.md)
- **Manual:** [manual.md](manual.md)

## Goal

Let a person create an account with name, e-mail and a fictitious CPF, and activate it through the link sent by e-mail, setting the password at that moment, without the response revealing whether the e-mail was already registered.

## Scope

**Includes**

- `POST /auth/register`, `POST /auth/confirm/summary` and `POST /auth/confirm`.
- Tables `users`, `user_tokens` and `accounts`; the account is created on confirmation.
- CPF encrypted with AES-256-GCM and an HMAC-SHA256 blind index from the start.
- Simulated notification service, called in process, and demo inbox (`/mock/inbox`) in the `dev`, `e2e` and `demo` profiles.
- Screens `/register`, `/confirm` and `/inbox`.

**Later**

- Rate limit on registration (3 per hour per IP) and on activation: phase 5.
- Equalizing response time between new and existing e-mails: phase 5.
- Notification over HTTP with circuit breaker and outbox: phase 4. The `NotificationGateway` interface created here is the swap point.
- Audit log entry: phase 5.

## Behavior

1. Name with 2 to 100 characters; valid e-mail, normalized to lower case and trimmed; CPF with 11 digits (masked or not) and valid verification digits. Registration does **not** ask for a password; a `password` field sent is ignored.
2. Invalid data: 400 `validation-error`. Format validation does not depend on the database, so it reveals nothing.
3. With valid data, the response is **always** `202` with the same body, in every case below:
   - **New e-mail and CPF:** creates the `PENDING` user, without password, and sends the confirmation e-mail.
   - **E-mail of a `PENDING` user:** creates no other user; **replaces** name and CPF of the pending registration with the new data, invalidates the previous confirmation tokens and sends a new link. If the new CPF belongs to another user, the repeated CPF case below applies and the pending registration does not change.
   - **E-mail of an `ACTIVE` or `BLOCKED` user:** creates nothing; sends the owner a warning that someone tried to register their e-mail.
   - **CPF already used by another user:** creates nothing and sends nothing to the e-mail given; sends the CPF owner the same registration attempt warning.
4. The confirmation token has 32 random bytes in Base64URL, is valid for 24 h and is used only once. Only its SHA-256 hash is stored.
5. The confirmation link is `<APP_BASE_URL>/confirm#token=<token>`. The token goes in the fragment (`#`), which is not sent to the server nor written to access logs. The confirmation e-mail shows the full name and the masked CPF (`***.***.***-25`) of the registration, and tells the user to register again if the data is not theirs.
6. `PENDING` users have no password; every `ACTIVE` or `BLOCKED` user has one. The database enforces it with `ck_users_password_when_active`.
7. `POST /auth/confirm/summary` takes the token **in the body** and, if it is valid, returns the masked name (first name and initial of the last surname, `shared.privacy.NameMasker`) and the masked CPF of the registration. It does not consume the token.
8. `POST /auth/confirm` takes the token and the **new** password (8 to 72 characters, up to 72 bytes in UTF-8, because BCrypt ignores the rest). With a valid token, it stores the password with BCrypt cost 12, activates the user, marks the token as used and creates the account with balance 0.00 and a daily limit of R$ 5,000.00, all in the same transaction. Response 200. Activation never checks a password.
9. Unknown, expired or already used token, in the summary or the activation: 400 `invalid-token`, with the same message in all three cases. Password outside the rules: 400 `validation-error`, without consuming the token.
10. The `/confirm` screen reads the token from the fragment and removes it from the URL with `history.replaceState` on open; fetches the summary and asks "Estes são os seus dados?" ("Is this your data?"), with a link to register again; asks for the password and its repetition (checked only in the client); shows success (with a link to login) or a link error. A new link opened in the same tab (only the fragment changes) restarts the flow with the new token.
11. The `/register` screen validates in real time with Zod (same rules as the backend) and, on submit, always shows the same message: "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta." ("If the data is correct, you will receive an e-mail to activate the account.")

Together, rules 3, 6 and 8 prevent **account pre-hijacking**: only whoever controls the e-mail chooses the password, so registering someone else's e-mail first gives no access to the account. A swap of name and CPF by someone who registers the e-mail again between the owner's registration and click shows up in the e-mail and in the summary of the latest link (rules 5 and 7), and the owner registers again ([ADR 0005](../../adr/0005-password-set-on-activation.md)).

## API

### `POST /api/v1/auth/register`

- **Access:** Public

```json
{ "name": "Ana Souza", "email": "ana@demo.com", "document": "529.982.247-25" }
```

Response `202`:

```json
{ "message": "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta." }
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Invalid format |

### `POST /api/v1/auth/confirm/summary`

- **Access:** Public

```json
{ "token": "q2Vx..." }
```

Response `200`:

```json
{ "maskedName": "Ana S****", "maskedDocument": "***.***.***-25" }
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Missing token |
| 400 | `invalid-token` | Unknown, expired or already used token |

### `POST /api/v1/auth/confirm`

- **Access:** Public

```json
{ "token": "q2Vx...", "password": "s3nh@forte" }
```

Response `200`:

```json
{ "status": "ACTIVE" }
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Missing token or password, password shorter than 8 characters or over 72 bytes |
| 400 | `invalid-token` | Unknown, expired or already used token |

### `GET /mock/inbox`

- **Access:** Public, only where `portwallet.mock.inbox.enabled=true` (profiles `dev`, `e2e`, `demo` and `test`). Elsewhere the controller does not even exist and the route answers 404 `not-found`.
- **Query:** `to` (valid e-mail, required; case-insensitive)

```json
[
  {
    "id": "8d0c...",
    "to": "ana@demo.com",
    "subject": "Ative sua conta PortWallet",
    "body": "Olá, Ana! Recebemos um cadastro na PortWallet com estes dados: ...",
    "link": "http://localhost:5173/confirm#token=q2Vx...",
    "createdAt": "2026-10-30T15:00:00Z"
  }
]
```

Newest first. The `link` field exists so tests and the screen do not need to extract the URL from the text.

## Data model

`V2__users.sql`

```sql
CREATE TABLE users (
    id                UUID         PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    email             VARCHAR(254) NOT NULL UNIQUE,
    password_hash     VARCHAR(60)  NOT NULL,
    document_enc      VARCHAR(255) NOT NULL,
    document_hash     VARCHAR(64)  NOT NULL UNIQUE,
    totp_secret_enc   VARCHAR(255),
    totp_enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    role              VARCHAR(10)  NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    status            VARCHAR(10)  NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED')),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    purpose     VARCHAR(20) NOT NULL CHECK (purpose IN ('EMAIL_CONFIRMATION', 'PASSWORD_RESET')),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_user_tokens_user_purpose ON user_tokens (user_id, purpose);
```

`V3__accounts.sql`

```sql
CREATE TABLE accounts (
    id           UUID          PRIMARY KEY,
    user_id      UUID          UNIQUE REFERENCES users (id),
    kind         VARCHAR(10)   NOT NULL CHECK (kind IN ('USER', 'SYSTEM')),
    balance      DECIMAL(19,2) NOT NULL,
    daily_limit  DECIMAL(19,2) NOT NULL,
    version      BIGINT        NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_accounts_owner CHECK ((kind = 'USER' AND user_id IS NOT NULL) OR (kind = 'SYSTEM' AND user_id IS NULL)),
    CONSTRAINT ck_accounts_balance CHECK (kind = 'SYSTEM' OR balance >= 0)
);
```

The `EXTERNAL_FUNDS` system account and the use of `balance` and `version` are defined in [nfr-ledger-concurrency](../nfr-ledger-concurrency/spec.md).

`V4__mock_inbox.sql`

```sql
CREATE TABLE mock_inbox_messages (
    id          UUID          PRIMARY KEY,
    seq         BIGINT        GENERATED ALWAYS AS IDENTITY,
    recipient   VARCHAR(254)  NOT NULL,
    subject     VARCHAR(200)  NOT NULL,
    body        VARCHAR(4000) NOT NULL,
    link        VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_mock_inbox_recipient ON mock_inbox_messages (recipient, seq DESC);
```

The inbox lives in the database, not in memory, so it keeps working with several instances in phase 7. Ordering uses the `seq` identity column, not `created_at`: two messages at the same instant would have an undefined order.

`V5__password_set_on_activation.sql`

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
UPDATE users SET password_hash = NULL WHERE status = 'PENDING';
ALTER TABLE users ADD CONSTRAINT ck_users_password_when_active
    CHECK (status = 'PENDING' OR password_hash IS NOT NULL);
```

Migrations use named constraints (`uq_users_email`, `ck_accounts_balance` and so on), so database error messages are recognizable.

## Backend

- `auth.domain`: `User`, `UserStatus`, `UserToken`, `TokenPurpose`, `Cpf` (value with normalization and digit validation, which never prints the full number). `Role` lives in `shared.security`, created in the application base.
- `auth.application.RegistrationService`: rules 3 and 4. Runs in a `TransactionTemplate`; if two simultaneous registrations compete for the same e-mail or CPF, the loser's uniqueness violation is absorbed and the response stays the same.
- `auth.application.ConfirmationService`: rules 6 to 9. `summary` returns the masked summary without consuming the token; `confirm` consumes the token, stores the password with the `PasswordEncoder` (`User.activate(passwordHash)`) and creates the account through `account.application.AccountProvisioning` (`Propagation.MANDATORY`, so there is never an active user without an account). The token is consumed with `UPDATE ... WHERE used_at IS NULL`, which guarantees single use even with simultaneous confirmations.
- `auth.application.AuthEmails`: texts of the confirmation e-mail (with name and masked CPF) and of the registration attempt warning (a single warning for repeated e-mail and CPF).
- `auth.application.PasswordConfig`: `BCryptPasswordEncoder` with cost 12.
- `auth.api`: `AuthController`, `RegisterRequest` (normalizes name, e-mail and CPF before validation), `@ValidCpf`. The activation password (`ConfirmRequest`) is also limited to 72 bytes in UTF-8 by `shared.validation.MaxUtf8Bytes`, because BCrypt ignores anything beyond that and an accented character takes two bytes.
- `shared.crypto`: `FieldEncryptor` (AES-256-GCM, random 12-byte IV, output `Base64(iv || ciphertext || tag)`), `BlindIndex` (HMAC-SHA256 in hex), `EncryptedStringConverter` (used on `document_enc` and, in phase 6, on `totp_secret_enc`) and `SecureTokens` (Base64URL generation and SHA-256 hash). Keys in `PORTWALLET_CRYPTO_ENCRYPTION_KEY` and `PORTWALLET_CRYPTO_BLIND_INDEX_KEY` (32 bytes in Base64, different from each other), validated by `CryptoProperties`: the application does not start without them. There are development values in the `dev`, `e2e` and `test` profiles, and `docker-compose.yml` passes demo values.
- `shared.privacy.NameMasker` (rule 7; also used by FR-17) and `Cpf.masked()`.
- `shared.money.MoneyConverter`: `AttributeConverter` from `Money` to `DECIMAL(19,2)`, used in `Account`.
- `shared.notification.NotificationGateway` with `send(Email)`. With `portwallet.mock.inbox.enabled=true`, the implementation is `mock.inbox.InboxNotificationGateway`, which writes to `mock_inbox_messages` through `InboxStore`. Without the property, `LoggingNotificationGateway` only logs the subject and the masked recipient (`shared.logging.Masking`).
- `mock.inbox.InboxController`, conditional on the same property.
- `portwallet.app-base-url` (`APP_BASE_URL`, default `http://localhost:5173`; in `docker compose`, `http://localhost:3000`) to build the links.

## Frontend

- `/register` (`RegisterPage`): form with `name`, `email` and `document` (CPF mask applied while typing), without password; validation when leaving each field and then on every change; field errors with `aria-invalid` and `aria-describedby`; field errors returned by the API show on the field; button disabled while submitting; `data-testid="register-success"` on the final message, with a link to the demo inbox when it exists.
- `/confirm` (`ConfirmPage`): loads the summary (`data-testid="confirm-summary"`, with retry on network failure), form "Crie uma senha" and "Repita a senha" ("Create a password", "Repeat the password"; `activationSchema`), success (`data-testid="confirm-success"`) and link error (`data-testid="confirm-error"`, without form). The token is read on the first render (a pure read, safe under Strict Mode), stays only in memory and leaves the URL in an effect when the page opens; a `hashchange` with a new token remounts the flow.
- `/inbox` (`InboxPage`): e-mail field (`data-testid="inbox-email"`), searched e-mail reflected in the URL (`/inbox?to=...`) and message list (`data-testid="inbox-message"`) with subject, date in São Paulo, body and link. The route only exists when `VITE_DEMO_INBOX=true` (`.env.development` and the `docker compose` build), and uses a wider, top-aligned layout (`PublicWideLayout`).
- Home screen with links to create an account and to the demo inbox.
- `src/features/auth/schemas.ts`: Zod schemas for registration and activation (same rules as the backend, including the 72 password bytes, and matching passwords). `src/lib/cpf.ts`: CPF validation and mask, tested with the same cases as the backend.
- Shared components: `TextField`, `Button` (disabled while submitting) and `Alert`.
- `/mock` proxy in Vite and Nginx.

## Security

- The registration response does not change with the state of the e-mail or CPF (rule 3).
- The password travels only once, on activation, and is only stored after the owner of the e-mail opens the link.
- The summary token goes in the request body, not in the URL.
- Logs never show password, token or CPF; e-mails appear masked (`a***@demo.com`).
- The plain CPF only exists in memory during the request.

## Tests

| Layer | Scenario |
| --- | --- |
| Unit | `CpfTest`: masked and unmasked, wrong digits, repeated sequences, partial mask, generated CPFs, masked form. `CryptoTest`: different ciphertexts for the same text, round trip, tampering and wrong key rejected, deterministic and key-dependent blind index, tokens, key validation and app that does not start without them. `NameMaskerTest`: one, two and three words, extra spaces, accented initial |
| Integration | `RegistrationTest`: the four cases of rule 3 with identical body and exactly the expected e-mail (with name and masked CPF); pending user without password, even if one is sent; encrypted CPF; normalized e-mail; old link invalidated on resend; pre-hijacking (attacker registers before the victim and gets no access, and their CPF is released; attacker registers again later and their data shows in the latest link's summary until the victim registers again); pending registration untouched when the new CPF belongs to another user; field validation. `ConfirmationTest`: masked summary without consuming the link; summary of an invalid or used link; activation stores the password with BCrypt cost 12 and opens the account with balance 0.00 and limit 5,000.00; reused token does not change the password; expired and unknown token give `invalid-token`; short password or over 72 bytes gives `validation-error` without consuming the link; missing fields; the database refuses an active user without password. `InboxControllerTest`: list by recipient, newest first, and 404 with the inbox off |
| Unit (frontend) | `cpf.test.ts` (same cases as the backend), `schemas.test.ts` (registration and activation), `RegisterPage.test.tsx` (mask, normalized data, no password field, local validation, API field error, network failure), `ConfirmPage.test.tsx` under Strict Mode (token removed from the URL on open, summary, activation with the new password, different passwords and short password without calling the API, invalid link in summary and activation, new link in the same tab, retry after network failure, no token) |
| E2E | `register.spec.ts`: registration through the UI without password, e-mail with the data, link through the demo inbox, masked summary and setting the password, without the token in the address bar; link used twice; registering again changes the latest link's summary and invalidates the previous one; local validation of the registration and of the password repetition without calling the API |

## Acceptance criteria

- [x] Identical registration response in every case
- [x] CPF encrypted and unique through the blind index
- [x] Account created only on confirmation
- [x] Full flow through the demo inbox in the E2E tests

## Decisions

- **CPF encryption brought forward from phase 6:** the `document_hash` column is needed for uniqueness and for the recipient lookup (FR-17) in this phase, and storing the plain CPF now would require migrating data later.
- **Account created on confirmation:** `PENDING` users do not show up in lookups nor have a balance, which simplifies the destination rules.
- **Token in the URL fragment:** it does not appear in server logs nor in the `Referer` header.
- **`demo` profile with inbox:** `docker compose` starts in the `demo` profile, so whoever evaluates the project can confirm a registration. The PRD planned the inbox only in `dev` and `e2e`; the exception applies only to the local demo.
- **Inbox enabled by property, not by profile:** `portwallet.mock.inbox.enabled` keeps the decision in one place, and tests can turn it off without switching profiles.
- **Cryptography in `shared.crypto`, not in `auth.infra.crypto`:** the `User` entity, in `domain`, needs the converter, and ArchUnit forbids `domain` from depending on `infra`. The package will also be used by TOTP in phase 6.
- **A single registration attempt warning:** repeated e-mail and CPF produce the same text, which does not say which data was used.
- **Password on activation against account pre-hijacking (superseded):** the first fix required, on activation, the password of the latest registration (422 `wrong-password`). It was replaced by the password set on activation, with a summary of the data before activating ([ADR 0005](../../adr/0005-password-set-on-activation.md)).

## Open questions

- None.
