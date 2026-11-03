# 5. Password set on activation

Date: 2026-11-03

## Status

Accepted. Supersedes the decision "Password on activation against account pre-hijacking" of the [FR-01 spec](../features/fr-01-registration/spec.md), in which activation checked the password chosen at registration.

## Context

Whoever registers someone else's e-mail first must not end up choosing the password of the account its owner activates later (account pre-hijacking). The first fix required, on activation, the same password as the latest registration. It was secure, but the password traveled twice, stayed stored before the e-mail was verified, activation became a second route that checks passwords (with its own rate limit and log care) and there was an error just for it (`wrong-password`).

## Decision

- Registration asks only for name, e-mail and CPF. `PENDING` users have no password.
- The password is **set** on activation, by whoever holds the link sent to the e-mail. Activation never checks a password; `wrong-password` no longer exists.
- The database enforces the invariant: `CHECK (status = 'PENDING' OR password_hash IS NOT NULL)`. Pending registrations that already had a password lose it in the migration.
- Registering a `PENDING` e-mail again still replaces name and CPF and invalidates the previous links. So that the owner of the e-mail notices a swap made by someone else, the confirmation e-mail shows the name and the end of the CPF, and the activation screen shows a masked summary (`POST /auth/confirm/summary`, token in the body) before asking for the password.

## Consequences

- Access to the account is protected with no route other than login checking passwords.
- The integrity of the data (name and CPF) depends on the owner checking the summary. The alternative without that dependency, one record per registration submission tied to its own token, is documented in the [refactor](../backlog/refactor/2026-10-30-password-on-activation.md) in case the risk proves relevant.
- PRD FR-01 changed: the password left the registration.
- The name mask (`shared.privacy.NameMasker`) was brought forward from FR-17 and moved to `shared`, because both `auth` and `account` use it.
