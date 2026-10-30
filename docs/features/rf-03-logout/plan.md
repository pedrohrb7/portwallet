# Plano: RF-03 Logout

- **Status:** Aprovada
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rf-02-login](../rf-02-login/plan.md)

## Passos

### 1. Backend

- [ ] `RefreshTokenService.revoke` e `AuthController.logout`
- **Arquivos:** `auth/application/RefreshTokenService.java`, `auth/api/AuthController.java`
- **Validação:** `LogoutTest` (revogação, sem cookie, outra família intacta)

### 2. Frontend

- [ ] `session.logout()` e botão "Sair" no cabeçalho
- **Arquivos:** `frontend/src/features/auth/session.ts`, `frontend/src/routes/AppLayout.tsx`
- **Validação:** `session.test.ts` (limpeza mesmo com erro de rede)

### 3. E2E

- [ ] Cenário de sair e recarregar em `e2e/login.spec.ts`
- **Validação:** `npm run e2e` verde em Chromium e Firefox

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] E2E verde em Chromium e Firefox
- [ ] Spec atualizada com o que mudou na implementação
