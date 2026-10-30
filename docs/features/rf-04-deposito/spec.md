# Spec: RF-04 Depósito simulado

- **Status:** Rascunho
- **Fase:** 2
- **PRD:** RF-04; US-03
- **Depende de:** [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/spec.md), [rf-08-saldo](../rf-08-saldo/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Permitir que o usuário adicione saldo simulado à própria conta, para ter dinheiro para transferir.

## Escopo

**Inclui**

- `POST /deposits`.
- Tela `/deposit`.

**Fica para depois**

- `Idempotency-Key` obrigatório e replay: fase 3. Até lá, um reenvio cria um novo depósito.
- Limite de requisições da API autenticada: fase 5.

## Comportamento

1. O valor precisa ser maior que zero, ter no máximo duas casas decimais e ser no máximo R$ 10.000,00 por operação. Fora disso, 400 `validation-error`.
2. O depósito debita a conta `EXTERNAL_FUNDS` e credita a conta do usuário, pelo `LedgerService` dentro do `OptimisticRetry`.
3. A transação tem `type = DEPOSIT`, `status = COMPLETED` e descrição fixa "Depósito simulado".
4. Usuário `BLOCKED` recebe 403 `account-blocked`: conta bloqueada não recebe dinheiro.
5. A resposta é 201 com o ID da transação e o novo saldo. Depósitos não têm endpoint de detalhe; aparecem no extrato.
6. Não há limite diário para depósitos.
7. Na tela, o botão fica desabilitado durante o envio. Com sucesso, a tela mostra o novo saldo devolvido pela API e invalida a query da conta; com erro, mostra a mensagem do `type`.

## API

### `POST /api/v1/deposits`

- **Acesso:** USER

```json
{ "amount": "250.00" }
```

Resposta `201`:

```json
{
  "transactionId": "c3f1...",
  "amount": "250.00",
  "balance": "1500.00",
  "createdAt": "2026-10-30T15:00:00Z"
}
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Regra 1 |
| 403 | `account-blocked` | Regra 4 |
| 409 | `concurrency-conflict` | Novas tentativas esgotadas |

## Modelo de dados

Nenhuma migration.

## Backend

- `transfer.api.DepositController` e `DepositRequest` (`@NotNull`, `@DecimalMin(value = "0.01")`, `@DecimalMax("10000.00")`, `@Digits(integer = 17, fraction = 2)`).
- `transfer.application.DepositService`: busca a conta do usuário, aplica a regra 4 e chama o ledger.

## Frontend

- `src/features/deposit/DepositPage.tsx`: campo de valor com máscara BRL (`data-testid="deposit-amount"`), botão "Depositar" (`data-testid="deposit-submit"`), mensagem de sucesso com o novo saldo (`data-testid="deposit-success"`).
- `src/components/MoneyInput.tsx`: entrada de valor em BRL que produz string com duas casas; reutilizada na transferência.
- Esquema Zod com as mesmas regras do backend.

## Segurança

- A conta creditada sempre vem do token.

## Testes

| Camada | Cenário |
| --- | --- |
| Integração | Depósito válido atualiza saldo e gera dois lançamentos; zero, negativo, três casas decimais e 10.000,01 dão 400; usuário bloqueado dá 403; invariantes do ledger válidas ao final |
| Unitário (frontend) | `MoneyInput` converte a digitação em string com duas casas; esquema Zod rejeita acima de 10.000 |
| E2E | Cenário 2 do PRD: depósito atualiza o saldo no dashboard |

## Critérios de aceite

- [ ] Depósito respeita os limites de valor
- [ ] Saldo e ledger consistentes depois do depósito
- [ ] Cenário E2E 2 verde

## Decisões

- **Depósito no pacote `transfer`:** os dois são casos de uso de movimentação que passam pelo ledger, e na fase 3 compartilham a idempotência.

## Questões em aberto

- Nenhuma.
