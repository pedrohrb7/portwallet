# Plan: FR-03 Logout

- **Status:** Approved
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [fr-02-login](../fr-02-login/plan.md)

## Steps

### 1. Backend

- [ ] `RefreshTokenService.revoke` and `AuthController.logout`
- **Files:** `auth/application/RefreshTokenService.java`, `auth/api/AuthController.java`
- **Validation:** `LogoutTest` (revocation, no cookie, other family untouched)

### 2. Frontend

- [ ] `session.logout()` and the "Sair" button in the header
- **Files:** `frontend/src/features/auth/session.ts`, `frontend/src/routes/AppLayout.tsx`
- **Validation:** `session.test.ts` (cleanup even with a network error)

### 3. E2E

- [ ] Sign out and reload scenario in `e2e/login.spec.ts`
- **Validation:** `npm run e2e` green in Chromium and Firefox

### 4. Manual

- [ ] "Signing out" section in the FR-02 manual
- **Files:** `docs/features/fr-02-login/manual.md`

## Final verification

- [ ] `./mvnw verify` green
- [ ] `npm run lint && npm run typecheck && npm test` green
- [ ] E2E green in Chromium and Firefox
- [ ] Spec updated with what changed during implementation
