# Spec: RF-08 Saldo

- **Status:** Aprovada
- **Fase:** 2
- **PRD:** RF-08; Frontend (Dashboard)
- **Depende de:** [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Mostrar ao usuário o saldo atual e quanto do limite diário de transferências ainda está disponível, na tela inicial.

## Escopo

**Inclui**

- `GET /accounts/me`.
- Dashboard em `/` com saldo e limite diário.

**Fica para depois**

- Últimas transações no dashboard: [RF-07](../rf-07-extrato/spec.md), que cria o endpoint usado por elas.
- ETag e 304 com `If-None-Match`: fase 6.

## Comportamento

1. `GET /accounts/me` devolve a conta do usuário autenticado; não existe parâmetro de ID, então não há como consultar outra conta.
2. `usedToday` é a soma das transferências `COMPLETED` enviadas pela conta desde 00:00 de hoje em `America/Sao_Paulo`. Depósitos não contam.
3. `availableToday = max(0, dailyLimit - usedToday)`.
4. `blocked` é `true` quando o usuário está `BLOCKED`.
5. O dashboard mostra o saldo formatado em BRL, o limite usado numa barra de progresso acessível (`role="progressbar"` com `aria-valuenow`) e um aviso quando a conta está bloqueada.
6. O dashboard consulta a conta a cada 15 s enquanto a aba está visível e logo depois de qualquer depósito ou transferência concluídos. O novo saldo só aparece depois da resposta do servidor, nunca de forma otimista.

## API

### `GET /api/v1/accounts/me`

- **Acesso:** USER

Resposta `200`:

```json
{
  "accountId": "6a1d...",
  "balance": "1250.00",
  "dailyLimit": "5000.00",
  "usedToday": "300.00",
  "availableToday": "4700.00",
  "blocked": false
}
```

| Status | `type` | Quando |
| --- | --- | --- |
| 401 | `unauthorized` | Sem token válido |

## Modelo de dados

Nenhuma migration. `usedToday` usa o índice `ix_transactions_source_created`.

## Backend

- `account.application.AccountQueryService.me(UUID userId)`: uma projeção da conta e uma consulta agregada de `usedToday`.
- `account.api.AccountController`.

## Frontend

- `src/features/account/useAccount.ts`: query `['account', 'me']`, com `refetchInterval` de 15 s e pausa com a aba oculta.
- `src/features/account/DashboardPage.tsx`: cartão de saldo (`data-testid="balance"`), barra de limite (`data-testid="daily-limit"`), aviso de conta bloqueada (`data-testid="account-blocked"`), atalhos para depositar, transferir e ver o extrato.
- Estados de carregamento com esqueleto e de erro com nova tentativa.

## Segurança

- A conta sempre vem do `sub` do token, nunca de um parâmetro.

## Testes

| Camada | Cenário |
| --- | --- |
| Integração | Conta nova devolve saldo 0,00 e limite 5.000,00; `usedToday` ignora depósitos e transferências de ontem (com `Clock` controlado perto da meia-noite de São Paulo); `blocked` reflete o status |
| Unitário (frontend) | Saldo formatado em BRL; barra de limite com os atributos ARIA |
| E2E | Cenário 1 do PRD (final): após o login, o dashboard mostra R$ 0,00 |

## Critérios de aceite

- [ ] Saldo e limite diário corretos no dashboard
- [ ] "Hoje" calculado no fuso de São Paulo

## Decisões

- **Sem atualização otimista:** decisão do PRD para operações financeiras; vira ADR na fase 6 junto com as demais de performance.

## Questões em aberto

- Nenhuma.
