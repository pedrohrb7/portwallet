# Spec: Foundation

- **Status:** Done
- **Phase:** 1
- **PRD:** Roadmap, phase 1; Stack and architecture
- **Depends on:** none
- **Plan:** [plan.md](plan.md)
- **ADR:** [0002](../../adr/0002-foundation-stack-and-structure.md)

## Goal

Create the skeleton of the monorepo with backend, frontend, versioned database, containers and CI, so that every following phase only adds features on top of a base that already builds, tests and starts on its own.

## Scope

**Includes**

- Spring Boot backend with Actuator, Flyway and H2.
- React + Vite + Tailwind frontend with a home screen and the API status.
- Docker Compose with Nginx at the edge.
- CI on GitHub Actions with build, tests, E2E and a Docker smoke test.

**Later**

- H2 in server mode, Redis and multiple instances: phase 7.
- ArchUnit and datasource-proxy: phase 2 onwards.

## Behavior

1. `docker compose up --build` starts the application at `http://localhost:3000` with no prior configuration.
2. `GET /actuator/health` answers `200 {"status":"UP"}` without component details.
3. The `/actuator/health/liveness` and `/actuator/health/readiness` probes answer 200; the container healthcheck uses readiness.
4. Flyway applies the migrations on startup and Hibernate only validates the schema (`ddl-auto=validate`).
5. The H2 console exists only in the `dev` profile.
6. Every screen shows the banner "Ambiente de demonstração, sem dinheiro real" ("Demo environment, no real money").
7. The home screen shows the API status: Online, Fora do ar (down) or Indisponível (unavailable).
8. Nginx serves hashed assets with `Cache-Control: public, max-age=31536000, immutable` and `index.html` with `no-cache`, and returns `index.html` for unknown routes.

## API

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| GET | `/actuator/health` | Public | Overall status |
| GET | `/actuator/health/liveness` | Public | Liveness probe |
| GET | `/actuator/health/readiness` | Public | Readiness probe |

Nginx exposes only `/api/` and `/actuator/health`; the rest of Actuator never leaves the container.

## Data model

- `V1__baseline.sql`: table `app_metadata (meta_key, meta_value)`, used only to prove Flyway ran.

## Backend

- `com.portwallet.PortWalletApplication`.
- `application.yml`: datasource configurable through `DATABASE_URL`, `open-in-view=false`, virtual threads, `shutdown=graceful`, errors without stack trace, `dev` profile with the H2 console.

## Frontend

- `DemoBanner` (`data-testid="demo-banner"`, `role="note"`).
- `ApiStatus` (`data-testid="api-status"`, `data-status` attribute with `LOADING`, `UP`, `DOWN` or `UNKNOWN`).
- Vite proxy for `/api` and `/actuator/health` (`API_URL`, default `http://localhost:8080`).

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | Health `UP`; probes exposed; health without `components`; migration applied; H2 console off outside `dev` |
| Unit (frontend) | Banner visible; Online status with `UP`; down with `DOWN` |
| E2E | Home page shows banner, title and API `UP` (Chromium and Firefox) |
| CI | `docker compose up --wait` and health through the Nginx edge |

## Acceptance criteria

- [x] Pipeline with a health endpoint and a trivial Playwright test
- [x] Application running in 2 commands after cloning
- [x] Versions pinned in the README

## Decisions

Recorded in [ADR 0002](../../adr/0002-foundation-stack-and-structure.md).

**CI revision (Phase 2):** the pipeline now runs only on `push` to `main` and only when code changes. A `changes` job (`dorny/paths-filter`, pinned to the commit SHA of v4.0.3) decides which jobs run: backend with `backend/`, frontend with `frontend/`, E2E with either of them and Docker also with `docker-compose.yml`; a change to the workflow itself runs everything. Skipped jobs show as "skipped", which does not block required checks. The E2E job now builds the backend jar itself, because the backend job may have been skipped.

## Open questions

- None. The pipeline ran green on GitHub Actions.
