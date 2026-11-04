# Spec: FR-02 Login and refresh

- **Status:** Done
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

`V7__refresh_tokens.sql`

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
- `auth.api.RefreshCookie`: builds and clears the cookie with `ResponseCookie`. `Max-Age` is the time left until the family expires, rounded up to whole seconds, so the milliseconds between issuing the token and writing the response do not cut a second off (a fresh login always gets `Max-Age=604800`).
- `/auth/login` and `/auth/refresh` permitted in the `SecurityFilterChain`.

## Frontend

- `/login` (`features/auth/LoginPage.tsx`): e-mail and password, generic error in `role="alert"` (`data-testid="login-error"`), button disabled while submitting, link to `/register`. Same client rules as `LoginRequest` (`loginSchema`): e-mail trimmed and lowercased, password required and at most 72 bytes, with no other password rule, so a wrong password always reaches the server and gets the generic answer. After an `invalid-credentials` answer the e-mail stays and the password is cleared and focused.
- After signing in, the user goes to the route `RequireAuth` sent them away from (`state.from`), or to `/`. Only paths inside the app are followed (`/...`, never `//...`). A user who is already signed in and opens `/login` goes straight there too.
- `features/auth/session.ts`: `login()` calls `POST /auth/login` through `authClient` (a 401 there is a wrong credential and must not trigger a refresh) and stores the session in `sessionStore`; `restore()` runs the refresh on first load (`RootLayout`) only while the session is still `loading`, sharing the in-flight request of `refreshSession()`.
- `/` follows the session (`routes/HomeRoute.tsx`): while the restore runs it shows the loading state, so a signed-in user never sees the public page flash by; visitors get the public `HomePage` (now with an **Entrar** link next to **Criar conta**); signed-in users get the `AppLayout` with `WelcomePage` ("Olá, {name}"), a placeholder the FR-08 dashboard replaces. `AppLayout` and `PublicLayout` take optional `children` for this route, and `SessionLoading` is shared with `RequireAuth`.
- The `AppLayout` header shows the user's name (`data-testid="current-user"`).

## Security

- The refresh token is never readable by JavaScript nor appears in a response body.
- Identical message for every credential failure.
- BCrypt cost 12 applies to every password.

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | Valid login returns a verifiable JWT and a cookie with the attributes of rule 4; unknown e-mail, wrong password and `PENDING` user return the same body; refresh rotates (the old token stops working); refresh with an expired or revoked token clears the cookie; refresh keeps the family's expiration |
| Unit (frontend) | `session.test.ts`: `login()` stores the token and the user, a failure leaves the session untouched; `restore()` with a 401 leaves the session `anonymous`, sends a single request when called twice and none once the session is known. `LoginPage.test.tsx`: normalized request, redirect to the original route and never outside the app, generic error keeping the e-mail and clearing the password, client validation, disabled button while sending, network error, already signed-in user. `router.test.tsx`: `/` waits for the restore and shows the authenticated home with the name in the header |
| E2E | `login.spec.ts`: login from the home page and reload keeping the session, with nothing readable by JavaScript (`document.cookie` empty, no token in the storages); signed-in user opening `/login`; wrong password and unknown e-mail with the same message; public home without a session. Users come from `e2e/support/users.ts` (`createActiveUser`) |

## Acceptance criteria

- [x] Access token only in memory; refresh only in an httpOnly cookie
- [x] Refresh rotated on every use
- [x] Session restored on page reload

## Decisions

- **Fixed family expiration:** rotation does not extend the session beyond 7 days since login.
- **`Path=/api/v1/auth` on the cookie:** the browser only sends the refresh token to the endpoints that need it.
- **`/` follows the session instead of a separate `/app` route:** the FR-08 dashboard takes `/` for signed-in users, so the URL a user lands on after signing in does not change when it ships; only `WelcomePage` is replaced.

## Open questions

- None.
