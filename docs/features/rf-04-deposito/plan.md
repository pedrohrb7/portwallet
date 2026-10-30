# Plano: RF-04 Depósito simulado

- **Status:** Rascunho
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/plan.md), [rf-08-saldo](../rf-08-saldo/plan.md)

## Passos

### 1. Backend

- [ ] `DepositRequest`, `DepositResponse`, `DepositService`, `DepositController`
- [ ] `AccountBlockedException` (403 `account-blocked`)
- **Arquivos:** `transfer/api/**`, `transfer/application/DepositService.java`, `account/domain/AccountBlockedException.java`
- **Validação:** `DepositTest` (sucesso, limites de valor, bloqueado, invariantes)

### 2. Frontend

- [ ] `MoneyInput` e `DepositPage`; link no menu e no dashboard
- **Arquivos:** `frontend/src/components/MoneyInput.tsx`, `frontend/src/features/deposit/**`, `frontend/src/routes/router.tsx`
- **Validação:** `MoneyInput.test.tsx`, `DepositPage.test.tsx`

### 3. E2E

- [ ] `e2e/deposit.spec.ts` (cenário 2)
- [ ] Helper `deposit(request, token, amount)` para preparar saldo nos próximos testes
- **Validação:** `npm run e2e` verde em Chromium e Firefox

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] E2E verde em Chromium e Firefox
- [ ] Tela revisada em desktop e mobile
- [ ] Spec atualizada com o que mudou na implementação
