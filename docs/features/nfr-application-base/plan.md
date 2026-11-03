# Plan: Application base

- **Status:** Done
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [nfr-foundation](../nfr-foundation/plan.md)

## Steps

### 1. Backend dependencies

- [x] Add `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-security-test` and `archunit-junit5`
- **Files:** `backend/pom.xml`
- **Validation:** `./mvnw verify` green (the health test keeps passing with health permitted)

### 2. Problem Details

- [x] `ApiException` and `GlobalExceptionHandler`, including validation, malformed JSON, parameter type and generic 500
- [x] `spring.mvc.problemdetails.enabled=true` and 404 for unknown routes
- **Files:** `backend/src/main/java/com/portwallet/shared/error/**`
- **Validation:** `ProblemDetailsTest` with a test controller that throws each error type

### 3. Money and clock

- [x] `Money` record, Jackson serializer and deserializer
- [x] `TimeConfig` with `Clock` and `BUSINESS_ZONE`
- **Files:** `shared/money/**`, `shared/time/**`
- **Validation:** `MoneyTest` and `MoneyJsonTest`

### 4. Security and JWT

- [x] Validated `JwtProperties`, `JwtService`, `SecurityConfig`, `CurrentUser`
- [x] Entry point and access denied handler in Problem Details
- [x] Secrets for the `dev`, `e2e` and `test` profiles; `JWT_SECRET` in `docker-compose.yml`, with a demo default
- **Files:** `shared/security/**`, `application.yml`, `src/test/resources/application-test.yml`, `docker-compose.yml`
- **Validation:** `SecurityTest` (401 without token, expired, wrong signature; 403 without role); `JwtSecretRequiredTest`

### 5. ArchUnit

- [x] `ArchitectureTest` with the spec rules
- **Files:** `backend/src/test/java/com/portwallet/ArchitectureTest.java`
- **Validation:** the test fails if a controller injects a repository (checked with a temporary class)

### 6. Frontend dependencies and HTTP client

- [x] Install `react-router`, `@tanstack/react-query`, `axios`, `react-hook-form`, `zod`, `@hookform/resolvers`
- [x] `api/client.ts`, `api/problem.ts`, `lib/money.ts`
- **Files:** `frontend/package.json`, `frontend/src/api/**`, `frontend/src/lib/**`
- **Validation:** `client.test.ts` (single refresh for simultaneous 401s) and `money.test.ts`

### 7. Session, routes and layout

- [x] `session.ts` and session provider
- [x] `router.tsx`, `RequireAuth`, `AppLayout`, `PublicLayout`, 404 page
- [x] `ApiStatus` now uses the shared client
- **Files:** `frontend/src/features/auth/**`, `frontend/src/routes/**`, `frontend/src/main.tsx`, `frontend/src/App.tsx`
- **Validation:** `RequireAuth.test.tsx`; health E2E stays green

## Final verification

- [x] `./mvnw verify` green
- [x] `npm run lint && npm run typecheck && npm test` green
- [x] Existing E2E green in Chromium and Firefox
- [x] ArchUnit fails with a temporary violating class (checked and removed)
- [x] Layout reviewed on desktop and mobile
- [x] Spec updated with what changed during implementation
- [x] ADR 0004 on money as a string in JSON

## Risks

- Security blocking health or the Vite proxy: the existing health test is the first to run after step 4.
