# Plano: Ledger e concorrência

- **Status:** Aprovada
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rf-01-cadastro](../rf-01-cadastro/plan.md)

## Passos

### 1. Migration e entidades

- [ ] `V6__ledger.sql` com a conta de sistema
- [ ] `Transaction`, `LedgerEntry`, enums e repositórios
- **Arquivos:** `db/migration/V6__ledger.sql`, `ledger/domain/**`, `ledger/infra/**`
- **Validação:** `./mvnw verify`

### 2. Regras da conta

- [ ] `Account.debit` e `Account.credit`, `InsufficientBalanceException`
- [ ] `SystemAccountRepository.applyDelta`
- **Arquivos:** `account/domain/Account.java`, `account/infra/SystemAccountRepository.java`
- **Validação:** `AccountTest`

### 3. LedgerService

- [ ] `LedgerService.post` com dupla entrada e `balance_after`
- **Arquivos:** `ledger/application/LedgerService.java`
- **Validação:** `LedgerServiceTest` (lançamentos, saldo, `CHECK` negativo, lançamento imutável)

### 4. Retry otimista

- [ ] `OptimisticRetry` com até 3 novas tentativas, espera aleatória e `ConcurrencyConflictException`
- **Arquivos:** `ledger/application/OptimisticRetry.java`, `shared/error/ConcurrencyConflictException.java`
- **Validação:** `OptimisticRetryTest` (sucesso na segunda tentativa; 409 após esgotar)

### 5. Invariantes e concorrência

- [ ] `LedgerInvariants` em `src/test`
- [ ] `LedgerConcurrencyTest`: 100 transferências cruzadas e 50 depósitos simultâneos, chamando o serviço diretamente
- **Arquivos:** `backend/src/test/java/com/portwallet/ledger/**`
- **Validação:** o teste passa 20 vezes seguidas, rodado localmente com `@RepeatedTest(20)`, sem falha intermitente

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] Teste de concorrência estável em execuções repetidas
- [ ] Spec atualizada com o que mudou na implementação

## Riscos

- Teste de concorrência instável: usar `CountDownLatch` para largada simultânea e verificar o estado final, nunca a ordem.
- H2 com comportamento de lock diferente do PostgreSQL: a regra de saldo é garantida pelo `CHECK`, que funciona igual nos dois.
