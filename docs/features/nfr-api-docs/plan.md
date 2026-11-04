# Plan: API documentation (OpenAPI and Swagger UI)

- **Status:** Done
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [nfr-application-base](../nfr-application-base/plan.md)

## Steps

### 1. Dependency and configuration

- [x] `springdoc-openapi-starter-webmvc-ui` in `pom.xml`
- [x] springdoc properties in `application.yml`, off by default and on in `dev | demo`
- [x] Documentation routes permitted in `SecurityConfig`
- **Files:** `backend/pom.xml`, `application.yml`, `shared/security/SecurityConfig.java`
- **Validation:** `./mvnw verify` green

### 2. Metadata, authentication, errors and money

- [x] `OpenApiConfig` with `OpenAPI`, `bearerAuth`, relative server, response customizer and the `Money` schema
- **Files:** `shared/openapi/OpenApiConfig.java`
- **Validation:** `OpenApiDevProfileTest` and `OpenApiDemoProfileTest` (same contract in `OpenApiDocsContract`) and `OpenApiDisabledTest` (`test` and `e2e` profiles)

### 3. Nginx

- [x] Proxy of `/v3/api-docs` and `/swagger-ui`
- **Files:** `frontend/nginx.conf`
- **Validation:** `docker compose up --build --wait` and `curl` of both routes through port 7000

### 4. Documentation

- [x] README with the Swagger UI address
- [x] `docs/` index updated

## Final verification

- [x] `./mvnw verify` green
- [x] Existing E2E green in Chromium and Firefox
- [x] Swagger UI reviewed in the browser through Nginx
- [x] Spec updated with what changed during implementation
