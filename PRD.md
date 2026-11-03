# PRD: Digital Wallet (Portfolio Project)

Oct 30, 2026 · @Pedro

## Overview

A full-stack digital wallet for a portfolio: users open an account, deposit, transfer to each other and check their statement, with idempotency, circuit breaker, rate limit and security implemented, visible in the UI and proven by tests.

**Problem.** Portfolio projects tend to be CRUDs that do not show how the candidate deals with failures, concurrency and abuse. In a wallet these problems are real: a duplicated retry becomes double money, an external service that is down must not freeze transfers, and a login without limits becomes a brute force target.

**Audience.** Recruiters and technical interviewers, who evaluate the project through the README, the demo and the code in a few minutes.

**Scope.** A modular, stateless monolith, with a Spring Boot + H2 backend and a React frontend, running as several instances behind a load balancer. The external services (antifraud and notification) are simulated inside the project itself, with controllable failures. There is no real money nor banking integration.

The product targets Brazilian users: the UI is in Brazilian Portuguese, amounts are in BRL and users are identified by CPF (Brazilian taxpayer number). Engineering artifacts are in English ([ADR 0006](docs/adr/0006-english-docs-portuguese-ui.md)).

## Goals and success metrics

The project succeeds when an evaluator can run the application and see the four concepts working in less than 10 minutes.

**Goals**

- Idempotency in financial operations, proven by a concurrent test.
- Resilience with circuit breaker and bulkhead visible live on a dashboard.
- Rate limit with 429 responses and standard headers, global across instances.
- Security: JWT, role-based authorization, TOTP step-up, protection against brute force and enumeration, encryption at rest and tamper-evident auditing.
- Consistent balance under concurrency, with a double-entry ledger and optimistic locking.
- Performance measured, not assumed: load testing, query control and frontend targets.
- Horizontal scale: stateless API running as 3 instances behind a load balancer, with shared state in H2 server mode and in Redis.

**Metrics**

| Metric | Target |
| --- | --- |
| Commands from clone to running application | ≤ 2 |
| Backend test coverage (domain and services) | ≥ 80% of lines |
| Playwright E2E scenarios | ≥ 12 critical flows, green in CI |
| Transactions created by 50 parallel requests with the same key | Exactly 1 |
| Balance after 100 concurrent transfers | Equal to the ledger sum, never negative |
| Transfer p95 latency, 100 virtual users for 5 min (k6) | < 200 ms, with errors < 1% |
| SQL queries per endpoint | Fixed ceiling per endpoint, checked by tests |
| Lighthouse (performance and accessibility) | ≥ 90 |
| CI pipeline | Build, tests and E2E on every push |
| Transfer throughput, from 1 to 3 instances (k6) | ≥ 2x, or the bottleneck identified and documented |
| Transfers lost or duplicated when an instance is killed during load | 0 |

**Non-goals**

- Integration with banks, real PIX or a payment gateway.
- Multiple currencies and exchange.
- Autoscaling on Kubernetes (left as an optional extension; the project scales with Docker Compose).
- Mobile app.

## Personas and user stories

Three personas cover the product: who uses the wallet, who operates it and who evaluates the portfolio.

| Persona | Who they are | What they need |
| --- | --- | --- |
| User | Wallet customer | Move money safely and without duplicate charges |
| Administrator | Platform operator | Contain fraud and follow the health of the system |
| Evaluator | Recruiter or interviewer | Try it fast, without registering, and see the concepts in action |

| ID | As a | I want to | So that |
| --- | --- | --- | --- |
| US-01 | User | Create an account with e-mail and password | I can access the wallet |
| US-02 | User | Sign in and stay authenticated securely | I do not type the password all the time |
| US-03 | User | Deposit simulated balance | I have money to transfer |
| US-04 | User | Transfer to another user by e-mail | I can pay someone |
| US-05 | User | See the statement paginated and filtered | I can follow my movements |
| US-06 | User | Resend a transfer on an unstable connection | I am sure it will not be duplicated |
| US-07 | Administrator | Block and unblock accounts | I can contain fraud |
| US-08 | Administrator | Reverse a transfer | I can fix errors without deleting history |
| US-09 | Administrator | See the state of the circuit breakers and turn on simulated failures | I can demonstrate and follow resilience |
| US-10 | Evaluator | Sign in with demo accounts already populated | I can try it without registering |

## Functional requirements

