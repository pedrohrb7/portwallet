# Plano: RF-02 Login e refresh

- **Status:** Rascunho
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rf-01-cadastro](../rf-01-cadastro/plan.md)

## Passos

### 1. Migration e domínio

- [ ] `V5__refresh_tokens.sql`, entidade `RefreshToken` e repositório
- **Arquivos:** `db/migration/V5__refresh_tokens.sql`, `auth/domain/RefreshToken.java`, `auth/infra/**`
- **Validação:** `./mvnw verify`

### 2. Login

- [ ] `PasswordEncoder` BCrypt custo 12 (se ainda não existir do RF-01)
- [ ] `LoginService`, `LoginRequest`, `AuthResponse`, `RefreshCookie`, `AuthController.login`
- **Arquivos:** `auth/application/LoginService.java`, `auth/api/**`
- **Validação:** `LoginTest` (sucesso, três falhas com corpo idêntico, atributos do cookie)

### 3. Refresh

- [ ] `RefreshTokenService.rotate` e `AuthController.refresh`
- **Arquivos:** `auth/application/RefreshTokenService.java`, `auth/api/AuthController.java`
- **Validação:** `RefreshTest` (rotação, token antigo recusado, expirado, revogado, validade da família)

### 4. Frontend

- [ ] `login()` e `restore()` em `session.ts`; página `/login`; nome no cabeçalho
- **Arquivos:** `frontend/src/features/auth/**`, `frontend/src/routes/**`
- **Validação:** `session.test.ts`, `LoginPage.test.tsx`

### 5. E2E

- [ ] Helper `createActiveUser(request)` em `e2e/support/` (cadastro + confirmação pela API e pela caixa de entrada)
- [ ] `e2e/login.spec.ts`: login, recarga mantendo a sessão, credencial errada com mensagem genérica
- **Validação:** `npm run e2e` verde em Chromium e Firefox

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] E2E verde em Chromium e Firefox
- [ ] Tela de login revisada em desktop e mobile
- [ ] Spec atualizada com o que mudou na implementação

## Riscos

- Duas abas fazendo refresh ao mesmo tempo: uma recebe 401 porque o token já foi rotacionado. Aceito nesta fase; a fase 5 trata o reuso e pode adicionar uma janela de tolerância.
