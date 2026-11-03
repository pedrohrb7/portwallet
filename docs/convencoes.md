# Convenções

Regras que valem para todas as features. Uma spec só repete uma regra daqui para abrir uma exceção, e explica o motivo.

## API

- Base `/api/v1`. Recursos no plural, rotas em kebab-case, JSON em camelCase.
- Toda resposta de erro segue Problem Details (RFC 9457), com `Content-Type: application/problem+json`.
- O campo `type` é um slug estável em kebab-case (ex.: `insufficient-balance`), que o frontend usa para escolher a mensagem. `title` é uma frase curta em português; `detail` é opcional e nunca traz dado sensível.
- Erros de validação (400, `type: validation-error`) trazem `errors: [{ "field": "amount", "message": "..." }]`.
- Nenhuma resposta traz stack trace, nome de classe ou SQL.

### Catálogo de `type`

| `type` | Status | Quando |
| --- | --- | --- |
| `validation-error` | 400 | Bean Validation, JSON malformado, parâmetro inválido |
| `invalid-cursor` | 400 | Cursor do extrato que não decodifica |
| `invalid-token` | 400 | Token de confirmação desconhecido, expirado ou já usado (resumo e ativação) |
| `unauthorized` | 401 | Access token ausente, inválido ou expirado |
| `invalid-credentials` | 401 | Login com e-mail inexistente, senha errada ou conta não confirmada |
| `invalid-refresh-token` | 401 | Refresh token ausente, expirado ou revogado |
| `forbidden` | 403 | Perfil sem permissão ou recurso de outro usuário |
| `account-blocked` | 403 | Conta do usuário bloqueada tentando movimentar dinheiro |
| `not-found` | 404 | Recurso ou rota inexistente |
| `method-not-allowed` | 405 | Método HTTP não suportado pela rota |
| `not-acceptable` | 406 | Formato de resposta pedido em `Accept` não suportado |
| `unsupported-media-type` | 415 | `Content-Type` da requisição não suportado |
| `recipient-not-found` | 404 | Busca de destinatário sem resultado (inexistente ou inativo, sem distinção) |
| `concurrency-conflict` | 409 | Lock otimista falhou após as novas tentativas |
| `insufficient-balance` | 422 | Saldo menor que o valor |
| `daily-limit-exceeded` | 422 | Transferência passaria do limite diário |
| `self-transfer` | 422 | Destino igual à origem |
| `recipient-unavailable` | 422 | Destinatário deixou de estar ativo entre a busca e a transferência |
| `internal-error` | 500 | Erro inesperado; detalhes só no log |

Fases seguintes acrescentam tipos (`idempotency-*`, `totp-*`, `rate-limited`, `fraud-check-unavailable`) nas próprias specs e neste catálogo.

## Dinheiro

- Sempre `BigDecimal` com escala 2 e `RoundingMode.UNNECESSARY`: um valor com mais de duas casas é rejeitado, nunca arredondado.
- No banco, `DECIMAL(19,2)`.
- No JSON, **string** com duas casas (`"150.00"`), em requisições e respostas. Requisições também aceitam número, convertido sem perda. Strings evitam a perda de precisão do `number` do JavaScript ([ADR 0004](adr/0004-dinheiro-como-string-no-json.md)).
- DTOs de resposta usam o tipo `Money`; DTOs de requisição podem usar `BigDecimal` com `@Digits(integer = 17, fraction = 2)` para a validação apontar o campo.
- No frontend, valores ficam como string até a formatação com `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`. Contas de dinheiro não são feitas no cliente.

## Datas e fuso

- Instantes em UTC, `TIMESTAMP WITH TIME ZONE` no banco e ISO-8601 com `Z` no JSON.
- Regras de "dia" (limite diário, filtros do extrato) usam o fuso `America/Sao_Paulo`.
- O código obtém a hora por um `Clock` injetado, para que os testes controlem o tempo.

## Identificadores

- UUID gerado pela aplicação para todas as entidades.
- IDs internos de usuário nunca aparecem em respostas para outros usuários; a busca de destinatário devolve o ID da conta, que só serve como destino de transferência.

## Backend

- Pacotes por funcionalidade em `com.portwallet`: `auth`, `account`, `ledger`, `transfer`, `mock`, `shared` (e, nas próximas fases, `idempotency`, `resilience`, `ratelimit`, `admin`).
- Dentro de cada pacote: `api` (controllers e DTOs), `domain` (entidades e regras), `application` (casos de uso, transações) e `infra` (repositórios e integrações), criados conforme a necessidade.
- Controllers não acessam repositórios; regras de negócio não ficam em controllers. Verificado por ArchUnit a partir da Fase 2.
- DTOs são `record`s com Bean Validation. Entidades JPA nunca saem da camada `application`.
- Documentação OpenAPI de cada endpoint: `@Operation` com resumo, `@ApiResponse` para o sucesso, `@Schema(example = ...)` nos campos dos DTOs, `@SecurityRequirements` vazio nos endpoints públicos e um `@ApiProblem` para cada erro de negócio que o endpoint pode devolver. Os erros genéricos (400 de validação, 401 e 500) são adicionados sozinhos e não devem ser declarados.
- `spring.jpa.open-in-view=false`; leituras por projeção em DTO.
- Migrations Flyway em `db/migration`, nomeadas `V<N>__<descricao_em_snake_case>.sql`, nunca editadas depois de publicadas. SQL portátil entre H2 (`MODE=PostgreSQL`) e PostgreSQL.
- Logs nunca contêm senha, token, segredo TOTP ou CPF completo.

## Frontend

- Rotas em `src/routes`, features em `src/features/<nome>`, componentes compartilhados em `src/components`, cliente HTTP em `src/api`.
- Estado do servidor com TanStack Query; formulários com React Hook Form + Zod; HTTP com Axios.
- Access token só em memória, nunca em `localStorage` ou `sessionStorage`.
- Erros da API mapeados pelo `type` do Problem Details para mensagens em português.
- Seletores de teste por papel ARIA e `data-testid`. Nada de `waitForTimeout` nos E2E.
- Interface em português do Brasil; textos de código, nomes de teste e commits em inglês.

## Testes

- Nomes de teste descrevem o comportamento (`rejectsTransferAboveBalance`).
- Testes de integração usam o perfil `test` e criam os próprios dados; nenhum teste depende da ordem de execução.
- Cada regra numerada de uma spec tem ao menos um teste que a prova.