Seventeen requirements, thirteen of them mandatory for the first version. MoSCoW priority: Must and Should.

| ID | Requirement | Rules | Priority |
| --- | --- | --- | --- |
| FR-01 | Registration | Name, e-mail and fictitious CPF with validated format. Always a generic response (202), whether the e-mail exists or not. Account activated through the confirmation link, where the user checks the registration summary and sets the password (at least 8 characters). | Must |
| FR-02 | Login | Returns a JWT access token (15 min) and a refresh token (7 days) in an httpOnly cookie, rotated on every use. Generic error for unknown e-mail or wrong password. | Must |
| FR-03 | Logout | Revokes the current refresh token. | Must |
| FR-04 | Simulated deposit | Amount > 0 and ≤ R$ 10,000 per operation. Requires `Idempotency-Key`. | Must |
| FR-05 | Transfer | Destination: any registered, active user, by e-mail or CPF, confirmed through the recipient lookup (FR-17); amount > 0; sufficient balance; active destination different from the sender; daily limit of R$ 5,000; goes through antifraud. Requires `Idempotency-Key`. Above R$ 1,000, requires a TOTP code. | Must |
| FR-06 | Transfer details | The sender or the recipient looks up a transfer by ID. | Must |
| FR-07 | Statement | Cursor-paginated, filterable by period and type, newest first. | Must |
| FR-08 | Balance | Column updated in the same transaction as the entries; must always match the ledger sum. | Must |
| FR-09 | Reversal | Admin reverses a transfer by creating inverse entries; nothing is deleted. Requires `Idempotency-Key`. | Should |
| FR-10 | Notification | After the transfer, sender and recipient are notified asynchronously through an outbox. | Should |
| FR-11 | Admin: users | List, search, block and unblock accounts. A blocked account neither sends nor receives. | Must |
| FR-12 | Admin: resilience | Dashboard with the state of the circuit breakers and bulkheads, and failure rate and latency controls for the simulated services. | Must |
| FR-13 | Demo data | Seed with two demo users and one demo admin, already confirmed, with transaction history. | Must |
| FR-14 | Password recovery | Always a generic response. Link with a single-use token, valid for 30 min; only the token hash is stored. The change revokes every session. | Should |
| FR-15 | Two-factor authentication | The user enables TOTP by reading a QR code and confirms with a code. Transfers above R$ 1,000 and password changes require the code (step-up). 5 wrong codes in 15 min lock the verification and create an audit record. | Must |
| FR-16 | Admin: audit integrity | Checks the hash chain of the audit log and points at the first tampered record. | Should |
| FR-17 | Recipient lookup | An authenticated user enters an e-mail or CPF and gets the masked name ("Ana S\*\*\*\*") of a registered, active user, to check before confirming. Also lists the 5 most recent recipients. Not found or inactive: a single message, without saying which. | Must |

Confirmation, password recovery and notification e-mails go out through the simulated notification service and stay visible in a demo inbox, enabled only in the `dev` and `e2e` profiles.

## Non-functional requirements

These are the requirements the portfolio exists to demonstrate. Each one has a defined implementation and a test that proves the behavior.

### Idempotency

- `Idempotency-Key` header (UUID v4) required on deposit, transfer and reversal `POST`s. Without it, the response is 400.
- `idempotency_keys` table with a composite unique key (key + user), SHA-256 hash of the body, status (`PROCESSING` or `COMPLETED`), HTTP status and response body, and a 24 h expiration.
- Flow:
  1. The request tries to insert the key with status `PROCESSING`. The unique constraint guarantees only one wins a race.
  2. Same key and same body, already completed: returns the stored response with the `Idempotent-Replayed: true` header.
  3. Same key with a different body: 422.
  4. Same key still `PROCESSING`: 409.
- Implemented as an `@Idempotent` annotation with a reusable interceptor, without logic scattered across controllers.
- A `@Scheduled` job removes expired keys.

### Circuit breaker

- Resilience4j on the antifraud and notification clients, which call the simulated services over HTTP to keep the scenario realistic.
- Antifraud configuration: sliding window of 10 calls; opens with ≥ 50% failures or slow calls (> 2 s); stays open for 15 s; lets 3 calls through in half-open.
- Retry with exponential backoff (2 attempts) only for transient errors, wrapping the circuit breaker.
- Antifraud fallback, as a documented business rule: transfers up to R$ 500 are approved with `fraudCheck = SKIPPED` for later review; above that, the response is 503 with a clear message.
- Notification fallback: the event stays in the outbox and is resent when the circuit closes.
- State exposed in Actuator and in the admin dashboard.

