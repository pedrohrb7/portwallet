# Spec: RF-03 Logout

- **Status:** Aprovada
- **Fase:** 2
- **PRD:** RF-03
- **Depende de:** [rf-02-login](../rf-02-login/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Encerrar a sessão do usuário, revogando o refresh token atual e limpando o estado do frontend.

## Escopo

**Inclui**

- `POST /auth/logout` e o botão "Sair" no cabeçalho.

**Fica para depois**

- Exigir `X-XSRF-TOKEN` no logout: fase 5.
- Revogar todas as sessões do usuário: RF-14 (troca de senha).

## Comportamento

1. `POST /auth/logout` com cookie de refresh válido revoga esse token e apaga o cookie (`Max-Age=0`). Resposta 204.
2. Sem cookie, ou com token desconhecido, expirado ou já revogado, a resposta também é 204 e o cookie é apagado: o logout é idempotente e não revela nada.
3. Só o token atual é revogado; outras sessões do mesmo usuário (outras famílias) continuam valendo.
4. O access token já emitido continua válido até expirar (no máximo 15 min). É o custo de um JWT sem estado, aceito pelo PRD.
5. No frontend, "Sair" chama o logout, descarta o access token da memória, limpa o cache do TanStack Query e navega para `/login`, mesmo se a chamada falhar.

## API

### `POST /api/v1/auth/logout`

- **Acesso:** Cookie `refresh_token` (opcional)

Resposta `204` sem corpo, com `Set-Cookie: refresh_token=; Max-Age=0; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Strict`.

## Modelo de dados

Nenhuma migration.

## Backend

- `RefreshTokenService.revoke(rawToken)` e `AuthController.logout`.
- `/auth/logout` liberado na `SecurityFilterChain`, já que a autenticação é o próprio cookie.

## Frontend

- Botão "Sair" no `AppLayout` (`data-testid="logout-button"`).
- `session.logout()`.

## Segurança

- Depois do logout, recarregar a página não restaura a sessão.
- O cache do TanStack Query é limpo para que dados do usuário anterior não apareçam para o próximo.

## Testes

| Camada | Cenário |
| --- | --- |
| Integração | Logout revoga o token (refresh seguinte dá 401); logout sem cookie dá 204; outra família do mesmo usuário continua válida |
| Unitário (frontend) | `logout()` limpa token e cache mesmo com erro de rede |
| E2E | Login, sair, recarregar a página: continua em `/login` |

## Critérios de aceite

- [ ] Refresh token revogado e cookie apagado
- [ ] Sessão não volta ao recarregar a página

## Decisões

- **Logout sempre 204:** não há o que informar ao cliente, e um erro só atrapalharia a saída.

## Questões em aberto

- Nenhuma.
