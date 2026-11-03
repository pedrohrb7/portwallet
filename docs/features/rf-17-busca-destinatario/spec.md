# Spec: RF-17 Busca de destinatário

- **Status:** Aprovada
- **Fase:** 2
- **PRD:** RF-17; Segurança avançada (trade-off da busca)
- **Depende de:** [rf-01-cadastro](../rf-01-cadastro/spec.md), [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Deixar o usuário conferir para quem vai transferir antes de confirmar: ele informa e-mail ou CPF e vê o nome mascarado do destinatário, ou escolhe um dos destinatários recentes.

## Escopo

**Inclui**

- `GET /recipients/lookup` e `GET /recipients/recent`.
- Primeiro passo da tela `/transfer`.

**Fica para depois**

- Rate limit próprio (20 por minuto e 200 por dia por usuário): fase 5.
- Registro de cada consulta na auditoria: fase 5.

Os dois itens acima fazem parte da contenção do trade-off de enumeração descrito no PRD. Até a fase 5, a busca exige autenticação, mas não tem limite.

## Comportamento

1. A consulta aceita e-mail ou CPF no mesmo campo: com `@`, é tratada como e-mail (normalizado para minúsculas); senão, como CPF (com ou sem máscara). Formato inválido em qualquer dos dois: 400 `validation-error`.
2. A busca por CPF usa o blind index (`document_hash`); o CPF nunca é decifrado para comparar.
3. Encontrado um usuário `ACTIVE` diferente do solicitante, a resposta traz o `recipientId` (ID da conta de destino) e o nome mascarado.
4. Usuário inexistente, `PENDING` ou `BLOCKED`: 404 `recipient-not-found`, com a mesma mensagem, "Destinatário não encontrado ou indisponível".
5. Consulta do próprio e-mail ou CPF: 422 `self-transfer`.
6. Máscara do nome: primeiro nome completo, seguido da inicial do último sobrenome e quatro asteriscos ("Ana Souza Lima" vira "Ana L\*\*\*\*"). Nome de uma palavra só aparece inteiro.
7. `GET /recipients/recent` devolve até 5 destinatários distintos das transferências `COMPLETED` enviadas pelo usuário, do mais recente para o mais antigo, só os que ainda estão `ACTIVE`.

## API

### `GET /api/v1/recipients/lookup?query=ana@demo.com`

- **Acesso:** USER

Resposta `200`:

```json
{ "recipientId": "6a1d...", "maskedName": "Ana S****" }
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Regra 1 |
| 404 | `recipient-not-found` | Regra 4 |
| 422 | `self-transfer` | Regra 5 |

### `GET /api/v1/recipients/recent`

- **Acesso:** USER

Resposta `200`:

```json
[{ "recipientId": "6a1d...", "maskedName": "Ana S****", "lastTransferAt": "2026-10-30T15:00:00Z" }]
```

## Modelo de dados

Nenhuma migration. A busca usa os índices únicos de `users.email` e `users.document_hash`; os recentes, `ix_transactions_source_created`.

## Backend

- `account.application.RecipientService`: `lookup(UUID requesterId, String query)` e `recent(UUID requesterId)`.
- `shared.privacy.NameMasker`, já criado pelo refactor de [senha na ativação](../../backlog/refactor/2026-10-30-senha-na-ativacao.md) ([ADR 0005](../../adr/0005-senha-definida-na-ativacao.md)).
- `account.api.RecipientController`.

## Frontend

- `src/features/transfer/RecipientStep.tsx`: campo "E-mail ou CPF" (`data-testid="recipient-query"`), botão "Buscar", cartão do resultado com o nome mascarado (`data-testid="recipient-result"`) e botão "Continuar"; lista de recentes (`data-testid="recent-recipients"`) clicável.
- Erros por `type`: não encontrado, formato inválido, a própria conta.

## Segurança

- O `recipientId` é o ID da conta, não do usuário, e só serve como destino de transferência.
- A resposta nunca traz e-mail, CPF nem nome completo.
- Logs registram a consulta com e-mail mascarado e CPF omitido.

## Testes

| Camada | Cenário |
| --- | --- |
| Unitário | `NameMasker` com uma, duas e três palavras e espaços extras |
| Integração | Busca por e-mail e por CPF com e sem máscara; inexistente, `PENDING` e `BLOCKED` devolvem corpo idêntico; própria conta dá 422; recentes distintos, ordenados, no máximo 5 e sem destinatários bloqueados |
| Unitário (frontend) | `RecipientStep` mostra o nome mascarado e as mensagens de erro |
| E2E | Parte do cenário 3 do PRD: a busca mostra o nome mascarado |

## Critérios de aceite

- [ ] Busca por e-mail e CPF com resposta mascarada
- [ ] Mesma resposta para inexistente e inativo
- [ ] Recentes funcionando

## Decisões

- **`recipientId` como ID da conta:** a transferência referencia exatamente o destino conferido pelo usuário, sem uma nova busca por e-mail que poderia mudar de resultado.

## Questões em aberto

- Nenhuma.
