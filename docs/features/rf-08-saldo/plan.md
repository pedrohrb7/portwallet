# Plano: RF-08 Saldo

- **Status:** Aprovada
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/plan.md)

## Passos

### 1. Backend

- [ ] `AccountQueryService.me` com projeção e soma de `usedToday`
- [ ] `AccountController` e `AccountResponse`
- **Arquivos:** `account/application/AccountQueryService.java`, `account/api/**`
- **Validação:** `AccountMeTest` (conta nova, limite com `Clock` perto da meia-noite, `blocked`)

### 2. Frontend

- [ ] `useAccount` e `DashboardPage` com saldo, barra de limite e aviso de bloqueio
- [ ] Rota `/` aponta para o dashboard
- **Arquivos:** `frontend/src/features/account/**`, `frontend/src/routes/router.tsx`
- **Validação:** `DashboardPage.test.tsx`

### 3. E2E

- [ ] `e2e/register.spec.ts` passa a terminar no dashboard com R$ 0,00 (cenário 1 completo)
- **Validação:** `npm run e2e` verde em Chromium e Firefox

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] E2E verde em Chromium e Firefox
- [ ] Dashboard revisado em desktop e mobile
- [ ] Spec atualizada com o que mudou na implementação
