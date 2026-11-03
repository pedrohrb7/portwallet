# Spec: Application base

- **Status:** Done
- **Phase:** 2
- **PRD:** API (Problem Details), Security, Frontend (guidelines)
- **Depends on:** [nfr-foundation](../nfr-foundation/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Build the common infrastructure every Phase 2 feature uses: error handling, money, clock, a JWT security chain, architecture rules and the frontend skeleton (routes, HTTP client, session and layout). No business rule goes here.

## Scope

**Includes**

- Problem Details for every error, following the [conventions](../../conventions.md#api).
- `Money` type and serialization of amounts as strings.
- Injectable `Clock` with the `America/Sao_Paulo` business time zone.
- Stateless Spring Security with HS256 JWT issuing and validation.
- ArchUnit with the layer rules.
- Frontend: React Router, TanStack Query, Axios, React Hook Form, Zod, authenticated layout, route guard and error mapping.

**Later**

- CORS and security headers (CSP and so on): phase 5. In every phase the frontend calls the API from the same origin (Vite or Nginx proxy), so CORS is not needed to work.
- Double-submit CSRF on refresh and logout: phase 5.
- `X-Correlation-Id` and JSON logs: phase 4 (observability).

## Behavior

1. Any unhandled exception becomes a 500 with `type: internal-error` and no internal details; the full exception goes to the log.
2. A Bean Validation failure, malformed JSON or a parameter with the wrong type becomes 400 `validation-error` with the `errors` list.
3. A request without a token, or with an invalid or expired token, to a protected endpoint becomes 401 `unauthorized`, in the same Problem Details format.
4. An authenticated user without the required role gets 403 `forbidden`.
5. An unknown route under `/api` becomes 404 `not-found` in Problem Details.
6. Money values are read as a string or a number and always written as a string with two decimal places; more than two decimal places is a 400.
7. The access token is an HS256 JWT with `sub` (user ID), `role` (`USER` or `ADMIN`), `iat` and `exp` (15 min). The secret comes from `JWT_SECRET` (at least 32 bytes); the application does not start without it outside the `dev`, `e2e` and `test` profiles.
8. The frontend keeps the access token only in memory. On load, it tries to restore the session by calling `/auth/refresh`; on a 401 from another call, it tries one refresh and repeats the request only once.
9. Protected routes redirect to `/login` without a session, keeping the original route to come back to after login.

## API

No new endpoints. Error format:

```json
{
  "type": "validation-error",
  "title": "Dados inválidos",
  "status": 400,
  "instance": "/api/v1/deposits",
  "errors": [{ "field": "amount", "message": "deve ser maior que zero" }]
}
```

## Data model

No migration.

## Backend

Package `com.portwallet.shared`:

- `error.ApiException` (base with `type`, status and title, and the `customize` hook for extra properties), subclassed per rule in the features.
- `error.Problems`: factory of the generic catalog types and the mapping of framework statuses (404, 405, 406, 415) to `type`.
- `error.GlobalExceptionHandler` (`@RestControllerAdvice` extending `ResponseEntityExceptionHandler`), with Spring's `ProblemDetail`. JSON read errors point at the field through Jackson's path (e.g. `amount`), or `body` for malformed JSON.
- `security.ProblemSecurityHandlers`: Spring Security entry point and access denied handler, for 401 (with `WWW-Authenticate: Bearer`) and 403 in the same format.
- `money.Money` (record with a scale-2 `BigDecimal`, operations `plus`, `minus`, `isGreaterThan`, `max`) and `money.MoneyJson` (`@JsonComponent` with serializer and deserializer).
- `time.TimeConfig`: `Clock` bean (UTC) and the constant `BUSINESS_ZONE = America/Sao_Paulo`.
- `security.SecurityConfig`: stateless `SecurityFilterChain`, `csrf` off in this phase, OAuth2 Resource Server with an HS256 `NimbusJwtDecoder`, `@EnableMethodSecurity`. Besides expiration, the decoder checks that `sub` is a UUID and that `role` is a known role; any other token is a 401.
- `security.JwtService`: issues tokens with `NimbusJwtEncoder`.
- `security.Role` (`USER`, `ADMIN`) and `security.AuthenticatedUser(UUID id, Role role)`, received by controllers as a method parameter through a `HandlerMethodArgumentResolver`.

Configuration `portwallet.security.jwt.secret` and `portwallet.security.jwt.access-token-ttl` (default `PT15M`) in `JwtProperties`, which refuses a secret shorter than 32 bytes and never prints it in `toString`.

`test` profile in `src/test/resources/application-test.yml` with a fixed secret, activated by Surefire. `dev` and `e2e` profiles with a development secret declared in the YAML and marked as insecure. `docker-compose.yml` starts in the `demo` profile with a demo secret, also marked as insecure and replaceable through `JWT_SECRET`.

## Frontend

Dependencies: `react-router`, `@tanstack/react-query`, `axios`, `react-hook-form`, `zod`, `@hookform/resolvers`.

- `src/api/client.ts`: `api` instance with `baseURL: '/api/v1'`, an interceptor that injects `Authorization`, and a 401 interceptor that repeats the request once after `refreshSession()`. An `authClient` instance, without interceptors, for the cookie endpoints. `refreshSession()` shares the same promise among simultaneous callers.
- `src/api/problem.ts`: `Problem` type, `toProblem(error)` (a network failure becomes `network-error`) and `messageFor(problem)`, which translates the `type` into a message; an unknown type falls back to a generic message.
- `src/features/auth/sessionStore.ts`: external store (read with `useSyncExternalStore`) with the in-memory token and the session state (`loading`, `authenticated`, `anonymous`). It lives outside React so the Axios interceptor can read it.
- `src/routes/`: `router.tsx`; `RootLayout` (banner and session restore on first load); `PublicLayout`; `RequireAuth`; `AppLayout` (header with navigation and user name); `navigation.ts` with the menu items; `NotFoundPage`; `HomePage` (the phase 1 home screen, public until the FR-08 dashboard takes `/`).
- `src/lib/money.ts`: `formatBRL(value: string)`.
- `src/test/axiosMock.ts` and `src/test/renderRoutes.tsx`: test utilities.

## Security

- Endpoints under `/api/v1/**` require authentication, except the public ones declared by each feature.
- `/actuator/health/**` is public; the rest of Actuator requires `ADMIN`.
- The JWT secret is never logged.

## Tests

| Layer | Scenario |
| --- | --- |
| Unit | `Money` rejects more than two decimal places and serializes as a string (`MoneyTest`, `MoneyJsonTest`); `JwtProperties` refuses a short secret and does not print it |
| Integration | `ProblemDetailsTest`: 400 with `errors` (Bean Validation, malformed JSON, money with three decimal places, parameter type, missing parameter, parameter constraint), `ApiException` with an extra property, 404, 405, 415 and 500 without details. `SecurityTest`: 401 without token, expired, wrong signature, unknown role and a `sub` that is not a UUID; 403 without role; `AuthenticatedUser` resolved. `JwtSecretRequiredTest`: the app does not start without `JWT_SECRET` |
| Architecture | `ArchitectureTest`: controllers do not depend on repositories nor on `infra`; `domain` does not depend on `api` nor on `infra`; `shared` does not depend on the features. Verified with a temporary violating class |
| Unit (frontend) | `client.test.ts`: a single refresh for three simultaneous 401s, with the three requests repeated; session cleared when the refresh fails. `RequireAuth.test.tsx`, `router.test.tsx`, `problem.test.ts`, `money.test.ts`, `health.test.ts` |

## Acceptance criteria

- [x] Every API error comes out as Problem Details with a catalog `type`
- [x] JWT issued and validated with a secret from configuration
- [x] ArchUnit rules active on `verify`
- [x] Frontend with routes, authenticated layout and HTTP client ready for the features

## Decisions

- **HS256 instead of RS256:** a symmetric key is enough with a single issuer and verifier. The PRD already records RS256 with KMS as the production evolution.
- **Money as a string in JSON:** avoids the precision loss of the JavaScript `number`. Recorded in [ADR 0004](../../adr/0004-money-as-string-in-json.md).
- **Authenticated user as a method parameter:** controllers receive `AuthenticatedUser` instead of reading the `SecurityContext`, which makes the dependency explicit and easy to test.
- **Session store outside React:** the Axios interceptor needs the token outside components; `useSyncExternalStore` connects the same store to the UI.
- **`/` stays public in this feature:** the phase 1 home screen stays at `/` until the FR-08 dashboard replaces it, so the health E2E test keeps working.
- **CSRF off until phase 5:** the API only accepts authentication through the `Authorization` header in this phase, except refresh and logout, which come with full protection in phase 5.

## Open questions

- None.
