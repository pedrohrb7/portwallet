# Spec: Base da aplicação

- **Status:** Rascunho
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

- `error.ApiException` (base com `type`, status e título), subclasses por regra nas features.
- `error.GlobalExceptionHandler` (`@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler`), com `ProblemDetail` do Spring.
- `error.ProblemAuthenticationEntryPoint` e `error.ProblemAccessDeniedHandler`, para 401 e 403 no mesmo formato.
- `money.Money` (record com `BigDecimal` de escala 2, operações `plus`, `minus`, `isGreaterThan`), com `JsonSerializer` e `JsonDeserializer` registrados num `Module` do Jackson.
- `time.TimeConfig`: bean `Clock` (UTC) e constante `BUSINESS_ZONE = America/Sao_Paulo`.
- `security.SecurityConfig`: `SecurityFilterChain` stateless, `csrf` desligado nesta fase, OAuth2 Resource Server com `NimbusJwtDecoder` HS256, conversor que lê o claim `role` como `ROLE_<role>`, `@EnableMethodSecurity`.
- `security.JwtService`: emite tokens com `NimbusJwtEncoder`.
- `security.CurrentUser`: acesso tipado ao usuário autenticado (`UUID id()`, `Role role()`).

Configuração `portwallet.security.jwt.secret` e `portwallet.security.jwt.access-token-ttl` (padrão `PT15M`), validadas com `@ConfigurationProperties` + `@Validated`.

Perfil `test` em `src/test/resources/application-test.yml` com segredo fixo. Perfis `dev` e `e2e` com segredo de desenvolvimento declarado no YAML e marcado como inseguro.

## Frontend

Dependências: `react-router`, `@tanstack/react-query`, `axios`, `react-hook-form`, `zod`, `@hookform/resolvers`.

- `src/api/client.ts`: instância Axios com `baseURL: '/api/v1'`, interceptor que injeta `Authorization`, e interceptor de 401 que faz um único refresh concorrente (requisições simultâneas aguardam o mesmo refresh).
- `src/api/problem.ts`: tipo `Problem` e `messageFor(problem)`, que traduz o `type` em mensagem; tipo desconhecido cai numa mensagem genérica.
- `src/features/auth/session.ts`: armazenamento do token em memória e estado da sessão (`loading`, `authenticated`, `anonymous`) exposto por contexto.
- `src/routes/`: `router.tsx` com rotas públicas e protegidas, `RequireAuth`, `AppLayout` (banner, cabeçalho com navegação e saída) e `PublicLayout`.
- `src/lib/money.ts`: `formatBRL(value: string)`.
- Página 404 do SPA.

## Segurança

- Endpoints em `/api/v1/**` exigem autenticação, exceto os públicos declarados por cada feature.
- `/actuator/health/**` é público; o resto do Actuator exige `ADMIN`.
- O segredo do JWT nunca é logado.

## Testes

| Camada | Cenário |
| --- | --- |
| Unitário | `Money` rejeita mais de duas casas e serializa como string; `messageFor` cobre tipos conhecidos e desconhecidos |
| Integração | 400 com `errors`; 401 sem token, com token expirado e com assinatura errada; 403 sem perfil; 404 em Problem Details; 500 sem detalhes; app não sobe sem `JWT_SECRET` no perfil padrão |
| Arquitetura | Controllers não dependem de repositórios; `domain` não depende de `api` nem de `infra` |
| Unitário (frontend) | Interceptor faz um único refresh para várias 401 simultâneas e repete as requisições; `RequireAuth` redireciona sem sessão |

## Critérios de aceite

- [ ] Todos os erros da API saem em Problem Details com `type` do catálogo
- [ ] JWT emitido e validado com segredo vindo de configuração
- [ ] Regras ArchUnit ativas no `verify`
- [ ] Frontend com rotas, layout autenticado e cliente HTTP prontos para as features

## Decisões

- **HS256 em vez de RS256:** uma chave simétrica basta com um único emissor e verificador. O PRD já registra RS256 com KMS como evolução de produção.
- **Dinheiro como string no JSON:** evita a perda de precisão do `number` do JavaScript. Vira ADR ao concluir.
- **CSRF desligado até a fase 5:** a API só aceita autenticação por header `Authorization` nesta fase, exceto refresh e logout, que entram com a proteção completa na fase 5.

## Questões em aberto

- Nenhuma.
