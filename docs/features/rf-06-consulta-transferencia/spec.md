# Spec: RF-06 Consulta de transferência

- **Status:** Aprovada
- **Fase:** 2
- **PRD:** RF-06; Segurança (checagem de propriedade)
- **Depende de:** [rf-05-transferencia](../rf-05-transferencia/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Permitir que o remetente ou o destinatário consulte os detalhes de uma transferência pelo ID, e que ninguém mais consiga.

## Escopo

**Inclui**

- `GET /transfers/{id}`.
- Tela de comprovante em `/transfers/:id`, usada pelo resultado da transferência e pelo extrato. A rota não está na tabela de telas do PRD e é acrescentada aqui.

**Fica para depois**

- Mostrar `fraudCheck` e o estado de estorno: fases 4 e RF-09.

## Comportamento

1. O ID precisa ser um UUID; senão, 400 `validation-error`.
2. Transação inexistente, ou do tipo `DEPOSIT`: 404 `not-found`. Depósitos são consultados só pelo extrato.
3. Transferência que existe, mas cuja conta de origem e de destino não pertencem ao usuário: 403 `forbidden`, como define o PRD.
4. Para o remetente, `direction = SENT` e `counterparty` é o destinatário; para o destinatário, `direction = RECEIVED` e `counterparty` é o remetente. O nome da contraparte sai mascarado.
5. A resposta não traz saldos, IDs de conta nem dados do usuário além do nome mascarado.

## API

### `GET /api/v1/transfers/{id}`

- **Acesso:** USER (remetente ou destinatário)

Resposta `200`:

```json
{
  "id": "e7b2...",
  "type": "TRANSFER",
  "status": "COMPLETED",
  "direction": "SENT",
  "amount": "150.00",
  "description": "Almoço",
  "counterparty": { "maskedName": "Ana S****" },
  "createdAt": "2026-10-30T15:00:00Z"
}
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Regra 1 |
| 403 | `forbidden` | Regra 3 |
| 404 | `not-found` | Regra 2 |

## Modelo de dados

Nenhuma migration.

## Backend

- `transfer.application.TransferQueryService.get(UUID userId, UUID transferId)`: uma consulta com as contas e os nomes necessários, sem carregar entidades.
- `TransferController.get`.

## Frontend

- `src/features/transfer/TransferDetailPage.tsx` em `/transfers/:id`: comprovante com direção, valor, contraparte, descrição e data no fuso de São Paulo (`data-testid="transfer-detail"`); estados de não encontrado e sem permissão.

## Segurança

- A checagem de propriedade fica no serviço, não no controller, e vale para qualquer chamada futura (ex.: painel admin usa outro caminho).

## Testes

| Camada | Cenário |
| --- | --- |
| Integração | Remetente vê `SENT`, destinatário vê `RECEIVED`; terceiro recebe 403; ID inexistente e ID de depósito recebem 404; ID malformado recebe 400; resposta sem saldos nem IDs de conta |
| Unitário (frontend) | Comprovante para `SENT` e `RECEIVED`; estado de erro |
| E2E | O link do resultado da transferência abre o comprovante |

## Critérios de aceite

- [ ] Só remetente e destinatário acessam a transferência
- [ ] Teste de IDOR cobrindo um terceiro usuário

## Decisões

- **403 para recurso de outro usuário:** segue a tabela de erros do PRD. Os IDs são UUID aleatórios, então a diferença entre 403 e 404 não ajuda a descobrir transferências.

## Questões em aberto

- Nenhuma.
