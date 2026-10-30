# Spec: RF-05 Transferência

- **Status:** Rascunho
- **Fase:** 2
- **PRD:** RF-05; US-04; Concorrência e consistência
- **Depende de:** [rf-17-busca-destinatario](../rf-17-busca-destinatario/spec.md), [rf-04-deposito](../rf-04-deposito/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Permitir que o usuário transfira saldo para outro usuário ativo, conferido pela busca de destinatário, respeitando saldo, limite diário e status das contas, com consistência sob concorrência.

## Escopo

**Inclui**

- `POST /transfers`.
- Passos de valor, confirmação e resultado da tela `/transfer`.
- Teste de 100 transferências concorrentes pela API.

**Fica para depois**

- `Idempotency-Key` obrigatório, chave gerada ao abrir a confirmação, duplo clique e retry: fase 3.
- Antifraude com circuit breaker e fallback: fase 4.
- Notificação de remetente e destinatário (RF-10): fase 4.
- Rate limit de 10 transferências por minuto: fase 5.
- Código TOTP acima de R$ 1.000 (`X-TOTP-Code`): fase 6.

## Comportamento

As regras são verificadas nesta ordem, e a primeira que falhar define a resposta:

1. Corpo válido: `recipientId` presente; `amount` maior que zero com até duas casas; `description` opcional com até 140 caracteres, sem espaços nas pontas. Senão, 400 `validation-error`.
2. Remetente `BLOCKED`: 403 `account-blocked`.
3. Destino igual à conta do remetente: 422 `self-transfer`.
4. Destino inexistente, conta de sistema, ou usuário de destino que não está `ACTIVE`: 422 `recipient-unavailable`.
5. Valor maior que o saldo: 422 `insufficient-balance`.
6. `usedToday + amount` maior que o limite diário (ver [RF-08](../rf-08-saldo/spec.md)): 422 `daily-limit-exceeded`, com `availableToday` no corpo do erro.
7. Passando tudo, o `LedgerService` debita a origem e credita o destino numa transação `TRANSFER`, `COMPLETED`, dentro do `OptimisticRetry`. As regras 2 a 6 são reavaliadas a cada tentativa, já que saldo e limite podem ter mudado.
8. Resposta 201 com `Location: /api/v1/transfers/{id}`, o detalhe da transferência e o novo saldo do remetente.

Duas transferências simultâneas da mesma conta não conseguem, juntas, passar do saldo nem do limite diário: ambas alteram a `version` da conta de origem, então a segunda repete as verificações com os valores já atualizados.

Na tela:

9. O fluxo tem quatro passos: destinatário ([RF-17](../rf-17-busca-destinatario/spec.md)), valor e descrição, confirmação e resultado. O passo de valor mostra o saldo e o limite disponível hoje.
10. A confirmação mostra o nome mascarado, o valor em BRL e a descrição. O botão "Confirmar" fica desabilitado durante o envio.
11. Erros 422 voltam para o passo correspondente com a mensagem do `type` (destinatário, valor). O saldo exibido só muda depois da resposta do servidor.
12. O resultado mostra o comprovante com link para o detalhe (RF-06) e atalhos para nova transferência e para o extrato.

## API

### `POST /api/v1/transfers`

- **Acesso:** USER

```json
{ "recipientId": "6a1d...", "amount": "150.00", "description": "Almoço" }
```

Resposta `201`:

```json
{
  "id": "e7b2...",
  "status": "COMPLETED",
  "amount": "150.00",
  "description": "Almoço",
  "recipient": { "maskedName": "Ana S****" },
  "balance": "1100.00",
  "createdAt": "2026-10-30T15:00:00Z"
}
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Regra 1 |
| 403 | `account-blocked` | Regra 2 |
| 409 | `concurrency-conflict` | Novas tentativas esgotadas |
| 422 | `self-transfer` | Regra 3 |
| 422 | `recipient-unavailable` | Regra 4 |
| 422 | `insufficient-balance` | Regra 5 |
| 422 | `daily-limit-exceeded` | Regra 6 |

O PRD mostra um exemplo com `targetEmail`. Esta spec usa o `recipientId` devolvido pela busca, como decidido em [RF-17](../rf-17-busca-destinatario/spec.md).

## Modelo de dados

Nenhuma migration.

## Backend

- `transfer.api.TransferController.create` e `TransferRequest`.
- `transfer.application.TransferService`: regras 2 a 7; consulta `usedToday` pelo mesmo serviço de RF-08.
- Exceções `SelfTransferException`, `RecipientUnavailableException`, `DailyLimitExceededException`.

## Frontend

- `src/features/transfer/TransferPage.tsx`: máquina de passos com estado local.
- `AmountStep.tsx` (`data-testid="transfer-amount"`, `transfer-description`), `ConfirmStep.tsx` (`data-testid="transfer-confirm"`), `ResultStep.tsx` (`data-testid="transfer-success"`).
- Mutação com TanStack Query que, no sucesso, invalida `['account', 'me']` e `['statement']`.

## Segurança

- A origem sempre vem do token.
- Mensagens de erro não revelam o motivo de um destinatário estar indisponível.

## Testes

| Camada | Cenário |
| --- | --- |
| Integração | Sucesso com saldo e lançamentos corretos; uma falha para cada regra de 1 a 6, sem alterar nenhum saldo; limite diário somando transferências anteriores do dia; destino bloqueado entre a busca e a transferência dá `recipient-unavailable` |
| Concorrência | 100 transferências cruzadas pela API (MockMvc) entre 10 usuários, em paralelo: invariantes válidas e nenhum saldo negativo; 20 transferências simultâneas de uma conta com saldo para só 5: no máximo 5 concluem, as outras recebem `insufficient-balance` ou `concurrency-conflict`, e o saldo final é o inicial menos as concluídas |
| Unitário (frontend) | Navegação entre passos; botão desabilitado durante o envio; erro 422 volta ao passo certo |
| E2E | Cenário 3 do PRD (parte da transferência, com dois contextos de navegador); cenário 4: saldo insuficiente mostra o erro e não altera o saldo |

## Critérios de aceite

- [ ] Todas as regras de negócio com teste
- [ ] Teste de 100 transferências concorrentes verde
- [ ] Cenários E2E 3 e 4 verdes (o 3 completo depende do extrato)

## Decisões

- **Ordem fixa das verificações:** a resposta para uma mesma situação é sempre a mesma, o que deixa os testes e as mensagens previsíveis.
- **`availableToday` no erro de limite:** o frontend mostra quanto ainda dá para transferir sem uma segunda chamada.

## Questões em aberto

- Nenhuma.
