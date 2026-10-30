# Plano: RF-17 Busca de destinatário

- **Status:** Aprovada
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rf-01-cadastro](../rf-01-cadastro/plan.md), [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/plan.md)

## Passos

### 1. Máscara de nome

- [ ] `NameMasker`
- **Arquivos:** `account/domain/NameMasker.java`
- **Validação:** `NameMaskerTest`

### 2. Busca

- [ ] `RecipientService.lookup` (e-mail ou CPF pelo blind index) e `RecipientController`
- **Arquivos:** `account/application/RecipientService.java`, `account/api/RecipientController.java`
- **Validação:** `RecipientLookupTest`

### 3. Recentes

- [ ] Consulta de destinatários distintos com projeção e `RecipientService.recent`
- **Arquivos:** `account/application/RecipientService.java`, `ledger/infra/TransactionRepository.java`
- **Validação:** `RecentRecipientsTest`

### 4. Frontend

- [ ] `RecipientStep` com busca e recentes
- **Arquivos:** `frontend/src/features/transfer/**`
- **Validação:** `RecipientStep.test.tsx`

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] Spec atualizada com o que mudou na implementação
