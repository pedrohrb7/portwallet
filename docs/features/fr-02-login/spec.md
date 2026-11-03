# Spec: FR-02 Login and refresh

- **Status:** Approved
- **Phase:** 2
- **PRD:** FR-02; Security; US-02
- **Depends on:** [fr-01-registration](../fr-01-registration/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Authenticate the user with e-mail and password and keep them signed in securely: a short-lived access token in memory and a rotating refresh token in an httpOnly cookie.

## Scope

**Includes**

- `POST /auth/login` and `POST /auth/refresh`.
- `refresh_tokens` table with token families.
- `/login` screen and session restore on page reload.

**Later**

- Revoking the whole family when an old token is reused: phase 5. The `family_id` column is created here already.
- Double-submit CSRF (`XSRF-TOKEN`) on refresh: phase 5.
- Login rate limit and countdown on 429: phase 5.
- BCrypt against a dummy hash to equalize response time: phase 5.
- Shortcuts to demo accounts on the login screen: FR-13.

## Behavior

1. Login with valid credentials of an `ACTIVE` or `BLOCKED` user returns 200 with the access token (JWT, 15 min) and sets the refresh cookie. A blocked user signs in, but cannot move money (FR-05).
2. Unknown e-mail, wrong password or `PENDING` user (who has no password, see FR-01): 401 `invalid-credentials`, with the same message, "E-mail ou senha inválidos" ("Invalid e-mail or password").
3. The refresh token has 32 random bytes in Base64URL and is valid for 7 days. Only its SHA-256 hash is stored. Each login starts a new family (`family_id`).
4. Cookie: name `refresh_token`, `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`, `Max-Age` of 7 days.
5. `POST /auth/refresh` with a valid, non-revoked token revokes that token, issues a new one in the same family (with the family's original expiration, without extending it) and returns a new access token. Revocation and issuing happen in the same transaction.
6. Refresh without cookie, or with an unknown, expired or revoked token: 401 `invalid-refresh-token`, and the cookie is cleared (`Max-Age=0`).
7. Refresh of a user who is no longer `ACTIVE` nor `BLOCKED`: 401 `invalid-refresh-token`.
8. The frontend calls `/auth/refresh` on load; on success, the session is restored without asking for the password. Several calls that get a 401 at the same time trigger a single refresh (see [nfr-application-base](../nfr-application-base/spec.md)).
9. After login, the user goes back to the route they tried to access, or to `/`.

## API

### `POST /api/v1/auth/login`

- **Access:** Public

```json
{ "email": "ana@demo.com", "password": "s3nh@forte" }
```

Response `200` (plus the header `Set-Cookie: refresh_token=...`):

```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresIn": 900,
  "user": { "id": "1b7e...", "name": "Ana Souza", "email": "ana@demo.com", "role": "USER" }
}
```

| Status | `type` | When |
| --- | --- | --- |
| 400 | `validation-error` | Missing or malformed fields |
| 401 | `invalid-credentials` | Rule 2 |

### `POST /api/v1/auth/refresh`

- **Access:** `refresh_token` cookie

Response `200`: same body as login, with a new cookie.

| Status | `type` | When |
| --- | --- | --- |
| 401 | `invalid-refresh-token` | Rules 6 and 7 |

## Data model

`V6__refresh_tokens.sql`

```sql
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    family_id   UUID        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
```

## Backend

- `auth.domain.RefreshToken` and repository.
- `auth.application.LoginService`: checks the password with `PasswordEncoder` and issues the token pair. A `PENDING` user has no password hash (`User.passwordHash()` is empty) and always fails.
- `auth.application.RefreshTokenService`: issuing, rotation and revocation.
- `auth.api.RefreshCookie`: builds and clears the cookie with `ResponseCookie`.
- `/auth/login` and `/auth/refresh` permitted in the `SecurityFilterChain`.

## Frontend

- `/login`: e-mail and password, generic error in `role="alert"` (`data-testid="login-error"`), button disabled while submitting, link to `/register`.
- `session.ts`: `login()`, `restore()` (refresh on load) and the current user state (`name`, `role`).
- The `AppLayout` header shows the user's name.

## Security

- The refresh token is never readable by JavaScript nor appears in a response body.
- Identical message for every credential failure.
- BCrypt cost 12 applies to every password.

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | Valid login returns a verifiable JWT and a cookie with the attributes of rule 4; unknown e-mail, wrong password and `PENDING` user return the same body; refresh rotates (the old token stops working); refresh with an expired or revoked token clears the cookie; refresh keeps the family's expiration |
| Unit (frontend) | `restore()` with a 401 leaves the session `anonymous`; redirect to the original route |
| E2E | PRD scenario 1 (login part); reloading the page keeps the user signed in |

## Acceptance criteria

- [ ] Access token only in memory; refresh only in an httpOnly cookie
- [ ] Refresh rotated on every use
- [ ] Session restored on page reload

## Decisions

- **Fixed family expiration:** rotation does not extend the session beyond 7 days since login.
- **`Path=/api/v1/auth` on the cookie:** the browser only sends the refresh token to the endpoints that need it.

## Open questions

- None.