### Rate limit

Bucket4j with buckets in Redis, one per key, shared by every instance: the limit is global, not multiplied by the number of instances.

| Scope | Key | Limit |
| --- | --- | --- |
| Login | IP + e-mail | 5 attempts every 15 min |
| Registration | IP | 3 per hour |
| Transfers | User | 10 per minute |
| Authenticated API | User | 100 per minute |
| Public API | IP | 30 per minute |
| Recipient lookup | User | 20 per minute and 200 per day |
| Wrong TOTP code | User | 5 errors every 15 min; then, locked until the end of the window |

- When exceeded: 429 with `Retry-After`, `X-RateLimit-Limit` and `X-RateLimit-Remaining`; body in Problem Details format (RFC 9457).
- The frontend shows a countdown until it is released.

### Security

- Stateless Spring Security with JWT. 15 min access token kept in memory in the frontend, never in `localStorage`.
- Refresh token in an httpOnly, `Secure`, `SameSite=Strict` cookie, rotated on every use. Reusing an old token revokes the whole family.
- Passwords with BCrypt (cost 12). JWT secret in an environment variable.
- `USER` and `ADMIN` roles with `@PreAuthorize`. Ownership check on every resource to prevent IDOR.
- Bean Validation on every DTO. Money always as `BigDecimal` with scale 2, never `double`.
- CORS restricted to the frontend origin; security headers (CSP, `X-Content-Type-Options`, `X-Frame-Options`).
- H2 console enabled only in the `dev` profile.
- Errors without stack trace to the client; logs without password, token or full CPF.
- `audit_log` table for login, failed login, transfer, reversal and blocking.

### Advanced security

- **TOTP step-up (RFC 6238):** transfers above R$ 1,000 and password changes require the authenticator code in the `X-TOTP-Code` header. Without it, the API answers 403 with `type: totp-required` and the frontend opens the code modal. A code already used is not valid again in the same window. Since a 6-digit code has only 1 million combinations, 5 wrong codes in 15 minutes lock the user's TOTP verification until the end of the window (429 with `type: totp-locked` and `Retry-After`). Every error and every lock is recorded in the audit log. The counter lives in Redis, so it holds across every instance.
- **Anti-enumeration:** login, registration and password recovery return the same status and the same message, whether the e-mail exists or not. To equalize response time, login runs BCrypt against a dummy hash when the user does not exist; registration with an e-mail already in use sends a warning to the owner instead of an error.
- **CSRF on refresh:** besides `SameSite=Strict`, the double-submit pattern with `CookieCsrfTokenRepository`: an `XSRF-TOKEN` cookie readable by JavaScript and an `X-XSRF-TOKEN` header required on `/auth/refresh` and `/auth/logout`, the only endpoints that depend on a cookie.
- **Encryption at rest:** CPF and TOTP secret encrypted with AES-256-GCM through a JPA `AttributeConverter`, with a random IV per record and the key in an environment variable. CPF uniqueness and lookup use a blind index (`HMAC-SHA256` of the CPF) in a separate column.
- **Tamper-evident audit:** each record stores `hash = SHA-256(prev_hash + content)`. Inserts are serialized to keep the chain order, and `GET /admin/audit/verify` recomputes everything and points at the first altered record.

The recipient lookup (FR-17) inherently reveals whether an e-mail or CPF is registered. It is the same trade-off as a PIX key lookup, explicitly accepted and contained this way: only for authenticated users, with its own rate limit, the name always masked and every lookup recorded in the audit log.

### Concurrency and consistency

- Double-entry ledger: every movement creates a debit and a credit of the same amount, in the same transaction.
- Optimistic locking (`@Version`) on the account. On conflict, up to 3 retries; then, 409.
- Balance never negative: validated in the service and enforced by `CHECK (balance >= 0)` in the database.
- Outbox pattern: the notification event is written in the same transaction as the transfer and published by a job.

### Observability

- Actuator with health (including the circuit breakers) and metrics.
- Structured JSON logs with `correlationId` in the MDC, propagated through the `X-Correlation-Id` header.
- Custom metrics: transfers by status, idempotent replays and 429 responses issued.

### Performance

