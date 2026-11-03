# Password set on activation, instead of checked on activation

- **Status:** Done (2026-11-03, no PR: local work, not committed yet)
- **Opened:** 2026-10-30
- **Decision:** [ADR 0005](../../adr/0005-password-set-on-activation.md)
- **Scope:** backend `auth` (registration, confirmation, `User` entity, e-mails), `shared.openapi`, new migration on `users`; frontend `features/auth` (`/register`, `/confirm`) and `api/`; integration, unit and E2E tests of the registration; `docs/features/fr-01-registration`, `docs/conventions.md`; PRD (FR-01)

## Motivation

The account pre-hijacking fix in FR-01 is secure, but it has costs that a simpler model avoids:

- The password travels twice: at registration and again at activation.
- A password chosen before the e-mail is verified stays stored, even if the account is never activated.
- Activation is one more route that checks a password, besides login, so it needs its own rate limit (phase 5) and care with logs.
- The `wrong-password` error only exists because of this design.

If the password is set on activation, only whoever holds the link (and therefore has access to the e-mail) chooses the password. Pre-hijacking the access stays impossible, with no route other than login checking passwords.

## Current state

- `POST /api/v1/auth/register` takes name, e-mail, CPF and password. The user stays `PENDING` with the BCrypt hash of the password.
- Registering a `PENDING` e-mail again replaces name, CPF and password with the new data and invalidates the previous links (except when the new CPF belongs to another user).
- `POST /api/v1/auth/confirm` takes token and password, checks the password against the latest registration and, if it matches, activates the user and opens the account. A different password returns 422 `wrong-password` without consuming the token.
- The `/confirm` screen asks for the registration password.

Details in [`docs/features/fr-01-registration/spec.md`](../../features/fr-01-registration/spec.md), rules 3 and 6 to 9 (before this refactor).

## Proposed change

**Business rules**

1. Registration asks only for name, e-mail and CPF. The four FR-01 response cases stay, with the same 202, and registering a `PENDING` e-mail again still replaces name and CPF and invalidates the previous links.
2. `PENDING` users have no password. Every `ACTIVE` or `BLOCKED` user has one.
3. The confirmation e-mail shows the name and the last two CPF digits of the registration.
4. The activation screen shows a summary of the registration (masked name and CPF) and asks "Estes são os seus dados?" ("Is this your data?"). If it is not, it tells the user to register again, which issues a new link.
5. On activation, the user **sets** the password (8 to 72 characters, up to 72 bytes, with a confirmation field in the client). The password is stored, the user is activated, the token is consumed and the account is opened, in the same transaction.
6. Activation never checks a password; `wrong-password` disappears. An invalid token is still 400 `invalid-token`.
7. Login of a `PENDING` user still fails with `invalid-credentials`.

**Why the summary (rules 3 and 4).** Without the password at registration, access is protected, but the data is not: an attacker who registered the victim's e-mail again between the victim's registration and the click would make the victim activate an account with the attacker's name and CPF. Since only the latest link works, the summary before activation lets the victim notice the swap. The CPF is still checked silently at registration, so the summary opens no enumeration: it only shows to whoever has the link, and only with the final digits.

**API**

- `POST /api/v1/auth/register`: `{ "name", "email", "document" }`, 202 response as today.
- `POST /api/v1/auth/confirm/summary` (new): `{ "token" }` in the body, not in the URL, so it stays out of access logs. Response 200 `{ "maskedName": "Ana S****", "maskedDocument": "***.***.***-25" }`; 400 `invalid-token`.
- `POST /api/v1/auth/confirm`: `{ "token", "password" }`, now stores the password. Errors: 400 `validation-error` and 400 `invalid-token`.

**Data.** New migration (number chosen during implementation):

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD CONSTRAINT ck_users_password_when_active
    CHECK (status = 'PENDING' OR password_hash IS NOT NULL);
```

**Code**

- Backend: `RegisterRequest`, `RegisterCommand`, `User.register` and `User.replacePendingRegistration` without password; `User.activate(passwordHash)`; `ConfirmationService.summary` and `confirm` without `PasswordEncoder.matches`; `AuthEmails.confirmation` with name and CPF ending; `NameMasker` brought forward from FR-17; removal of `WrongPasswordException` and of the `wrong-password` `@ApiProblem`.
- Frontend: `/register` without password; `/confirm` with summary, new password, password confirmation and a link to register again; removal of `wrong-password` from `problem.ts`.

**Rejected alternatives**

- *Keep the password checked on activation (current state):* secure, but with the costs listed under Motivation.
- *Tie each registration submission to its own token:* would solve the name and CPF swap without relying on the user's attention, but would need a new table of pending registrations. The summary covers the case with less structure.
- *Registration with e-mail only (name and CPF also on activation):* the CPF check would move to activation and could not be silent, letting anyone with their own e-mail find out whether a CPF is registered.

**Implementation order**

1. Migration and a test that the `CHECK` prevents `ACTIVE` without password.
2. Registration without password and e-mail with the summary (`RegistrationTest`).
3. Summary and activation setting the password; removal of `wrong-password` from code, from the catalog in `docs/conventions.md` and from OpenAPI (`ConfirmationTest`, pre-hijacking scenarios rewritten: the attacker gets in under no registration order, and the data swap shows in the summary).
4. Frontend (`RegisterPage.test.tsx`, `ConfirmPage.test.tsx`, `schemas.test.ts`).
5. E2E: registration without password, summary, setting the password, link used twice, registering again changes the summary.
6. Documentation: ADR 0005, FR-01 spec and plan updated (the previous fix recorded as superseded), Swagger reviewed.

**Verification:** `./mvnw verify`, `npm run lint && npm run typecheck && npm test`, `npm run e2e` in Chromium and Firefox, registration and activation screens reviewed on desktop and mobile, no occurrence of `wrong-password` in code or documentation.

## Risks

- **PRD change:** FR-01 no longer asks for a password at registration. Needs approval before starting.
- **Existing `PENDING` users with a password:** stay valid; activation replaces the hash, with no data migration.
- **FR-02 dependency:** login assumes every active user has a password. The migration's `CHECK` guarantees it and must exist before FR-02. Running this refactor before FR-02 avoids redoing the login tests because of the `/auth/confirm` contract change.
- **Name and CPF swap depends on the user's attention to the summary:** mitigated by the summary also being in the e-mail; the alternative without this dependency is recorded above, in case the risk proves relevant.

## Out of scope

- Rate limit on activation and registration (phase 5).
- Password recovery (FR-14).
- Any change to the login flow beyond `PENDING` users without password.

## Outcome

Implemented on 2026-11-03, in the proposed order. Differences from the proposal:

- **Migration `V5__password_set_on_activation.sql`** also clears the hash of existing `PENDING` users, instead of keeping it until activation: this way rule 2 holds for all data and no password chosen before e-mail verification stays stored. As a consequence, FR-02's `refresh_tokens` became `V6` and the ledger, `V7`.
- **`NameMasker` in `shared.privacy`**, not in `account.domain` as FR-17 planned: both `auth` and `account` use it. The CPF mask became `Cpf.masked()`.
- **Bug found by the E2E tests and fixed:** opening a second confirmation link in the same tab where `/confirm` was already open only changes the URL fragment, without reloading the page, and the screen kept showing the result of the previous link. The screen now listens to `hashchange` and starts over with the new token.
- The 422 example of the OpenAPI contract test (`OpenApiDocsContract`) now uses `insufficient-balance`.
