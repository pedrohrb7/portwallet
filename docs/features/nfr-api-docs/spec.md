# Spec: API documentation (OpenAPI and Swagger UI)

- **Status:** Done
- **Phase:** 2 (brought forward from phase 8)
- **PRD:** Stack and architecture (springdoc-openapi); API
- **Depends on:** [nfr-application-base](../nfr-application-base/spec.md)
- **Plan:** [plan.md](plan.md)

## Goal

Publish the API's OpenAPI documentation and Swagger UI from Phase 2 on, so every endpoint is born documented and whoever evaluates the project can explore and call the API from the browser.

## Scope

**Includes**

- springdoc-openapi with the document at `/v3/api-docs` and Swagger UI at `/swagger-ui`.
- Exposed only in the `dev` and `demo` profiles.
- API metadata, Bearer authentication scheme, error responses in Problem Details and the `Money` type documented as a string.
- Proxy in the `docker compose` Nginx.

**Later**

- Request and response examples per endpoint: each feature documents its own endpoints with the springdoc annotations.

## Behavior

1. In the `dev` and `demo` profiles, `GET /v3/api-docs` returns the OpenAPI 3.1 document and `GET /swagger-ui` opens Swagger UI. Both are public, without token.
2. In any other profile (including the default, `test` and `e2e`), both routes do not exist and answer 404 `not-found`.
3. The document describes only the business API (`/api/v1/**`); Actuator and `/mock/**` are left out.
4. The document declares the `bearerAuth` scheme (HTTP Bearer, JWT) and applies it to every operation by default. Public endpoints (registration, login and so on) drop the requirement with an empty `@SecurityRequirements`.
5. The errors of each operation are documented as `application/problem+json`, with the `Problem` schema (catalog in the [conventions](../../conventions.md#api)) and **one example per `type`**, each with the real `type`, `title` and status:
   - 400 `validation-error`, only when the operation takes a body or parameters;
   - 401 `unauthorized`, only when the operation requires a token (never on public endpoints);
   - 500 `internal-error`, always;
   - the business errors the controller declares with `@ApiProblem` (e.g. 400 `invalid-token`, 422 `insufficient-balance`).

   Errors with the same status share the same response, with one example per `type` and the description as a list. Responses appear in status order.
6. Success responses and request bodies are `application/json`, with examples on the fields.
7. `Money` shows as a `string` with the pattern `^-?\d+\.\d{2}$` and example `"150.00"`, following [ADR 0004](../../adr/0004-money-as-string-in-json.md).
8. The document uses a relative server (`/`), so "Try it out" works both directly on the API (`:8080`) and behind Nginx (`:3000`).
9. In Swagger UI, the "Authorize" button accepts the access token returned by login.

## API

| Method | Endpoint | Access | Profiles |
| --- | --- | --- | --- |
| GET | `/v3/api-docs` | Public | `dev`, `demo` |
| GET | `/swagger-ui` | Public | `dev`, `demo` |

## Data model

No migration.

## Backend

- Dependency `org.springdoc:springdoc-openapi-starter-webmvc-ui`.
- `application.yml`: `springdoc.api-docs.enabled` and `springdoc.swagger-ui.enabled` off by default and on in the `dev | demo` block; `springdoc.paths-to-match=/api/v1/**`; `springdoc.default-produces-media-type` and `default-consumes-media-type` set to `application/json` (without them, success showed as `*/*`); `springdoc.swagger-ui.path=/swagger-ui`.
- `shared.openapi.OpenApiConfig` (active only with `springdoc.api-docs.enabled=true`): `OpenAPI` bean with title, version, description, relative server and the `bearerAuth` scheme; an `OperationCustomizer` that builds each operation's error responses from rule 5 (public means an empty `@SecurityRequirements`; input means any parameter other than `AuthenticatedUser`); an `OpenApiCustomizer` that registers the `Problem` schema and sorts responses by status; the `Money` schema replaced with `string` through `SpringDocUtils`. `Problem` is registered in the customizer, not in the `OpenAPI` bean, because springdoc drops schemas that nothing references yet.
- `SecurityConfig`: `/v3/api-docs/**`, `/swagger-ui/**` and `/swagger-ui.html` permitted. With springdoc off, the route does not exist and falls into the Problem Details 404.

## Frontend

- `nginx.conf`: proxy of `/v3/api-docs` and `/swagger-ui` to the API, with `Host` and `X-Forwarded-Host` set to `$http_host`. Without `X-Forwarded-Host`, springdoc reads only `X-Forwarded-Proto` and builds URLs without the public port (`http://localhost/...` instead of `http://localhost:3000/...`). Vite needs no proxy: in development, Swagger UI is opened directly at `http://localhost:8080/swagger-ui`.

## Security

- The documentation does not exist outside `dev` and `demo`, so the API is not mapped out in a real environment.
- The document carries no secrets nor examples with real data.

## Tests

| Layer | Scenario |
| --- | --- |
| Integration | `OpenApiDevProfileTest` and `OpenApiDemoProfileTest`, with the shared contract `OpenApiDocsContract`: `/v3/api-docs` answers 200 without token with OpenAPI 3.1, `bearerAuth`, server `/`, `Problem` schema; a protected endpoint without input documents only 200, 401 and 500, and a public endpoint never documents 401; one example per declared `type`, including two `type`s in the same 400; responses in status order; success as `application/json`; `Money` as a string; nothing outside `/api/v1`; `/swagger-ui` redirects to `index.html`, which opens without token. `OpenApiDisabledTest` (`test` and `e2e` profiles): both routes answer 404 `not-found` |
| Docker | Through Nginx on port 3000: `/v3/api-docs`, `/swagger-ui` (302 to `http://localhost:3000/swagger-ui/index.html`), `/v3/api-docs/swagger-config` with the public port and Swagger UI reviewed in the browser |

## Acceptance criteria

- [x] Swagger UI reachable in `dev` and `demo`, absent in the other profiles
- [x] Errors and money documented according to the conventions
- [x] Document validated by tests on every build

## Decisions

- **Brought forward from phase 8:** documenting each endpoint together with the feature that creates it costs less and keeps the document always up to date.
- **Only in `dev` and `demo`:** evaluators access it through `docker compose`; real environments do not expose the API map.
- **Errors per operation, with `@ApiProblem`:** the first version added 400, 401 and 500 to every operation with a generic `validation-error` example. Public endpoints showed 401 and a 422 appeared with a validation error example. Now the generic ones depend on the operation and each business error is declared next to its endpoint, with its own example.

## Open questions

- None.
