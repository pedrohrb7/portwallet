# Plano: RF-05 Transferência

- **Status:** Aprovada
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rf-17-busca-destinatario](../rf-17-busca-destinatario/plan.md), [rf-04-deposito](../rf-04-deposito/plan.md)

## Passos

### 1. Regras de negócio

- [ ] `TransferRequest`, exceções de regra e `TransferService` com as verificações na ordem da spec
- **Arquivos:** `transfer/api/TransferRequest.java`, `transfer/application/TransferService.java`, `transfer/domain/**`
- **Validação:** `TransferServiceTest` (uma falha por regra, sem alterar saldos)

### 2. Endpoint

- [ ] `TransferController.create` com `Location` e `TransferResponse`
- **Arquivos:** `transfer/api/TransferController.java`, `transfer/api/TransferResponse.java`
- **Validação:** `TransferApiTest` (201, cabeçalho `Location`, Problem Details de cada erro)

### 3. Concorrência pela API

- [ ] `TransferConcurrencyTest`: 100 transferências cruzadas e 20 simultâneas de uma conta com saldo para 5
- **Arquivos:** `backend/src/test/java/com/portwallet/transfer/TransferConcurrencyTest.java`
- **Validação:** `LedgerInvariants.assertConsistent` ao final; estável em 20 execuções seguidas

### 4. Frontend

- [ ] `TransferPage` com os passos de valor, confirmação e resultado, integrando o `RecipientStep`
- **Arquivos:** `frontend/src/features/transfer/**`
- **Validação:** `TransferPage.test.tsx`

### 5. E2E

- [ ] `e2e/transfer.spec.ts`: cenário 3 (até o resultado, com dois contextos de navegador) e cenário 4
- **Validação:** `npm run e2e` verde em Chromium e Firefox

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] E2E verde em Chromium e Firefox
- [ ] Todos os passos da tela revisados em desktop e mobile
- [ ] Spec atualizada com o que mudou na implementação

## Riscos

- Com 3 novas tentativas, a disputa intensa numa mesma conta pode gerar 409 no teste de concorrência. O teste aceita `concurrency-conflict` como resultado válido e verifica só as invariantes e a quantidade de transferências concluídas.
