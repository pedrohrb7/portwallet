# Spec: FR-03 Logout

- **Status:** Approved
- **Phase:** 2
- **PRD:** FR-03
- **Depends on:** [fr-02-login](../fr-02-login/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

End the user's session, revoking the current refresh token and clearing the frontend state.

## Scope

**Includes**

- `POST /auth/logout` and the "Sair" ("Sign out") button in the header.

**Later**

- Requiring `X-XSRF-TOKEN` on logout: phase 5.
- Revoking every session of the user: FR-14 (password change).

## Behavior

1. `POST /auth/logout` with a valid refresh cookie revokes that token and clears the cookie (`Max-Age=0`). Response 204.
2. Without cookie, or with an unknown, expired or already revoked token, the response is also 204 and the cookie is cleared: logout is idempotent and reveals nothing.
3. Only the current token is revoked; other sessions of the same user (other families) stay valid.
4. The access token already issued stays valid until it expires (15 min at most). It is the cost of a stateless JWT, accepted by the PRD.
5. In the frontend, "Sair" calls logout, discards the access token from memory, clears the TanStack Query cache and navigates to `/login`, even if the call fails.

## API

### `POST /api/v1/auth/logout`

- **Access:** `refresh_token` cookie (optional)

Response `204` without body, with `Set-Cookie: refresh_token=; Max-Age=0; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Strict`.

## Data model

No migration.

## Backend

- `RefreshTokenService.revoke(rawToken)` and `AuthController.logout`.
- `/auth/logout` permitted in the `SecurityFilterChain`, since the authentication is the cookie itself.

## Frontend

- "Sair" button in `AppLayout` (`data-testid="logout-button"`).
- `session.logout()`.

## Security

- After logout, reloading the page does not restore the session.
- The TanStack Query cache is cleared so the previous user's data does not show up for the next one.

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | Logout revokes the token (the next refresh gives 401); logout without cookie gives 204; another family of the same user stays valid |
| Unit (frontend) | `logout()` clears token and cache even with a network error |
| E2E | Login, sign out, reload the page: stays on `/login` |

## Acceptance criteria

- [ ] Refresh token revoked and cookie cleared
- [ ] Session does not come back on page reload

## Decisions

- **Logout always 204:** there is nothing to tell the client, and an error would only get in the way of signing out.

## Open questions

- None.
