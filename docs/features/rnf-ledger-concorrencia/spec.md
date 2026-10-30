# Spec: Ledger e concorrência

- **Status:** Aprovada
- **Fase:** 2
- **PRD:** Concorrência e consistência; Modelo de dados (invariantes); RF-08
- **Depende de:** [rf-01-cadastro](../rf-01-cadastro/spec.md) (tabela `accounts`)
- **Plano:** [plan.md](plan.md)

## Objetivo

Garantir que todo movimento de dinheiro seja registrado em dupla entrada, que o saldo de cada conta sempre bata com o ledger e que nenhum saldo fique negativo, mesmo com muitas operações simultâneas na mesma conta e em várias instâncias.

## Escopo

**Inclui**

- Tabelas `transactions` e `ledger_entries` e a conta de sistema `EXTERNAL_FUNDS`.
- `LedgerService`, o único ponto que altera saldos.
- Lock otimista com novas tentativas e 409 após esgotá-las.
- Utilitário de teste que verifica as invariantes do ledger.

**Fica para depois**

- Estorno (lançamentos inversos): RF-09, fase 5 ou depois.
- Coluna `idempotency_key` preenchida: fase 3. Ela já nasce aqui, opcional.
- `fraud_check` preenchido: fase 4. Também já nasce opcional.

## Comportamento

1. Toda movimentação cria uma linha em `transactions` e exatamente dois lançamentos em `ledger_entries`: um `DEBIT` na conta de origem e um `CREDIT` na de destino, com o mesmo valor, na mesma transação de banco.
2. Depósitos debitam a conta de sistema `EXTERNAL_FUNDS`, então não há exceção à dupla entrada.
3. Cada lançamento guarda `balance_after`, o saldo da conta logo depois dele.
4. O `balance` da conta é atualizado na mesma transação dos lançamentos e é sempre igual a créditos menos débitos no ledger.
5. Uma conta `USER` nunca tem saldo negativo: o serviço valida antes de debitar (`insufficient-balance`) e o banco garante com `CHECK`.
6. Contas `USER` usam lock otimista pela coluna `version`. Em conflito, a operação inteira é repetida em uma nova transação, até 3 novas tentativas, com espera aleatória curta (10 a 50 ms) entre elas. Esgotadas, 409 `concurrency-conflict`.
7. A conta de sistema é atualizada com `UPDATE ... SET balance = balance - ?` atômico, sem checar `version`. Ela participa de todo depósito; com lock otimista, depósitos simultâneos de usuários diferentes entrariam em conflito entre si sem motivo. O `balance_after` dela é lido depois do `UPDATE`, com a linha já travada pela própria transação.
8. Lançamentos são imutáveis: a entidade é `@Immutable` e o repositório não expõe atualização nem remoção.
9. Valores de transação são sempre positivos, e origem e destino são contas diferentes (`CHECK` no banco).

## API

Sem endpoints próprios. O saldo é exposto por [RF-08](../rf-08-saldo/spec.md) e os lançamentos pelo [extrato](../rf-07-extrato/spec.md).

## Modelo de dados

`V6__ledger.sql`

```sql
CREATE TABLE transactions (
    id                 UUID          PRIMARY KEY,
    type               VARCHAR(10)   NOT NULL CHECK (type IN ('DEPOSIT', 'TRANSFER', 'REVERSAL')),
    status             VARCHAR(10)   NOT NULL CHECK (status IN ('COMPLETED', 'REVERSED')),
    amount             DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    source_account_id  UUID          NOT NULL REFERENCES accounts (id),
    target_account_id  UUID          NOT NULL REFERENCES accounts (id),
    fraud_check        VARCHAR(10)   CHECK (fraud_check IN ('APPROVED', 'SKIPPED', 'DENIED')),
    idempotency_key    UUID,
    description        VARCHAR(140),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_transactions_distinct_accounts CHECK (source_account_id <> target_account_id)
);
CREATE INDEX ix_transactions_source_created ON transactions (source_account_id, created_at);

CREATE TABLE ledger_entries (
    id              UUID          PRIMARY KEY,
    transaction_id  UUID          NOT NULL REFERENCES transactions (id),
    account_id      UUID          NOT NULL REFERENCES accounts (id),
    direction       VARCHAR(6)    NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount          DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    balance_after   DECIMAL(19,2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_ledger_entries_statement ON ledger_entries (account_id, created_at DESC, id DESC);
CREATE INDEX ix_ledger_entries_transaction ON ledger_entries (transaction_id);

INSERT INTO accounts (id, user_id, kind, balance, daily_limit, version, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 'SYSTEM', 0.00, 0.00, 0, CURRENT_TIMESTAMP);
```

