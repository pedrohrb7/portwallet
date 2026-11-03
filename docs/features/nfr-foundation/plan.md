# Plan: Foundation

- **Status:** Done
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** none

## Steps

### 1. Pinned tooling

- [x] `.sdkmanrc` with Temurin 21.0.13 and Maven 3.9.16
- [x] Root `.editorconfig` and `.gitignore`
- **Validation:** `sdk env install` installs the versions

### 2. Backend

- [x] `pom.xml` with Spring Boot 3.5.16, Java 21, Web, Validation, Data JPA, Actuator, Flyway and H2
- [x] Maven Wrapper 3.3.4 pointing to Maven 3.9.16
- [x] Mockito as a JVM agent in Surefire; JaCoCo report on `verify`
- [x] `application.yml` and migration `V1__baseline.sql`
- **Files:** `backend/pom.xml`, `backend/src/main/**`, `backend/.mvn/**`, `backend/mvnw*`
- **Validation:** `HealthEndpointTest` (5 tests) with `./mvnw verify`, without warnings

### 3. Frontend

- [x] Vite `react-ts` project, without the template boilerplate
- [x] Tailwind 4 (`@tailwindcss/vite`), Prettier, oxlint, Vitest + Testing Library
- [x] `DemoBanner`, `ApiStatus` and `api/health.ts`
- [x] Scripts `typecheck`, `test`, `format`, `format:check`, `e2e`
- **Files:** `frontend/**`
- **Validation:** `npm run lint && npm run format:check && npm run typecheck && npm test && npm run build`

### 4. E2E

- [x] `playwright.config.ts` with Chromium and Firefox and `webServer` for backend and Vite
- [x] `e2e/health.spec.ts`
- **Validation:** `npm run e2e` green in both browsers

### 5. Containers

- [x] Multi-stage `backend/Dockerfile` with Spring Boot layers, unprivileged user and healthcheck
- [x] `frontend/Dockerfile` and `nginx.conf` with proxy and cache
- [x] `docker-compose.yml` with `api` and `web`
- **Validation:** `docker compose up --build --wait`, health through port 3000, cache headers checked

### 6. CI

- [x] `.github/workflows/ci.yml` with jobs `backend`, `frontend`, `e2e` and `docker`
- **Validation:** 4 green jobs on GitHub Actions

### 7. Documentation

- [x] README with how to run, versions and CI
- [x] ADRs 0001 and 0002

## Final verification

- [x] `./mvnw verify` green
- [x] `npm run lint && npm run typecheck && npm test` green
- [x] E2E scenario green in Chromium and Firefox
- [x] Screen reviewed on desktop and mobile
- [x] CI green on GitHub
