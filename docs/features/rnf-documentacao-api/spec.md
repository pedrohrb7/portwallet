# Spec: Documentação da API (OpenAPI e Swagger UI)

- **Status:** Concluída
- **Fase:** 2 (antecipada da fase 8)
- **PRD:** Stack e arquitetura (springdoc-openapi); API
- **Depende de:** [rnf-base-aplicacao](../rnf-base-aplicacao/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Publicar a documentação OpenAPI da API e o Swagger UI desde a Fase 2, para que cada endpoint nasça documentado e quem avalia o projeto consiga explorar e chamar a API pelo navegador.

## Escopo

**Inclui**

- springdoc-openapi com o documento em `/v3/api-docs` e o Swagger UI em `/swagger-ui`.
- Exposição só nos perfis `dev` e `demo`.
- Metadados da API, esquema de autenticação Bearer, respostas de erro em Problem Details e o tipo `Money` documentado como string.
- Proxy no Nginx do `docker compose`.

**Fica para depois**

- Exemplos de requisição e resposta por endpoint: cada feature documenta os próprios endpoints com as anotações do springdoc.

## Comportamento

1. Nos perfis `dev` e `demo`, `GET /v3/api-docs` devolve o documento OpenAPI 3.1 e `GET /swagger-ui` abre o Swagger UI. Os dois são públicos, sem token.
2. Em qualquer outro perfil (incluindo o padrão, `test` e `e2e`), as duas rotas não existem e respondem 404 `not-found`.
3. O documento descreve só a API de negócio (`/api/v1/**`); Actuator e `/mock/**` ficam de fora.
4. O documento declara o esquema `bearerAuth` (HTTP Bearer, JWT) e o aplica a todas as operações por padrão. Endpoints públicos (cadastro, login etc.) removem a exigência com `@SecurityRequirements` vazio.
5. Os erros de cada operação são documentados como `application/problem+json`, com o esquema `Problem` (catálogo em [convenções](../../convencoes.md#api)) e **um exemplo por `type`**, cada um com o `type`, o `title` e o status reais:
   - 400 `validation-error`, só quando a operação recebe corpo ou parâmetros;
   - 401 `unauthorized`, só quando a operação exige token (nunca em endpoints públicos);
   - 500 `internal-error`, sempre;
   - os erros de negócio que o controller declara com `@ApiProblem` (ex.: 400 `invalid-token`, 422 `wrong-password`).

   Erros com o mesmo status ficam na mesma resposta, com um exemplo para cada `type` e a descrição em lista. As respostas aparecem em ordem de status.
6. Respostas de sucesso e corpos de requisição são `application/json`, com exemplos nos campos.
7. `Money` aparece como `string` com o padrão `^-?\d+\.\d{2}$` e exemplo `"150.00"`, conforme o [ADR 0004](../../adr/0004-dinheiro-como-string-no-json.md).
8. O documento usa servidor relativo (`/`), para que o "Try it out" funcione tanto direto na API (`:8080`) quanto atrás do Nginx (`:3000`).
9. No Swagger UI, o botão "Authorize" aceita o access token devolvido pelo login.

## API

| Método | Endpoint | Acesso | Perfis |
| --- | --- | --- | --- |
| GET | `/v3/api-docs` | Público | `dev`, `demo` |
| GET | `/swagger-ui` | Público | `dev`, `demo` |

## Modelo de dados

Nenhuma migration.

## Backend

- Dependência `org.springdoc:springdoc-openapi-starter-webmvc-ui`.
- `application.yml`: `springdoc.api-docs.enabled` e `springdoc.swagger-ui.enabled` desligados por padrão e ligados no bloco `dev | demo`; `springdoc.paths-to-match=/api/v1/**`; `springdoc.default-produces-media-type` e `default-consumes-media-type` em `application/json` (sem eles, o sucesso aparecia como `*/*`); `springdoc.swagger-ui.path=/swagger-ui`.
- `shared.openapi.OpenApiConfig` (ativa só com `springdoc.api-docs.enabled=true`): bean `OpenAPI` com título, versão, descrição, servidor relativo e esquema `bearerAuth`; `OperationCustomizer` que monta as respostas de erro de cada operação a partir da regra 5 (público é quem tem `@SecurityRequirements` vazio; entrada é qualquer parâmetro além de `AuthenticatedUser`); `OpenApiCustomizer` que registra o esquema `Problem` e ordena as respostas por status; substituição do esquema de `Money` por `string` com `SpringDocUtils`. O `Problem` é registrado no customizer, e não no bean `OpenAPI`, porque o springdoc descarta esquemas que ainda não têm referência.
- `SecurityConfig`: `/v3/api-docs/**`, `/swagger-ui/**` e `/swagger-ui.html` liberados. Com o springdoc desligado, a rota não existe e cai no 404 em Problem Details.

## Frontend

- `nginx.conf`: proxy de `/v3/api-docs` e `/swagger-ui` para a API, com `Host` e `X-Forwarded-Host` iguais a `$http_host`. Sem o `X-Forwarded-Host`, o springdoc lê só o `X-Forwarded-Proto` e gera URLs sem a porta pública (`http://localhost/...` em vez de `http://localhost:3000/...`). O Vite não precisa de proxy: em desenvolvimento, o Swagger UI é aberto direto em `http://localhost:8080/swagger-ui`.

## Segurança

- A documentação não existe fora de `dev` e `demo`, para não mapear a API num ambiente real.
- O documento não traz segredos nem exemplos com dados reais.

## Testes

| Camada | Cenário |
| --- | --- |
| Integração | `OpenApiDevProfileTest` e `OpenApiDemoProfileTest`, com o contrato comum `OpenApiDocsContract`: `/v3/api-docs` responde 200 sem token com OpenAPI 3.1, `bearerAuth`, servidor `/`, esquema `Problem`; endpoint protegido sem entrada documenta só 200, 401 e 500, e endpoint público nunca documenta 401; um exemplo próprio por `type` declarado, inclusive dois `type` no mesmo 400; respostas em ordem de status; sucesso em `application/json`; `Money` como string; nada fora de `/api/v1`; `/swagger-ui` redireciona para o `index.html`, que abre sem token. `OpenApiDisabledTest` (perfis `test` e `e2e`): as duas rotas respondem 404 `not-found` |
| Docker | Pelo Nginx na porta 3000: `/v3/api-docs`, `/swagger-ui` (302 para `http://localhost:3000/swagger-ui/index.html`), `/v3/api-docs/swagger-config` com a porta pública e Swagger UI revisado no navegador |

## Critérios de aceite

- [x] Swagger UI acessível em `dev` e `demo`, inexistente nos demais perfis
- [x] Erros e dinheiro documentados conforme as convenções
- [x] Documento validado por teste a cada build

## Decisões

- **Antecipada da fase 8:** documentar cada endpoint junto com a feature que o cria custa menos e mantém o documento sempre atualizado.
- **Só em `dev` e `demo`:** quem avalia acessa pelo `docker compose`; ambientes reais não expõem o mapa da API.
- **Erros por operação, com `@ApiProblem`:** a primeira versão adicionava 400, 401 e 500 a todas as operações com um exemplo genérico de `validation-error`. Endpoints públicos mostravam 401 e um 422 aparecia com exemplo de erro de validação. Agora os genéricos dependem da operação e cada erro de negócio é declarado junto do endpoint, com o próprio exemplo.

## Questões em aberto

- Nenhuma.
