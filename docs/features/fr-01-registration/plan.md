# Plan: FR-01 Registration and confirmation

- **Status:** Done
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [nfr-application-base](../nfr-application-base/plan.md)

## Steps

### 1. Migrations

- [x] `V2__users.sql`, `V3__accounts.sql`, `V4__mock_inbox.sql`
- **Files:** `backend/src/main/resources/db/migration/`
- **Validation:** `./mvnw verify` (`ddl-auto=validate` fails if entity and schema diverge)

### 2. Field encryption

- [x] Validated `CryptoProperties` (32-byte keys), `FieldEncryptor`, `BlindIndex`, `EncryptedStringConverter`
- [x] Development keys in the `dev`, `e2e` and `test` profiles; variables in `docker-compose.yml`
- **Files:** `shared/crypto/**`, `application.yml`, `src/test/resources/application-test.yml`, `docker-compose.yml`
- **Validation:** `CryptoTest`

### 3. Domain

- [x] `Cpf`, `User`, `UserToken`, enums, repositories
- **Files:** `auth/domain/**`, `auth/infra/**`
- **Validation:** `CpfTest`

### 4. Notification and inbox

- [x] `NotificationGateway`, `InboxNotificationGateway` (`dev`, `e2e`), `LoggingNotificationGateway` (other profiles)
- [x] `InboxController` and e-mail templates (confirmation and registration attempt warning)
- **Files:** `shared/notification/**`, `mock/inbox/**`
- **Validation:** `InboxControllerTest` (list by recipient; 404 with `portwallet.mock.inbox.enabled=false`)

### 5. Registration

- [x] `RegisterRequest` with Bean Validation and the `@ValidCpf` validator
- [x] `RegistrationService` with the four cases and `AuthController.register`
- **Files:** `auth/api/**`, `auth/application/RegistrationService.java`
- **Validation:** `RegistrationTest` with the four cases and identical body; CPF encrypted in the database

### 6. Confirmation

- [x] `AccountProvisioning` in the `account` module (creates the `USER` account with zero balance and the default limit)
- [x] `ConfirmationService` and `AuthController.confirm`
- **Files:** `account/application/**`, `account/domain/Account.java`, `auth/application/ConfirmationService.java`
- **Validation:** `ConfirmationTest` (activation and account created; reused, expired and unknown token)

### 7. Frontend

- [x] `schemas.ts` with CPF validation
- [x] Pages `/register`, `/confirm`, `/inbox` and `VITE_DEMO_INBOX` in `.env.development`
- **Files:** `frontend/src/features/auth/**`, `frontend/src/features/inbox/**`, `frontend/src/routes/router.tsx`
- **Validation:** `cpf.test.ts`, `schemas.test.ts`, `RegisterPage.test.tsx`, `ConfirmPage.test.tsx`

### 8. E2E

- [x] Helpers in `e2e/support/`: `uniqueEmail()`, `validCpf()`, `linkFromInbox(request, email, subject)` reading `/mock/inbox`
- [x] `e2e/register.spec.ts`: registration, confirmation through the inbox link
- **Validation:** `npm run e2e` green in Chromium and Firefox

### 9. Security fix: account pre-hijacking (superseded by step 10)

Raised by an automated security review after step 8.

- [x] Registering a `PENDING` e-mail again replaces name, CPF and password (`User.replacePendingRegistration`), except when the new CPF belongs to another user
- [x] `/auth/confirm` requires the password of the latest registration (`WrongPasswordException`, 422 `wrong-password`), checked before consuming the token
- [x] `/confirm` screen with a password form
- **Files:** `auth/domain/User.java`, `auth/application/RegistrationService.java`, `auth/application/ConfirmationService.java`, `auth/application/WrongPasswordException.java`, `auth/api/AuthController.java`, `frontend/src/features/auth/ConfirmPage.tsx`, `frontend/src/api/**`, `docs/conventions.md`
- **Validation:** `RegistrationTest` (attacker before and after the victim; CPF of another user), `ConfirmationTest` (wrong password keeps the link), `ConfirmPage.test.tsx`, E2E scenario "activation requires the password of the latest registration"

### 10. Password set on activation (replaces the check from step 9)

Refactor [2026-10-30-password-on-activation](../../backlog/refactor/2026-10-30-password-on-activation.md), [ADR 0005](../../adr/0005-password-set-on-activation.md).

- [x] `V5__password_set_on_activation.sql` with `ck_users_password_when_active`
- [x] Registration without password; confirmation e-mail with name and masked CPF
- [x] `POST /auth/confirm/summary`; `/auth/confirm` sets the password; removal of `WrongPasswordException` and `wrong-password`
- [x] `shared.privacy.NameMasker` and `Cpf.masked()`
- [x] `/register` screen without password; `/confirm` with summary, new password and repetition; a new link in the same tab restarts the flow
- [x] User manual ([manual.md](manual.md))
- **Validation:** `RegistrationTest`, `ConfirmationTest`, `NameMaskerTest`, `CpfTest`, `schemas.test.ts`, `RegisterPage.test.tsx`, `ConfirmPage.test.tsx`, `register.spec.ts`

## Final verification

- [x] `./mvnw verify` green
- [x] `npm run lint && npm run typecheck && npm test` green
- [x] E2E green in Chromium and Firefox
- [x] Screens reviewed on desktop and mobile
- [x] `demo` profile with inbox in `docker compose`, recorded in the spec decisions
- [x] Spec updated with what changed during implementation

## Risks

- Divergence between the frontend and backend CPF validators: both use the same list of test cases.
