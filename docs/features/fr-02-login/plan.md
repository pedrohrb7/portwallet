# Plan: FR-02 Login and refresh

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [fr-01-registration](../fr-01-registration/plan.md)

## Steps

### 1. Migration and domain

- [ ] `V6__refresh_tokens.sql`, `RefreshToken` entity and repository
- **Files:** `db/migration/V6__refresh_tokens.sql`, `auth/domain/RefreshToken.java`, `auth/infra/**`
- **Validation:** `./mvnw verify`

### 2. Login

- [ ] BCrypt cost 12 `PasswordEncoder` (already exists from FR-01)
- [ ] `LoginService`, `LoginRequest`, `AuthResponse`, `RefreshCookie`, `AuthController.login`
- **Files:** `auth/application/LoginService.java`, `auth/api/**`
- **Validation:** `LoginTest` (success, three failures with identical body, cookie attributes)

### 3. Refresh

- [ ] `RefreshTokenService.rotate` and `AuthController.refresh`
- **Files:** `auth/application/RefreshTokenService.java`, `auth/api/AuthController.java`
- **Validation:** `RefreshTest` (rotation, old token refused, expired, revoked, family expiration)

### 4. Frontend

- [ ] `login()` and `restore()` in `session.ts`; `/login` page; name in the header
- **Files:** `frontend/src/features/auth/**`, `frontend/src/routes/**`
- **Validation:** `session.test.ts`, `LoginPage.test.tsx`

### 5. E2E

- [ ] Helper `createActiveUser(request)` in `e2e/support/` (registration through the API, link from the inbox and activation setting the password)
- [ ] `e2e/login.spec.ts`: login, reload keeping the session, wrong credential with the generic message
- **Validation:** `npm run e2e` green in Chromium and Firefox

### 6. Manual

- [ ] `manual.md` with signing in, staying signed in and the login error messages; remove the FR-01 manual note about the missing login page
- **Files:** `docs/features/fr-02-login/manual.md`, `docs/features/fr-01-registration/manual.md`

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] E2E green in Chromium and Firefox
- [ ] Login screen reviewed on desktop and mobile
- [ ] Spec updated with what changed during implementation

## Risks

- Two tabs refreshing at the same time: one gets a 401 because the token was already rotated. Accepted in this phase; phase 5 handles reuse and may add a grace window.