- **Cursor pagination in the statement:** opaque cursor (Base64 of `created_at` + `id`) and the query `WHERE (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT ?`. The cost per page is constant and no item is skipped or repeated when new transactions come in, unlike `OFFSET`.
- **Indexes designed for the queries:** `ledger_entries (account_id, created_at DESC, id DESC)` for the statement, `transactions (source_account_id, created_at)` for the daily limit and `idempotency_keys (expires_at)` for the cleanup. The `EXPLAIN` before and after is recorded in an ADR.
- **N+1 under control:** reads through DTO projections and `@EntityGraph`, with `spring.jpa.open-in-view=false`. Tests with datasource-proxy fail if an endpoint goes over the expected number of queries.
- **Bulkhead:** Resilience4j limits simultaneous calls to antifraud to 10. The excess goes straight to the fallback, instead of holding threads while the service is slow.
- **Virtual threads:** `spring.threads.virtual.enabled=true`, with a k6 benchmark comparing throughput and p95 with and without them, under simulated antifraud latency.
- **ETag and 304:** `/accounts/me` and the first statement page return an ETag derived from the account `version`. The frontend polling sends `If-None-Match` and gets a 304 without body when nothing changed.
- **Load test:** k6 scenarios versioned in `load/`, with a target of p95 < 200 ms and errors < 1% on transfers, 100 virtual users for 5 minutes. Results and charts in the README.
- **Frontend:** admin routes with `React.lazy`; hashed assets served with `Cache-Control: public, max-age=31536000, immutable` and `index.html` with `no-cache`; Lighthouse ≥ 90 checked in CI with Lighthouse CI.
- **No optimistic updates on financial operations:** the UI only shows the new balance after the server confirms it. Decision recorded in an ADR.

### Horizontal scale

The API keeps no state in memory: any instance serves any request. The demo starts 3 instances behind an Nginx with `docker compose up --scale api=3`.

| State | Where it lives | Why it works with several instances |
| --- | --- | --- |
| User session | JWT on the client | No instance keeps sessions; all validate with the same key |
| Refresh tokens | H2 in server mode | Rotation and reuse detection in a single table |
| Idempotency keys | H2 in server mode | The composite primary key settles the race even across different instances |
| Balance and ledger | H2 in server mode | Optimistic locking through the `version` column |
| Rate limit buckets | Redis | Global limit, not multiplied by the number of instances |
| Already used TOTP codes | Redis with TTL | A code used on one instance is refused on the others; the error counter is global too |
| Chaos configuration | H2 in server mode | Every instance simulates the same failure |
| Circuit breaker state | Memory of each instance | On purpose: each instance protects itself; the dashboard shows the state per instance |

- **Shared database:** H2 runs in server mode (TCP) in its own container, instead of embedded in each instance.
- **Scheduled jobs:** outbox and key cleanup run with ShedLock (JDBC provider), so only one instance runs each job at a time.
- **Audit chain:** the order is guaranteed in the database, with `SELECT ... FOR UPDATE` on a control row that stores the last hash, not with an in-memory lock.
- **Load balancer:** round-robin Nginx, with passive health checks (`max_fails`). Actuator readiness stays exposed for a future orchestrator.
- **Graceful shutdown:** `server.shutdown=graceful`; the instance finishes in-flight requests before leaving.
- **Visible distribution:** every response carries the `X-Instance-Id` header, and the admin dashboard shows which instance served each call.
- **Proof:** k6 against 1 and 3 instances, comparing throughput, and a test that kills an instance in the middle of the load without losing or duplicating transfers.

With a single H2, the database becomes the bottleneck and the single point of failure of the demo. This is expected and recorded under Risks.

## Stack and architecture

A modular Spring Boot monolith, replicated as several instances behind an Nginx, serving a React SPA. Shared state lives in H2 server mode and in Redis. The rate limit, authentication and idempotency barriers sit at the edge, before any business rule.

&#91;embedded content: architecture · SPA, Spring Boot monolith, H2 and simulated services\]

The external services are simulated in the app itself, with failure and latency controls, so the circuit breaker can be demonstrated live. The admin dashboard reads the circuit state through Actuator.

