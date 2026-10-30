# Spec: Base da aplicação

- **Status:** Concluída
- **Fase:** 2
- **PRD:** API (Problem Details), Segurança, Frontend (diretrizes)
- **Depende de:** [rnf-fundacao](../rnf-fundacao/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Montar a infraestrutura comum que todas as features da Fase 2 usam: tratamento de erros, dinheiro, relógio, cadeia de segurança com JWT, regras de arquitetura e o esqueleto do frontend (rotas, cliente HTTP, sessão e layout). Nenhuma regra de negócio entra aqui.

## Escopo

**Inclui**

- Problem Details para todos os erros, conforme [convenções](../../convencoes.md#api).
- Tipo `Money` e serialização de valores como string.
- `Clock` injetável com fuso de negócio `America/Sao_Paulo`.
- Spring Security stateless com emissão e validação de JWT HS256.
- ArchUnit com as regras de camadas.
- Frontend: React Router, TanStack Query, Axios, React Hook Form, Zod, layout autenticado, guarda de rota e mapeamento de erros.

**Fica para depois**

- CORS e headers de segurança (CSP etc.): fase 5. Em todas as fases o frontend chama a API pela mesma origem (proxy do Vite ou do Nginx), então CORS não é necessário para funcionar.
- CSRF double-submit no refresh e logout: fase 5.
- `X-Correlation-Id` e logs em JSON: fase 4 (observabilidade).

## Comportamento

1. Qualquer exceção não tratada vira 500 com `type: internal-error` e sem detalhes internos; a exceção completa vai para o log.
2. Falha de Bean Validation, JSON malformado ou parâmetro com tipo errado vira 400 `validation-error` com a lista `errors`.
3. Requisição sem token, ou com token inválido ou expirado, a um endpoint protegido vira 401 `unauthorized`, com o mesmo formato de Problem Details.
4. Usuário autenticado sem o perfil exigido recebe 403 `forbidden`.
5. Rota inexistente em `/api` vira 404 `not-found` em Problem Details.
6. Valores monetários são lidos como string ou número e escritos sempre como string com duas casas; mais de duas casas decimais é 400.
7. O access token é um JWT HS256 com `sub` (ID do usuário), `role` (`USER` ou `ADMIN`), `iat` e `exp` (15 min). O segredo vem de `JWT_SECRET` (mínimo 32 bytes); a aplicação não sobe sem ele fora dos perfis `dev`, `e2e` e `test`.
8. O frontend guarda o access token só em memória. Ao carregar, tenta restaurar a sessão chamando `/auth/refresh`; numa resposta 401 de outra chamada, tenta um refresh e repete a requisição uma única vez.
9. Rotas protegidas redirecionam para `/login` sem sessão, guardando a rota de origem para voltar depois do login.

## API

Sem endpoints novos. Formato de erro:

```json
{
  "type": "validation-error",
  "title": "Dados inválidos",
  "status": 400,
  "instance": "/api/v1/deposits",
  "errors": [{ "field": "amount", "message": "deve ser maior que zero" }]
}
```

## Modelo de dados

Nenhuma migration.

## Backend

Pacote `com.portwallet.shared`:

- `error.ApiException` (base com `type`, status e título, e o gancho `customize` para propriedades extras), subclasses por regra nas features.
- `error.Problems`: fábrica dos tipos genéricos do catálogo e o mapeamento de status do framework (404, 405, 406, 415) para `type`.
- `error.GlobalExceptionHandler` (`@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler`), com `ProblemDetail` do Spring. Erros de leitura do JSON apontam o campo pelo caminho do Jackson (ex.: `amount`), ou `body` para JSON malformado.
- `security.ProblemSecurityHandlers`: entry point e access denied handler do Spring Security, para 401 (com `WWW-Authenticate: Bearer`) e 403 no mesmo formato.
- `money.Money` (record com `BigDecimal` de escala 2, operações `plus`, `minus`, `isGreaterThan`, `max`) e `money.MoneyJson` (`@JsonComponent` com serializador e desserializador).
- `time.TimeConfig`: bean `Clock` (UTC) e constante `BUSINESS_ZONE = America/Sao_Paulo`.
- `security.SecurityConfig`: `SecurityFilterChain` stateless, `csrf` desligado nesta fase, OAuth2 Resource Server com `NimbusJwtDecoder` HS256, `@EnableMethodSecurity`. O decoder valida, além de expiração, que `sub` é um UUID e que `role` é um perfil conhecido; token fora disso é 401.
- `security.JwtService`: emite tokens com `NimbusJwtEncoder`.
- `security.Role` (`USER`, `ADMIN`) e `security.AuthenticatedUser(UUID id, Role role)`, recebido pelos controllers como parâmetro de método, por um `HandlerMethodArgumentResolver`.

Configuração `portwallet.security.jwt.secret` e `portwallet.security.jwt.access-token-ttl` (padrão `PT15M`) em `JwtProperties`, que recusa segredo com menos de 32 bytes e nunca o imprime em `toString`.

Perfil `test` em `src/test/resources/application-test.yml` com segredo fixo, ativado pelo Surefire. Perfis `dev` e `e2e` com segredo de desenvolvimento declarado no YAML e marcado como inseguro. O `docker-compose.yml` sobe no perfil `demo` com um segredo de demonstração, também marcado como inseguro e substituível por `JWT_SECRET`.

## Frontend

Dependências: `react-router`, `@tanstack/react-query`, `axios`, `react-hook-form`, `zod`, `@hookform/resolvers`.

- `src/api/client.ts`: instância `api` com `baseURL: '/api/v1'`, interceptor que injeta `Authorization`, e interceptor de 401 que repete a requisição uma vez depois de `refreshSession()`. Instância `authClient`, sem interceptores, para os endpoints de cookie. `refreshSession()` compartilha a mesma promessa entre chamadores simultâneos.
- `src/api/problem.ts`: tipo `Problem`, `toProblem(error)` (falha de rede vira `network-error`) e `messageFor(problem)`, que traduz o `type` em mensagem; tipo desconhecido cai numa mensagem genérica.
- `src/features/auth/sessionStore.ts`: store externo (lido com `useSyncExternalStore`) com o token em memória e o estado da sessão (`loading`, `authenticated`, `anonymous`). Fica fora do React para o interceptor do Axios conseguir lê-lo.
- `src/routes/`: `router.tsx`; `RootLayout` (banner e restauração da sessão na primeira carga); `PublicLayout`; `RequireAuth`; `AppLayout` (cabeçalho com navegação e nome do usuário); `navigation.ts` com os itens do menu; `NotFoundPage`; `HomePage` (a tela inicial da fase 1, pública até o dashboard do RF-08 ocupar `/`).
- `src/lib/money.ts`: `formatBRL(value: string)`.
- `src/test/axiosMock.ts` e `src/test/renderRoutes.tsx`: utilitários de teste.

## Segurança

- Endpoints em `/api/v1/**` exigem autenticação, exceto os públicos declarados por cada feature.
- `/actuator/health/**` é público; o resto do Actuator exige `ADMIN`.
- O segredo do JWT nunca é logado.

## Testes

| Camada | Cenário |
| --- | --- |
| Unitário | `Money` rejeita mais de duas casas e serializa como string (`MoneyTest`, `MoneyJsonTest`); `JwtProperties` recusa segredo curto e não o imprime |
| Integração | `ProblemDetailsTest`: 400 com `errors` (Bean Validation, JSON malformado, dinheiro com três casas, tipo de parâmetro, parâmetro ausente, restrição de parâmetro), `ApiException` com propriedade extra, 404, 405, 415 e 500 sem detalhes. `SecurityTest`: 401 sem token, expirado, assinatura errada, perfil desconhecido e `sub` que não é UUID; 403 sem perfil; `AuthenticatedUser` resolvido. `JwtSecretRequiredTest`: app não sobe sem `JWT_SECRET` |
| Arquitetura | `ArchitectureTest`: controllers não dependem de repositórios nem de `infra`; `domain` não depende de `api` nem de `infra`; `shared` não depende das features. Verificado com uma classe violadora temporária |
| Unitário (frontend) | `client.test.ts`: um único refresh para três 401 simultâneas, com as três requisições repetidas; sessão limpa quando o refresh falha. `RequireAuth.test.tsx`, `router.test.tsx`, `problem.test.ts`, `money.test.ts`, `health.test.ts` |

## Critérios de aceite

- [x] Todos os erros da API saem em Problem Details com `type` do catálogo
- [x] JWT emitido e validado com segredo vindo de configuração
- [x] Regras ArchUnit ativas no `verify`
- [x] Frontend com rotas, layout autenticado e cliente HTTP prontos para as features

## Decisões

- **HS256 em vez de RS256:** uma chave simétrica basta com um único emissor e verificador. O PRD já registra RS256 com KMS como evolução de produção.
- **Dinheiro como string no JSON:** evita a perda de precisão do `number` do JavaScript. Registrada no [ADR 0004](../../adr/0004-dinheiro-como-string-no-json.md).
- **Usuário autenticado como parâmetro de método:** os controllers recebem `AuthenticatedUser` em vez de ler o `SecurityContext`, o que deixa a dependência explícita e fácil de testar.
- **Store de sessão fora do React:** o interceptor do Axios precisa do token fora de componentes; `useSyncExternalStore` liga o mesmo store à interface.
- **`/` continua pública nesta feature:** a tela inicial da fase 1 fica em `/` até o dashboard do RF-08 substituí-la, para o E2E de health continuar valendo.
- **CSRF desligado até a fase 5:** a API só aceita autenticação por header `Authorization` nesta fase, exceto refresh e logout, que entram com a proteção completa na fase 5.

## Questões em aberto

- Nenhuma.
