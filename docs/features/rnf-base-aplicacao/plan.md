# Plano: Base da aplicação

- **Status:** Concluída
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rnf-fundacao](../rnf-fundacao/plan.md)

## Passos

### 1. Dependências do backend

- [x] Adicionar `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-security-test` e `archunit-junit5`
- **Arquivos:** `backend/pom.xml`
- **Validação:** `./mvnw verify` verde (o teste de health continua passando com o health liberado)

### 2. Problem Details

- [x] `ApiException` e `GlobalExceptionHandler`, incluindo validação, JSON malformado, tipo de parâmetro e 500 genérico
- [x] `spring.mvc.problemdetails.enabled=true` e 404 para rotas inexistentes
- **Arquivos:** `backend/src/main/java/com/portwallet/shared/error/**`
- **Validação:** `ProblemDetailsTest` com um controller de teste que lança cada tipo de erro

### 3. Dinheiro e relógio

- [x] Record `Money`, serializador e desserializador Jackson
- [x] `TimeConfig` com `Clock` e `BUSINESS_ZONE`
- **Arquivos:** `shared/money/**`, `shared/time/**`
- **Validação:** `MoneyTest` e `MoneyJsonTest`

### 4. Segurança e JWT

- [x] `JwtProperties` validadas, `JwtService`, `SecurityConfig`, `CurrentUser`
- [x] Entry point e access denied handler em Problem Details
- [x] Segredos dos perfis `dev`, `e2e` e `test`; `JWT_SECRET` no `docker-compose.yml`, com padrão de demonstração
- **Arquivos:** `shared/security/**`, `application.yml`, `src/test/resources/application-test.yml`, `docker-compose.yml`
- **Validação:** `SecurityTest` (401 sem token, expirado, assinatura errada; 403 sem perfil); `JwtSecretRequiredTest`

### 5. ArchUnit

- [x] `ArchitectureTest` com as regras da spec
- **Arquivos:** `backend/src/test/java/com/portwallet/ArchitectureTest.java`
- **Validação:** o teste falha se um controller injetar um repositório (verificado com uma classe temporária)

### 6. Dependências e cliente HTTP do frontend

- [x] Instalar `react-router`, `@tanstack/react-query`, `axios`, `react-hook-form`, `zod`, `@hookform/resolvers`
- [x] `api/client.ts`, `api/problem.ts`, `lib/money.ts`
- **Arquivos:** `frontend/package.json`, `frontend/src/api/**`, `frontend/src/lib/**`
- **Validação:** `client.test.ts` (refresh único para 401 simultâneas) e `money.test.ts`

### 7. Sessão, rotas e layout

- [x] `session.ts` e provedor de sessão
- [x] `router.tsx`, `RequireAuth`, `AppLayout`, `PublicLayout`, página 404
- [x] `ApiStatus` passa a usar o cliente compartilhado
- **Arquivos:** `frontend/src/features/auth/**`, `frontend/src/routes/**`, `frontend/src/main.tsx`, `frontend/src/App.tsx`
- **Validação:** `RequireAuth.test.tsx`; E2E de health continua verde

## Verificação final

- [x] `./mvnw verify` verde
- [x] `npm run lint && npm run typecheck && npm test` verdes
- [x] E2E existente verde em Chromium e Firefox
- [x] ArchUnit falha com uma classe violadora temporária (verificado e removida)
- [x] Layout revisado em desktop e mobile
- [x] Spec atualizada com o que mudou na implementação
- [x] ADR 0004 sobre dinheiro como string no JSON

## Riscos

- Segurança bloqueando o health ou o proxy do Vite: o teste de health existente é o primeiro a rodar depois do passo 4.
