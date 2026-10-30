# Plano: RF-07 Extrato

- **Status:** Rascunho
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rf-05-transferencia](../rf-05-transferencia/plan.md), [rf-04-deposito](../rf-04-deposito/plan.md)

## Passos

### 1. Cursor

- [ ] `StatementCursor` com Base64URL e validação
- **Arquivos:** `ledger/domain/StatementCursor.java`
- **Validação:** `StatementCursorTest`

### 2. Consulta

- [ ] `StatementRepository` com `JdbcClient`, filtros e `size + 1`
- [ ] Verificar com `EXPLAIN` no H2 que a consulta usa `ix_ledger_entries_statement` e resolver a questão em aberto da spec
- **Arquivos:** `ledger/infra/StatementRepository.java`
- **Validação:** `StatementRepositoryTest` (ordem, filtros, borda de meia-noite)

### 3. Endpoint

- [ ] `StatementQuery`, `StatementService`, `StatementController`, `StatementResponse`
- **Arquivos:** `ledger/application/StatementService.java`, `ledger/api/**`
- **Validação:** `StatementApiTest` (percurso completo sem repetição, inserção entre páginas, erros 400, isolamento por usuário)

### 4. Frontend

- [ ] `useStatement`, `StatementPage` com filtros na URL e carregamento incremental
- [ ] `RecentActivity` no dashboard
- **Arquivos:** `frontend/src/features/statement/**`, `frontend/src/features/account/RecentActivity.tsx`
- **Validação:** `StatementPage.test.tsx`

### 5. E2E

- [ ] Completar o cenário 3 em `e2e/transfer.spec.ts`: extrato dos dois usuários
- [ ] `e2e/statement.spec.ts`: filtro por tipo refletido na URL e mantido ao recarregar
- **Validação:** `npm run e2e` verde em Chromium e Firefox

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] Cenários E2E 1 a 4 verdes em Chromium e Firefox (aceite da Fase 2)
- [ ] Extrato e dashboard revisados em desktop e mobile
- [ ] Spec atualizada com o que mudou na implementação
- [ ] README atualizado com a entrega da Fase 2

## Riscos

- Lançamentos com o mesmo `created_at`: o `id` desempata, e o teste cria vários lançamentos com o `Clock` parado para provar isso.
