# Plano: Fundação

- **Status:** Concluída
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** nenhum

## Passos

### 1. Ferramentas fixadas

- [x] `.sdkmanrc` com Temurin 21.0.13 e Maven 3.9.16
- [x] `.editorconfig` e `.gitignore` da raiz
- **Validação:** `sdk env install` instala as versões

### 2. Backend

- [x] `pom.xml` com Spring Boot 3.5.16, Java 21, Web, Validation, Data JPA, Actuator, Flyway e H2
- [x] Maven Wrapper 3.3.4 apontando para o Maven 3.9.16
- [x] Mockito como agente da JVM no Surefire; relatório JaCoCo no `verify`
- [x] `application.yml` e migration `V1__baseline.sql`
- **Arquivos:** `backend/pom.xml`, `backend/src/main/**`, `backend/.mvn/**`, `backend/mvnw*`
- **Validação:** `HealthEndpointTest` (5 testes) com `./mvnw verify`, sem avisos

### 3. Frontend

- [x] Projeto Vite `react-ts`, sem o boilerplate do template
- [x] Tailwind 4 (`@tailwindcss/vite`), Prettier, oxlint, Vitest + Testing Library
- [x] `DemoBanner`, `ApiStatus` e `api/health.ts`
- [x] Scripts `typecheck`, `test`, `format`, `format:check`, `e2e`
- **Arquivos:** `frontend/**`
- **Validação:** `npm run lint && npm run format:check && npm run typecheck && npm test && npm run build`

### 4. E2E

- [x] `playwright.config.ts` com Chromium e Firefox e `webServer` para backend e Vite
- [x] `e2e/health.spec.ts`
- **Validação:** `npm run e2e` verde nos dois navegadores

### 5. Containers

- [x] `backend/Dockerfile` multi-stage com camadas do Spring Boot, usuário sem privilégio e healthcheck
- [x] `frontend/Dockerfile` e `nginx.conf` com proxy e cache
- [x] `docker-compose.yml` com `api` e `web`
- **Validação:** `docker compose up --build --wait`, health pela porta 3000, headers de cache conferidos

### 6. CI

- [x] `.github/workflows/ci.yml` com jobs `backend`, `frontend`, `e2e` e `docker`
- **Validação:** 4 jobs verdes no GitHub Actions

### 7. Documentação

- [x] README com execução, versões e CI
- [x] ADRs 0001 e 0002

## Verificação final

- [x] `./mvnw verify` verde
- [x] `npm run lint && npm run typecheck && npm test` verdes
- [x] Cenário E2E verde em Chromium e Firefox
- [x] Tela revisada em desktop e mobile
- [x] CI verde no GitHub
