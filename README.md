# PortWallet

**English** · [Português](README.pt-BR.md)

Full-stack portfolio digital wallet: users open an account, deposit, transfer to each other and check their statement, with idempotency, circuit breaker, rate limit and security implemented, visible in the UI and proven by tests.

> Demo environment, no real money. The product targets Brazilian users, with Brazilian Portuguese as the default language; the UI, API messages and e-mails are also available in English, Spanish and French, picked from the browser or the language switcher at the bottom of every page ([ADR 0007](docs/adr/0007-multilingual-product.md)). Code and documentation are in English ([ADR 0006](docs/adr/0006-english-docs-portuguese-ui.md)).

The full scope is in the [PRD](PRD.md). Technical specs, implementation plans, user manuals, conventions and architecture decisions live in [`docs/`](docs/README.md).

## Status

| Phase | Delivery | Status |
| --- | --- | --- |
| 1. Foundation | Monorepo, Maven, Vite, Flyway, Docker Compose, CI, health and a trivial E2E | Done |
| 2. Wallet core | Registration, login, deposit, transfer, statement, ledger | In progress ([progress](docs/README.md#features)) |
| 3. Idempotency | `@Idempotent`, replay, cleanup | Pending |
| 4. Resilience | Circuit breaker, bulkhead, outbox, admin dashboard | Pending |
| 5. Rate limit and base security | Bucket4j, rotating refresh, CSRF, auditing | Pending |
| 6. Advanced security and performance | TOTP, encryption, k6, Lighthouse | Pending |
| 7. Horizontal scale | H2 server, Redis, ShedLock, Nginx with 3 instances | Pending |
| 8. Showcase | GIFs, results, public deploy | Pending |

## Running with Docker (2 commands)

```sh
git clone <repository-url> portwallet && cd portwallet
docker compose up --build
```

Open <http://localhost:7000>. The port can be changed with `WEB_PORT=7500 docker compose up --build`.

Compose starts the API in the `demo` profile. In this profile, e-mails (such as the account activation link) arrive in the **demo inbox**, at <http://localhost:7000/inbox>. The [registration manual](docs/features/fr-01-registration/manual.md) walks through creating and activating an account, and the [login manual](docs/features/fr-02-login/manual.md) through signing in.

### Environment variables

Compose already passes demo values for all of them. Outside a local demo, set your own values: without them, the API only starts in the `dev` and `e2e` profiles, which have development values.

| Variable | Use |
| --- | --- |
| `JWT_SECRET` | Access token secret (HS256), at least 32 bytes |
| `PORTWALLET_CRYPTO_ENCRYPTION_KEY` | AES-256-GCM key for the encrypted fields (CPF): 32 bytes in Base64 |
| `PORTWALLET_CRYPTO_BLIND_INDEX_KEY` | HMAC-SHA256 key for the CPF blind index: 32 bytes in Base64, different from the previous one |
| `APP_BASE_URL` | Public address of the frontend, used in the links sent by e-mail |

To generate a key: `openssl rand -base64 32`.

### Development mode (H2 console)

The default Compose is the public demo: the API is reachable only through Nginx and the H2 console is off. To inspect the database, add the development override:

```sh
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

It runs the API in the `dev` profile and publishes port 7080 on the loopback only (`127.0.0.1`). The H2 console is at <http://localhost:7080/h2-console>: set the JDBC URL to `jdbc:h2:mem:portwallet` (the form suggests `jdbc:h2:~/test`), user `sa`, no password. The database lives in memory, so it starts empty on every API restart. To return to the demo, run `docker compose up -d` again.

## Pinned versions

| Tool | Version |
| --- | --- |
| Java | 21 (Temurin 21.0.13) |
| Maven | 3.9.16 (through the Maven Wrapper) |
| Spring Boot | 3.5.16 |
| Node.js | 24 (`frontend/.nvmrc`) |
| React | 19 |
| Vite | 8 |
| TypeScript | 7 |
| Tailwind CSS | 4 |
| Vitest | 5 |
| Playwright | 1.64 |

## Local development

### Prerequisites

The JDK and Maven are pinned in [`.sdkmanrc`](.sdkmanrc). With [SDKMAN](https://sdkman.io) installed:

```sh
sdk env install   # installs the versions from .sdkmanrc
sdk env           # activates the versions in the current shell
```

Node.js 24 is also needed for the frontend.

### Backend

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # API at http://localhost:7080
./mvnw verify                                           # tests + coverage (target/site/jacoco)
```

In the `dev` profile, the H2 console is at <http://localhost:7080/h2-console> (JDBC URL `jdbc:h2:mem:portwallet`, user `sa`, no password). Simulated e-mails are in the demo inbox, at <http://localhost:7173/inbox> with the frontend running.

### Frontend

```sh
cd frontend
npm install
npm run dev         # http://localhost:7173, proxying /api to the backend
npm test            # unit tests (Vitest)
npm run lint        # oxlint
npm run typecheck   # tsc
npm run format      # Prettier
```

### E2E tests

```sh
cd frontend
npx playwright install chromium firefox   # first time only
npm run e2e
```

Playwright starts the backend in the `e2e` profile and the Vite server on its own; if they are already running, they are reused. The report goes to `frontend/playwright-report`.

## Structure

```text
portwallet/
├── backend/            Spring Boot (Maven), feature packages
├── frontend/           React + Vite + Tailwind
│   └── e2e/            Playwright tests
├── docs/               specs, plans, manuals, conventions and ADRs
├── .github/workflows/  CI pipeline
└── docker-compose.yml
```

## API documentation

Swagger UI is available in the `dev` and `demo` profiles, and does not exist in the others:

- With Docker: <http://localhost:7000/swagger-ui>. Port 7080 is not published: in Compose the API is reached only through the Nginx edge (unless you use the [development mode](#development-mode-h2-console)).
- With the backend running locally (`./mvnw spring-boot:run`) in the `dev` profile: <http://localhost:7080/swagger-ui>

The OpenAPI 3.1 document is at `/v3/api-docs`. To call protected endpoints, sign in and paste the `accessToken` into "Authorize".

## Endpoints

| Endpoint | Description |
| --- | --- |
| `POST /api/v1/auth/register` | Registration (name, e-mail, CPF); always the same 202 response |
| `POST /api/v1/auth/confirm/summary` | Masked data of the registration a confirmation link belongs to |
| `POST /api/v1/auth/confirm` | Activates the account, setting the password, and opens the wallet |
| `GET /mock/inbox?to=<e-mail>` | Demo inbox (`dev`, `e2e` and `demo` profiles only) |
| `GET /actuator/health` | Overall API status, without component details |
| `GET /actuator/health/liveness` | Liveness probe |
| `GET /actuator/health/readiness` | Readiness probe, used by the Docker healthcheck |

## CI

GitHub Actions runs on every push to `main`. A first job detects what changed, and each check only runs when the code it covers changed. Documentation-only changes (`docs/`, `.md` files) trigger no check.

| Job | What it does | Runs when this changes |
| --- | --- | --- |
| **Backend** | `./mvnw verify` (tests and JaCoCo coverage report) | `backend/` |
| **Frontend** | Lint, formatting, types, unit tests and build | `frontend/` |
| **E2E** | Playwright in Chromium and Firefox against the backend; the HTML report is published as an artifact | `backend/` or `frontend/` |
| **Docker** | Starts `docker compose` and checks health through the Nginx edge | `backend/`, `frontend/` or `docker-compose.yml` |
| **Images** | Builds `portwallet-api` and `portwallet-web` and pushes them to GHCR, tagged `latest` and with the commit SHA | `backend/` or `frontend/` |
| **Deploy** | Points the demo's services on the VPS to the new images, the API first ([ADR 0010](docs/adr/0010-build-and-deploy-from-github-actions.md)) | `backend/` or `frontend/` |

Any change to the workflow itself (`.github/workflows/ci.yml`) runs every job. A skipped job shows as "skipped", and E2E runs even if the backend or frontend jobs were skipped, as long as no job that ran has failed. Images and Deploy follow the same rule, after E2E and Docker: nothing is published or deployed when a check that ran has failed.