| Layer | Technology | Use |
| --- | --- | --- |
| Backend | Java 21 (LTS) + Spring Boot 3.x | Web, Validation, Data JPA, Security, Actuator |
| Database | H2 + Flyway | Versioned migrations; `MODE=PostgreSQL` |
| Authentication | Spring Security + OAuth2 Resource Server (Nimbus JWT) | JWT issuing and validation; double-submit CSRF on refresh |
| 2FA | TOTP library (RFC 6238) + QR code generator | Step-up on sensitive operations |
| Cryptography | JCA (AES-GCM, HMAC-SHA256) | Encrypted fields and blind index |
| Resilience | Resilience4j | Circuit breaker, bulkhead, retry and time limiter |
| Rate limit | Bucket4j + Redis | Buckets shared across instances |
| Documentation | springdoc-openapi | Swagger UI at `/swagger-ui` |
| Frontend | React + TypeScript + Vite | SPA with routes loaded on demand |
| Styling | Tailwind CSS | Utilities and theme |
| Data and forms | TanStack Query, React Hook Form, Zod, Axios | Server cache, validation and interceptors |
| Tests | JUnit 5, Mockito, WireMock, ArchUnit, datasource-proxy, Vitest, Playwright | Unit, integration, queries, architecture and E2E |
| Performance | k6, Lighthouse CI | Backend load and frontend quality |
| Infra | Docker Compose, GitHub Actions | Local execution and CI |
| Scale | Nginx, H2 in server mode, Redis, ShedLock | Load balancer, shared database and state, single-run jobs |

Newer versions (Java 25, Spring Boot 4) also work; what matters is pinning them in the README.

**Repository structure**

```text
wallet/
├── backend/            Spring Boot (Maven), feature packages:
│                       auth, account, transfer, ledger, idempotency,
│                       resilience, ratelimit, admin, mock, shared
├── frontend/           React + Vite + Tailwind
│   └── e2e/            Playwright tests
├── docs/adr/           architecture decisions
├── docker-compose.yml
└── README.md
```

## Data model

Nine tables, versioned with Flyway. The ledger is the source of truth: entries are immutable and the account balance is a derived value, kept in the same transaction.

| Table | Main fields | Notes |
| --- | --- | --- |
| `users` | id (UUID), name, email, password\_hash, document\_enc, document\_hash, totp\_secret\_enc, totp\_enabled, role, status, created\_at | Unique e-mail; status `PENDING`, `ACTIVE` or `BLOCKED`; `password_hash` empty while `PENDING`; `*_enc` encrypted with AES-GCM; `document_hash` is the blind index, unique |
| `accounts` | id, user\_id, balance, daily\_limit, version, created\_at | 1:1 with the user; `DECIMAL(19,2)`; `CHECK (balance >= 0)`; `version` for optimistic locking and ETag |
| `transactions` | id, type, status, amount, source\_account\_id, target\_account\_id, fraud\_check, idempotency\_key, created\_at | type: `DEPOSIT`, `TRANSFER`, `REVERSAL`; fraud\_check: `APPROVED`, `SKIPPED`, `DENIED` |
| `ledger_entries` | id, transaction\_id, account\_id, direction, amount, balance\_after, created\_at | `DEBIT` or `CREDIT`; immutable, no update nor delete |
| `idempotency_keys` | key, user\_id, request\_hash, status, response\_code, response\_body, created\_at, expires\_at | Composite primary key (key, user\_id) |
| `outbox_events` | id, aggregate\_id, type, payload, status, attempts, next\_attempt\_at | JSON payload; reprocessed with backoff |
| `refresh_tokens` | id, user\_id, token\_hash, family\_id, expires\_at, revoked | Stores only the hash; `family_id` detects reuse |
| `user_tokens` | id, user\_id, purpose, token\_hash, expires\_at, used\_at | purpose: `EMAIL_CONFIRMATION` or `PASSWORD_RESET`; single use |
| `audit_log` | id, user\_id, action, ip, details, prev\_hash, hash, created\_at | Insert only; `hash` chains with the previous record |

**Invariants**

- Deposits debit a system account (`EXTERNAL_FUNDS`), keeping double entry without exceptions.
- For every transaction, the sum of debits equals the sum of credits.
- For every account, `balance` equals credits minus debits in the ledger. An integration test checks this after every concurrent scenario.

## API

REST versioned under `/api/v1`, documented with springdoc-openapi (Swagger UI). Every error follows Problem Details (RFC 9457) with a stable `type`, such as `insufficient-balance`.

