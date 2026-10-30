# Plano: Documentação da API (OpenAPI e Swagger UI)

- **Status:** Concluída
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rnf-base-aplicacao](../rnf-base-aplicacao/plan.md)

## Passos

### 1. Dependência e configuração

- [x] `springdoc-openapi-starter-webmvc-ui` no `pom.xml`
- [x] Propriedades do springdoc no `application.yml`, desligadas por padrão e ligadas em `dev | demo`
- [x] Rotas da documentação liberadas no `SecurityConfig`
- **Arquivos:** `backend/pom.xml`, `application.yml`, `shared/security/SecurityConfig.java`
- **Validação:** `./mvnw verify` verde

### 2. Metadados, autenticação, erros e dinheiro

- [x] `OpenApiConfig` com `OpenAPI`, `bearerAuth`, servidor relativo, customizer de respostas e esquema de `Money`
- **Arquivos:** `shared/openapi/OpenApiConfig.java`
- **Validação:** `OpenApiDevProfileTest` e `OpenApiDemoProfileTest` (mesmo contrato em `OpenApiDocsContract`) e `OpenApiDisabledTest` (perfis `test` e `e2e`)

### 3. Nginx

- [x] Proxy de `/v3/api-docs` e `/swagger-ui`
- **Arquivos:** `frontend/nginx.conf`
- **Validação:** `docker compose up --build --wait` e `curl` das duas rotas pela porta 3000

### 4. Documentação

- [x] README com o endereço do Swagger UI
- [x] Índice de `docs/` atualizado

## Verificação final

- [x] `./mvnw verify` verde
- [x] E2E existente verde em Chromium e Firefox
- [x] Swagger UI revisado no navegador pelo Nginx
- [x] Spec atualizada com o que mudou na implementação
