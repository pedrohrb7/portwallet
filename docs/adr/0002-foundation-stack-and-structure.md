# 2. Foundation: stack, versions and repository structure

Date: 2026-10-30

## Status

Accepted

## Context

The PRD accepts Java 21 + Spring Boot 3.x or Java 25 + Spring Boot 4, as long as versions are pinned. The goal is to run the application in at most 2 commands after cloning, and CI must prove build, tests and E2E on every push.

## Decision

- **Backend:** Java 21 (LTS) and Spring Boot 3.5.16. The ecosystem named in the PRD (springdoc, Resilience4j, Bucket4j, ShedLock) has mature support on this line.
- **Pinned Java tooling:** `.sdkmanrc` with Temurin 21.0.13 and Maven 3.9.16 for local development; Maven Wrapper (`./mvnw`) in CI and Docker, so nobody depends on an installed Maven.
- **Frontend:** React 19, TypeScript, Vite 8, Tailwind CSS 4, npm and Node 24 (`.nvmrc`). Linting with oxlint and formatting with Prettier.
- **Monorepo at the root:** `backend/`, `frontend/` and `docs/` directly at the repository root, without the intermediate `wallet/` folder from the PRD.
- **Database in phase 1:** in-memory H2 with `MODE=PostgreSQL`, schema versioned by Flyway and `ddl-auto=validate`, so Hibernate never changes the schema. H2 in server mode comes in phase 7.
- **Health:** Actuator exposes only `health` (without component details) and `info`, with liveness and readiness probes. The container healthcheck uses readiness.
- **Docker Compose:** `web` (Nginx serving the Vite build) is the only published port and proxies `/api` and `/actuator/health` to `api`. Hashed assets get `Cache-Control: immutable` and `index.html` gets `no-cache`, as the PRD asks.
- **Tests:** Mockito loaded as a JVM agent in Surefire, not through self-attach, which will stop working in future JDKs. JaCoCo produces the coverage report on `verify`.
- **E2E:** Playwright starts the backend and Vite through `webServer`; in CI it uses the jar built by the backend job, and locally `./mvnw spring-boot:run`.

## Consequences

- Upgrading Java or Spring Boot requires changing `.sdkmanrc`, `pom.xml`, the Dockerfiles, CI and the README version table.
- In-memory H2 loses data on every restart until phase 7, which is acceptable while there is no seed.