| Method | Endpoint | Access | Idempotency-Key | Description |
| --- | --- | --- | --- | --- |
| POST | `/auth/register` | Public | No | Registers and sends the confirmation link; always 202 |
| POST | `/auth/confirm/summary` | Public | No | Masked name and CPF of the registration the link belongs to |
| POST | `/auth/confirm` | Public | No | Activates the account with the link token, setting the password |
| POST | `/auth/login` | Public | No | Returns the access token and sets the refresh and XSRF cookies |
| POST | `/auth/refresh` | Cookie + XSRF | No | Rotates the refresh token and returns a new access token |
| POST | `/auth/logout` | Cookie + XSRF | No | Revokes the refresh token |
| POST | `/auth/password/forgot` | Public | No | Sends the recovery link; always 202 |
| POST | `/auth/password/reset` | Public | No | Changes the password with the link token and revokes the sessions |
| POST | `/auth/2fa/setup` | USER | No | Generates the TOTP secret and returns the QR code |
| POST | `/auth/2fa/enable` | USER | No | Confirms the first code and enables 2FA |
| GET | `/accounts/me` | USER | No | Balance, daily limit and how much was already used today; with ETag |
| POST | `/deposits` | USER | Yes | Simulated deposit |
| POST | `/transfers` | USER | Yes | New transfer; `X-TOTP-Code` above R$ 1,000 |
| GET | `/transfers/{id}` | USER (owner) | No | Transfer details |
| GET | `/statement` | USER | No | Statement with `cursor`, `size`, `from`, `to`, `type`; returns `nextCursor` |
| GET | `/admin/users` | ADMIN | No | Lists and searches users |
| PATCH | `/admin/users/{id}/status` | ADMIN | No | Blocks or unblocks |
| POST | `/admin/transfers/{id}/reversal` | ADMIN | Yes | Reverses a transfer |
| GET | `/admin/resilience` | ADMIN | No | State of the circuit breakers and bulkheads |
| PUT | `/admin/chaos` | ADMIN | No | Sets the failure rate and latency of a simulated service |
| GET | `/admin/audit/verify` | ADMIN | No | Checks the audit hash chain |
| GET | `/recipients/lookup` | USER | No | Lookup by e-mail or CPF; returns an opaque recipient ID and the masked name |
| GET | `/recipients/recent` | USER | No | The user's 5 most recent recipients |

The simulated services live outside `/api`, at `/mock/antifraud/check` and `/mock/notifications`, reachable only internally. The demo inbox lives at `/mock/inbox`, only in the `dev` and `e2e` profiles.

**Error codes**

| Status | When |
| --- | --- |
| 202 | Registration and password recovery, with an identical response whether the e-mail exists or not |
| 304 | ETag equal to `If-None-Match`: nothing changed |
| 400 | Validation failure, missing `Idempotency-Key` or invalid cursor |
| 401 | Missing, invalid or expired token; invalid credentials (generic message) |
| 403 | Role without permission, another user's resource, blocked account, invalid XSRF token or TOTP code required (`totp-required`) |
| 404 | Unknown resource |
| 409 | Key still being processed or concurrency conflict after the retries |
| 422 | Insufficient balance, daily limit exceeded, transfer to oneself, key reused with a different body or invalid TOTP code |
| 429 | Rate limit exceeded or TOTP verification locked (`totp-locked`) |
| 503 | Antifraud unavailable or bulkhead full, with an amount above the fallback limit |

**Example**

```http
POST /api/v1/transfers
Authorization: Bearer <access-token>
Idempotency-Key: 6f1c2d3e-8a9b-4c5d-9e0f-1a2b3c4d5e6f
Content-Type: application/json

{ "targetEmail": "ana@demo.com", "amount": 150.00, "description": "Almoço" }
```

## Frontend

React + TypeScript SPA with Vite and Tailwind CSS. Server state with TanStack Query, forms with React Hook Form + Zod, routes with React Router and an Axios client with interceptors for token refresh, `Idempotency-Key` and `X-Correlation-Id`. The UI is in Brazilian Portuguese.

