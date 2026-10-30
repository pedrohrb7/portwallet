# Plano: RF-06 Consulta de transferência

- **Status:** Aprovada
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rf-05-transferencia](../rf-05-transferencia/plan.md)

## Passos

### 1. Backend

- [ ] Projeção da transferência com contas e nomes; `TransferQueryService.get` com a checagem de propriedade
- [ ] `TransferController.get` e `TransferDetailResponse`
- **Arquivos:** `transfer/application/TransferQueryService.java`, `transfer/api/**`, `ledger/infra/TransactionRepository.java`
- **Validação:** `TransferDetailTest` (remetente, destinatário, terceiro, inexistente, depósito, malformado)

### 2. Frontend

- [ ] `TransferDetailPage` e rota `/transfers/:id`; link no resultado da transferência
- **Arquivos:** `frontend/src/features/transfer/TransferDetailPage.tsx`, `frontend/src/routes/router.tsx`
- **Validação:** `TransferDetailPage.test.tsx`

### 3. E2E

- [ ] Abrir o comprovante a partir do resultado em `e2e/transfer.spec.ts`
- **Validação:** `npm run e2e` verde em Chromium e Firefox

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] E2E verde em Chromium e Firefox
- [ ] Comprovante revisado em desktop e mobile
- [ ] Spec atualizada com o que mudou na implementação
