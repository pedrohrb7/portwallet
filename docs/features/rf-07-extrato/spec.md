# Spec: RF-07 Extrato

- **Status:** Aprovada
- **Fase:** 2
- **PRD:** RF-07; US-05; Performance (paginação por cursor)
- **Depende de:** [rf-05-transferencia](../rf-05-transferencia/spec.md), [rf-04-deposito](../rf-04-deposito/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Mostrar as movimentações da conta do usuário, da mais recente para a mais antiga, com filtros por período e tipo e paginação por cursor, que mantém o custo por página constante e nunca pula nem repete itens quando entram transações novas.

## Escopo

**Inclui**

- `GET /statement`.
- Tela `/statement` com rolagem infinita e filtros na URL.
- Últimas transações no dashboard.

**Fica para depois**

- ETag e 304 na primeira página: fase 6.
- Teto de queries com datasource-proxy e `EXPLAIN` registrado em ADR: fase 6.

## Comportamento

1. O extrato lista os lançamentos (`ledger_entries`) da conta do usuário, ordenados por `created_at DESC, id DESC`.
2. `size` vai de 1 a 100, com padrão 20. Fora disso, 400 `validation-error`.
3. `from` e `to` são datas (`YYYY-MM-DD`), inclusivas, interpretadas no fuso `America/Sao_Paulo`: `from` vira 00:00 desse dia e `to` vira 00:00 do dia seguinte, exclusivo. `from` depois de `to`: 400 `validation-error`.
4. `type` filtra pelo tipo da transação: `DEPOSIT`, `TRANSFER` ou `REVERSAL`.
5. O cursor é opaco: Base64URL de `<created_at em microssegundos desde a época>:<id do lançamento>`. A próxima página usa `WHERE (created_at, id) < (:createdAt, :id)`, nunca `OFFSET`.
6. Cursor que não decodifica, ou com formato inválido: 400 `invalid-cursor`.
7. O servidor busca `size + 1` linhas; se vier a linha extra, devolve `nextCursor` a partir do último item exibido; senão, `nextCursor` é `null`.
8. Os filtros não fazem parte do cursor. O cliente reenvia os mesmos filtros a cada página; trocar um filtro recomeça do início.
9. Cada item traz a contraparte com o nome mascarado. Depósitos não têm contraparte; o item mostra a descrição "Depósito simulado".
10. Tudo numa única consulta por página, com projeção e joins, sem N+1.
11. Na tela, os filtros ficam na URL (`/statement?type=TRANSFER&from=2026-10-01`), para que recarregar ou compartilhar o link mantenha a visão. A próxima página carrega ao chegar ao fim da lista e também por um botão "Carregar mais", acessível por teclado.
12. O dashboard mostra os 5 lançamentos mais recentes (`size=5`) com link para o extrato completo.

## API

### `GET /api/v1/statement?size=20&type=TRANSFER&from=2026-10-01&to=2026-10-30&cursor=...`

- **Acesso:** USER

Resposta `200`:

```json
{
  "items": [
    {
      "entryId": "0f3a...",
      "transactionId": "e7b2...",
      "type": "TRANSFER",
      "direction": "DEBIT",
      "amount": "150.00",
      "balanceAfter": "1100.00",
      "description": "Almoço",
      "counterparty": { "maskedName": "Ana S****" },
      "createdAt": "2026-10-30T15:00:00Z"
    }
  ],
  "nextCursor": "MTc2MTgzNjQwMDAwMDAwMDo..."
}
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Regras 2, 3 e 4 |
| 400 | `invalid-cursor` | Regra 6 |

## Modelo de dados

Nenhuma migration. A consulta usa `ix_ledger_entries_statement (account_id, created_at DESC, id DESC)`, criado em [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/spec.md).

## Backend

- `ledger.domain.StatementCursor`: codifica e decodifica o cursor.
- `ledger.application.StatementService.page(UUID userId, StatementQuery query)`.
- `ledger.infra.StatementRepository`: consulta com `JdbcClient` (SQL explícito, para controlar a comparação de tupla e o uso do índice) e projeção direta para `StatementItem`.
- `ledger.api.StatementController` e `StatementQuery` com Bean Validation.

## Frontend

- `src/features/statement/useStatement.ts`: `useInfiniteQuery` com chave `['statement', filtros]`.
- `src/features/statement/StatementPage.tsx`: filtros (tipo, período) ligados aos search params; lista (`data-testid="statement-list"`, itens `statement-item`) com data, descrição, contraparte, valor colorido por direção e saldo após; `IntersectionObserver` e botão "Carregar mais" (`data-testid="statement-load-more"`); estado vazio.
- `src/features/account/RecentActivity.tsx` no dashboard, usando `size=5`.
- Itens de transferência abrem o comprovante (`/transfers/:id`).

## Segurança

- A conta vem do token; não há parâmetro de conta.
- O cursor não é assinado: adulterá-lo só muda o ponto de partida dentro da própria conta, que continua filtrada pelo token.

## Testes

| Camada | Cenário |
| --- | --- |
| Unitário | `StatementCursor` ida e volta; entrada inválida |
| Integração | Ordem decrescente; paginação percorre tudo sem repetir nem pular; inserir transações novas entre duas páginas não altera a segunda; filtros de tipo e período (com borda de meia-noite em São Paulo); `size` fora do intervalo; cursor inválido; item de depósito sem contraparte; usuário só vê os próprios lançamentos |
| Unitário (frontend) | Filtros sincronizados com a URL; botão "Carregar mais" busca a próxima página |
| E2E | Cenário 3 do PRD completo: a transferência aparece no extrato dos dois usuários, com direções opostas |

## Critérios de aceite

- [ ] Paginação por cursor sem itens repetidos ou pulados
- [ ] Filtros refletidos na URL
- [ ] Cenário E2E 3 completo verde

## Decisões

- **SQL explícito com `JdbcClient`:** a comparação de tupla e o uso do índice ficam visíveis e testáveis, o que o JPQL esconderia.
- **Filtros fora do cursor:** o cursor continua pequeno e o cliente já precisa conhecer os filtros para montar a tela.

## Questões em aberto

- Confirmar na implementação que o H2 usa o índice com a comparação de tupla `(created_at, id) < (?, ?)`. Se não usar, trocar pela forma equivalente `created_at < ? OR (created_at = ? AND id < ?)` e registrar a diferença.