| Screen | Route | Content | Key behaviors |
| --- | --- | --- | --- |
| Login | `/login` | E-mail, password, shortcuts to demo accounts | Countdown on a 429; generic error |
| Registration | `/register` | Name, e-mail, CPF | Real-time validation with Zod; same final message in every case |
| Confirmation | `/confirm` | Registration summary and choice of the password | Reads the token from the URL and removes it from the history |
| Password recovery | `/forgot-password`, `/reset-password` | Link request and new password | Generic message on the request |
| Dashboard | `/` | Balance, daily limit used, latest transactions | Amounts in BRL with `Intl.NumberFormat`; polling with ETag |
| Deposit | `/deposit` | Amount | Same idempotent protection as the transfer |
| Transfer | `/transfer` | Recipient lookup by e-mail or CPF, recent recipients, amount, description, confirmation screen with the masked name | Key generated when the confirmation opens and reused on retries and double click; button disabled while submitting; TOTP code modal when the API asks |
| Statement | `/statement` | List with infinite scroll and filters | Cursor pagination; filters reflected in the URL |
| Account security | `/settings/security` | Enabling 2FA with a QR code, password change | Password change requires a TOTP code |
| Demo inbox | `/inbox` | E-mails sent by the simulated service | Only in the `dev` and `e2e` profiles |
| Admin: users | `/admin/users` | Search, status, block and unblock | Route protected by role; loaded on demand |
| Admin: resilience | `/admin/resilience` | State of each circuit breaker and bulkhead, failure rate, chaos controls | Updated every 2 s, without reloading the page |
| Admin: audit | `/admin/audit` | Latest events and a button to verify integrity | Highlights the first tampered record |

**Guidelines**

- Stable test selectors through `data-testid` and ARIA roles.
- Feedback for every API error, mapped by the Problem Details `type`.
- Responsive and accessible layout: contrast, visible focus, keyboard navigation.
- Banner "Ambiente de demonstração, sem dinheiro real" ("Demo environment, no real money") on every screen.

## Test strategy

Each project concept has a test that proves the behavior, not just the configuration. The concurrency and E2E tests are the showcase.

| Layer | Tools | What it covers |
| --- | --- | --- |
| Unit (backend) | JUnit 5, Mockito, AssertJ | Domain rules: balance, daily limit, ledger generation, request hash, cursor encoding |
| Integration | `@SpringBootTest`, MockMvc, H2 | Endpoints, migrations, 401/403, IDOR, Problem Details, ETag and 304 |
| Concurrency | `ExecutorService` + `CountDownLatch` | 50 parallel requests with the same key create 1 transaction; 100 cross transfers keep the balance equal to the ledger |
| Resilience | WireMock | 500 errors and timeouts open the circuit; correct fallback; half-open closes again; a full bulkhead goes to the fallback |
| Rate limit | Integration | The 6th login attempt returns 429 with `Retry-After` |
| Security | Integration | Same response and similar time for existing and unknown e-mails; refresh without XSRF returns 403; CPF stored encrypted; an altered audit record is detected; a reused TOTP is refused; the 6th wrong code in 15 min returns 429 and creates an audit record |
| Queries | datasource-proxy | Query ceiling per endpoint, failing on N+1 |
| Architecture | ArchUnit | Controllers do not access repositories; dependencies between modules respected |
| Load | k6 | Transfer p95 and error rate; comparison with and without virtual threads |
| Unit (frontend) | Vitest, Testing Library | Components, currency formatting, API hooks |
| Frontend (quality) | Lighthouse CI | Performance and accessibility ≥ 90 |
| E2E | Playwright | Critical flows below |
| Scale | k6, Docker Compose | Throughput with 1 and 3 instances; instance killed in the middle of the load without loss or duplication; the same Idempotency-Key sent to different instances creates 1 transaction; global rate limit across instances |

**Playwright E2E scenarios**

1. Registration, confirmation through the demo inbox, login and dashboard with a zero balance.
2. A deposit updates the balance on the dashboard.
3. The recipient lookup shows the masked name, and the transfer shows up in both users' statements (two browser contexts).
4. Insufficient balance shows the right error and does not change the balance.
5. A double click on "Confirmar" creates a single transaction (counting requests with `page.route`).
6. A network failure on the first attempt, aborted with `page.route`, followed by a retry does not duplicate the transfer.
7. Five wrong logins lock temporarily and show the countdown.
8. Admin turns on antifraud failures, the dashboard shows the circuit `OPEN`, a small transfer is approved with a warning and a large one is refused.
9. A regular user cannot access `/admin` routes.
10. Transparent token refresh after the access token expires, with the clock controlled by `page.clock`.
11. The user enables 2FA; a transfer above R$ 1,000 opens the modal and only completes with the right code (generated in the test from the secret).
12. Password recovery through the demo inbox; the old session stops working after the change.

**Playwright configuration**

- `webServer` starts the backend in the `e2e` profile (with seed) and the frontend.
- Chromium and Firefox projects; trace and video on the first retry.
- Each test creates its own users through the API, without depending on execution order and without `waitForTimeout`.
- Runs on GitHub Actions, publishing the HTML report as an artifact.

