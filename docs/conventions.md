# Conventions

Rules that apply to every feature. A spec only repeats a rule from here to make an exception, and explains why.

## API

- Base `/api/v1`. Plural resources, kebab-case routes, camelCase JSON.
- Every error response follows Problem Details (RFC 9457), with `Content-Type: application/problem+json`.
- The `type` field is a stable kebab-case slug (e.g. `insufficient-balance`) that the frontend uses to pick the message. `title` is a short sentence in the language of `Accept-Language`, read from `problem.<type>.title` in the message bundles; it depends only on `type` and the language. `detail` is optional and never carries sensitive data.
- Validation errors (400, `type: validation-error`) carry `errors: [{ "field": "amount", "message": "..." }]`, with `message` in the language of the request.
- A new `type` gets a title in all four bundles (`messages*.properties`) and a message in all four frontend catalogs.
- No response carries a stack trace, class name or SQL.

### `type` catalog

| `type` | Status | When |
| --- | --- | --- |
| `validation-error` | 400 | Bean Validation, malformed JSON, invalid parameter |
| `invalid-cursor` | 400 | Statement cursor that does not decode |
| `invalid-token` | 400 | Unknown, expired or already used confirmation token (summary and activation) |
| `unauthorized` | 401 | Missing, invalid or expired access token |
| `invalid-credentials` | 401 | Login with unknown e-mail, wrong password or unconfirmed account |
| `invalid-refresh-token` | 401 | Missing, expired or revoked refresh token |
| `forbidden` | 403 | Role without permission or another user's resource |
| `account-blocked` | 403 | Blocked user account trying to move money |
| `not-found` | 404 | Unknown resource or route |
| `method-not-allowed` | 405 | HTTP method not supported by the route |
| `not-acceptable` | 406 | Response format requested in `Accept` not supported |
| `unsupported-media-type` | 415 | Request `Content-Type` not supported |
| `recipient-not-found` | 404 | Recipient lookup with no result (unknown or inactive, without distinction) |
| `concurrency-conflict` | 409 | Optimistic lock failed after the retries |
| `insufficient-balance` | 422 | Balance lower than the amount |
| `daily-limit-exceeded` | 422 | Transfer would exceed the daily limit |
| `self-transfer` | 422 | Destination equals source |
| `recipient-unavailable` | 422 | Recipient stopped being active between the lookup and the transfer |
| `internal-error` | 500 | Unexpected error; details only in the log |

Later phases add types (`idempotency-*`, `totp-*`, `rate-limited`, `fraud-check-unavailable`) in their own specs and in this catalog.

## Money

- Always `BigDecimal` with scale 2 and `RoundingMode.UNNECESSARY`: a value with more than two decimal places is rejected, never rounded.
- In the database, `DECIMAL(19,2)`.
- In JSON, a **string** with two decimal places (`"150.00"`), in requests and responses. Requests also accept a number, converted without loss. Strings avoid the precision loss of the JavaScript `number` ([ADR 0004](adr/0004-money-as-string-in-json.md)).
- Response DTOs use the `Money` type; request DTOs may use `BigDecimal` with `@Digits(integer = 17, fraction = 2)` so validation points at the field.
- In the frontend, values stay as strings until formatted with `formatBRL(value, language)`: always BRL (`R$`), with the separators of the interface language. No money arithmetic happens in the client.

## Dates and time zone

- Instants in UTC, `TIMESTAMP WITH TIME ZONE` in the database and ISO-8601 with `Z` in JSON.
- "Day" rules (daily limit, statement filters) use the `America/Sao_Paulo` time zone.
- Code gets the time from an injected `Clock`, so tests control time.

## Identifiers

- Application-generated UUID for every entity.
- Internal user IDs never appear in responses to other users; recipient lookup returns the account ID, which only serves as a transfer destination.

## Backend

- Feature packages under `com.portwallet`: `auth`, `account`, `ledger`, `transfer`, `mock`, `shared` (and, in later phases, `idempotency`, `resilience`, `ratelimit`, `admin`).
- Inside each package: `api` (controllers and DTOs), `domain` (entities and rules), `application` (use cases, transactions) and `infra` (repositories and integrations), created as needed.
- Controllers do not access repositories; business rules do not live in controllers. Enforced by ArchUnit from Phase 2 on.
- DTOs are `record`s with Bean Validation. JPA entities never leave the `application` layer.
- OpenAPI documentation for each endpoint: `@Operation` with a summary, `@ApiResponse` for the success case, `@Schema(example = ...)` on DTO fields, an empty `@SecurityRequirements` on public endpoints and one `@ApiProblem` per business error the endpoint can return. Generic errors (400 validation, 401 and 500) are added automatically and must not be declared.
- `spring.jpa.open-in-view=false`; reads through DTO projections.
- Flyway migrations in `db/migration`, named `V<N>__<description_in_snake_case>.sql`, never edited once published. SQL portable between H2 (`MODE=PostgreSQL`) and PostgreSQL.
- Logs never contain a password, token, TOTP secret or full CPF.

## Frontend

- Routes in `src/routes`, features in `src/features/<name>`, shared components in `src/components`, HTTP client in `src/api`.
- Server state with TanStack Query; forms with React Hook Form + Zod; HTTP with Axios.
- Access token only in memory, never in `localStorage` or `sessionStorage`.
- No user-facing text in components: every text is a key in the catalogs of `src/i18n/locales` (pt-BR is the source; the other catalogs are typed against it), rendered with `t()` or `<Trans>`. Zod schemas use keys as messages.
- API errors mapped by the Problem Details `type` to messages in the catalogs (`problemKey`).
- Test selectors by ARIA role and `data-testid`. No `waitForTimeout` in E2E tests.
- Build flags (`VITE_*`) are public, since they end up in the bundle. The dev and E2E values live in `frontend/.env.development`, which is versioned (CI and fresh clones depend on it). Never put secrets there; personal overrides go in `.env*.local`, which stays ignored.

## Language

- The UI, API error titles and messages and the e-mails exist in Brazilian Portuguese (default and fallback), English, Spanish and French ([ADR 0007](adr/0007-multilingual-product.md), [nfr-i18n](features/nfr-i18n/spec.md)). The frontend sends its language in `Accept-Language`; e-mails use the language saved on the user.
- Backend messages with arguments go through `MessageFormat`: use the typographic apostrophe (`’`), never `'`. French uses a no-break space before `:`, `?`, `!` and `;`.
- OpenAPI descriptions stay in Brazilian Portuguese.
- Code, test names, commits and all documentation are in English ([ADR 0006](adr/0006-english-docs-portuguese-ui.md)).
- The root README exists in English (`README.md`) and Portuguese (`README.pt-BR.md`); both change together.

## Tests

- Test names describe behavior (`rejectsTransferAboveBalance`).
- Integration tests use the `test` profile and create their own data; no test depends on execution order.
- Every numbered rule in a spec has at least one test that proves it.