O `EXPLAIN` dos índices, antes e depois, é registrado em ADR na fase 6, como pede o PRD.

## Backend

Pacote `com.portwallet.ledger`:

- `domain.Transaction`, `domain.LedgerEntry` (`@Immutable`), `domain.TransactionType`, `domain.Direction`.
- `application.LedgerService.post(TransactionType type, UUID sourceAccountId, UUID targetAccountId, Money amount, String description)`: deve ser chamado dentro de uma transação aberta pelo caso de uso; carrega as contas, aplica as regras 5 e 7, grava a transação e os lançamentos, e devolve um `PostedTransaction`.
- `application.OptimisticRetry.execute(Supplier<T>)`: roda o supplier num `TransactionTemplate` novo a cada tentativa e trata `ObjectOptimisticLockingFailureException` conforme a regra 6. Os casos de uso (depósito, transferência) passam por ele, e nunca chamam `LedgerService` fora dele.

Pacote `com.portwallet.account`:

- `domain.Account` com `@Version`, `debit(Money)` e `credit(Money)`, que lançam `InsufficientBalanceException` (422 `insufficient-balance`).
- `infra.SystemAccountRepository.applyDelta(UUID, BigDecimal)` com o `UPDATE` atômico da regra 7.

## Frontend

Nenhuma mudança.

## Segurança

- Nenhum endpoint altera `balance` diretamente; só `LedgerService`.
- A conta de sistema nunca aparece em buscas de destinatário nem como conta de usuário.

## Testes

| Camada | Cenário |
| --- | --- |
| Unitário | `Account.debit` rejeita valor maior que o saldo; `Account.credit` soma com escala 2 |
| Integração | Depósito e transferência geram dois lançamentos com o mesmo valor e `balance_after` correto; `CHECK` do banco recusa saldo negativo mesmo contornando o serviço; tentativa de alterar um lançamento falha |
| Concorrência | 100 transferências cruzadas entre 10 contas em 16 threads, iniciadas juntas por `CountDownLatch`: invariantes válidas, nenhum saldo negativo, soma de todos os saldos (incluindo o sistema) igual a zero; 50 depósitos simultâneos em contas diferentes terminam sem nenhum 409 |
| Retry | Com um conflito forçado na primeira tentativa, a operação conclui na segunda; com conflito em todas, 409 `concurrency-conflict` |

`LedgerInvariants.assertConsistent(JdbcTemplate)` fica em `src/test` e é chamado ao final de todo teste de concorrência, nesta e nas próximas features.

## Critérios de aceite

- [ ] Teste de 100 transferências concorrentes verde, com as invariantes verificadas
- [ ] Saldo nunca negativo, garantido no serviço e no banco
- [ ] 409 `concurrency-conflict` depois de 3 novas tentativas

## Decisões

- **Conta de sistema fora do lock otimista:** o `UPDATE` atômico serializa os depósitos só pelo tempo da escrita na linha, em vez de forçar novas tentativas em todos eles.
- **Retry com transação nova:** repetir dentro da mesma transação reutilizaria entidades obsoletas do contexto de persistência.
- **Espera aleatória entre tentativas:** evita que as mesmas requisições voltem a colidir em sincronia.

## Questões em aberto

- Nenhuma.