## Roadmap and acceptance criteria

Eight phases, each one deliverable on its own: if the project stops halfway, what exists is already publishable. Durations are estimates for part-time dedication.

1. **Foundation (\~1 week).** Monorepo, Maven, Vite, Flyway, Docker Compose and GitHub Actions.
   - Acceptance: green pipeline with a health endpoint and a trivial Playwright test.
2. **Wallet core (\~2 weeks).** Registration, JWT login, accounts, deposit, recipient lookup, transfer, statement with cursor pagination, ledger and optimistic locking.
   - Acceptance: the 100 concurrent transfers test passes; E2E scenarios 1 to 4 green.
3. **Idempotency (\~1 week).** `@Idempotent` annotation, keys table, replay and cleanup; key generated in the frontend.
   - Acceptance: the 50 parallel requests test passes; E2E scenarios 5 and 6 green.
4. **Resilience (\~1-2 weeks).** Simulated services, circuit breaker, bulkhead, retry, fallbacks, outbox and admin dashboard.
   - Acceptance: WireMock tests pass; E2E scenario 8 green.
5. **Rate limit and base security (\~1 week).** Bucket4j, refresh token rotation, double-submit CSRF, anti-enumeration, auditing and security headers.
   - Acceptance: E2E scenarios 7, 9 and 10 green; OWASP ZAP baseline scan without high alerts.
6. **Advanced security and performance (\~2 weeks).** 2FA with step-up, password recovery, encryption at rest, audit chain, indexes, N+1 tests, ETag, virtual threads and load testing.
   - Acceptance: E2E scenarios 11 and 12 green; p95 and Lighthouse targets met; query ceiling checked on every endpoint.
7. **Horizontal scale (\~1 week).** H2 in server mode, Redis for rate limit and TOTP, ShedLock on the jobs, Nginx with 3 instances, graceful shutdown and `X-Instance-Id`.
   - Acceptance: k6 compares 1 and 3 instances; killing an instance during the load neither loses nor duplicates transfers; the whole E2E suite green against the 3-instance environment.
8. **Showcase (\~1 week).** README with demo GIFs, k6 results, diagram, ADRs, Swagger and public deploy.
   - Acceptance: an evaluator runs the project in at most 2 commands and understands the concepts from the README.

**Definition of done, in any phase**

- [ ] New tests written and CI green
- [ ] Domain coverage ≥ 80%
- [ ] Relevant decisions recorded as ADRs (`docs/adr/`)
- [ ] READMEs (English and Portuguese) updated with what the phase delivered

## Risks, limitations and what would change in production

The simplifications are conscious choices for a portfolio. Documenting them in the README shows the author knows where each limit is.

| Limitation in the project | In production |
| --- | --- |
| A single H2 in server mode: bottleneck and single point of failure | Managed PostgreSQL with failover and read replicas, with Testcontainers in the tests |
| Load numbers measured with H2, optimistic and bound by the database | Repeat k6 against PostgreSQL in a production-like environment |
| A single Redis | Redis with Sentinel or Cluster |
| Nginx and fixed replicas in Docker Compose | Kubernetes with HPA, scaling by CPU or latency |
| Idempotency keys in the main database | Redis with TTL, or a dedicated partitioned table |
| Outbox published by polling with ShedLock | CDC (Debezium) or messaging (Kafka, RabbitMQ) |
| External services simulated in the app itself | Real services, with contract tests (Pact) |
| JWT secret and encryption key in environment variables | Secret vault or KMS with envelope encryption and key rotation; JWT in RS256 |
| Audit chain in the same database it protects | Periodic copy of the hashes to immutable storage (WORM) |
| Simulated inbox | A real transactional e-mail provider |
| Optimistic locking on heavily contended accounts | Pessimistic locking or a queue per account for high-volume accounts |

**Development risks**

- **Scope growing too much.** Mitigation: follow the phases and only start the next one once the previous one is accepted.
- **H2 behaving differently from PostgreSQL.** Mitigation: use `MODE=PostgreSQL` and avoid database-specific SQL.
- **Flaky E2E tests.** Mitigation: data isolated per test, waits on screen state instead of fixed time, trace enabled in CI.
- **Concepts that do not show to the evaluator.** Mitigation: resilience dashboard, GIFs in the README and a "How to test each concept in 1 minute" section.
