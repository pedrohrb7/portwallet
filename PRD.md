# PRD — Carteira Digital (Projeto de Portfólio)

Oct 30, 2026 · @Pedro

## Visão geral

Uma carteira digital fullstack para portfólio: usuários abrem conta, depositam, transferem entre si e consultam extrato, com idempotência, circuit breaker, rate limit e segurança implementados, visíveis na interface e provados por testes.

**Problema.** Projetos de portfólio costumam ser CRUDs que não mostram como o candidato lida com falhas, concorrência e abuso. Numa carteira esses problemas são reais: um retry duplicado vira dinheiro em dobro, um serviço externo fora do ar não pode travar as transferências e um login sem limite vira alvo de força bruta.

**Público.** Recrutadores e entrevistadores técnicos, que avaliam o projeto pelo README, pela demo e pelo código em poucos minutos.

**Escopo.** Um monólito modular e stateless, com backend Spring Boot + H2 e frontend React, executado em várias instâncias atrás de um load balancer. Os serviços externos (antifraude e notificação) são simulados dentro do próprio projeto, com falhas controláveis. Não há dinheiro real nem integração bancária.

## Objetivos e métricas de sucesso

O projeto é bem-sucedido quando um avaliador consegue rodar a aplicação e ver os quatro conceitos funcionando em menos de 10 minutos.

**Objetivos**

- Idempotência em operações financeiras, provada por teste concorrente.
- Resiliência com circuit breaker e bulkhead visíveis ao vivo num painel.
- Rate limit com respostas 429 e headers padrão, global entre instâncias.
- Segurança: JWT, autorização por perfil, step-up com TOTP, proteção contra força bruta e enumeração, criptografia em repouso e auditoria à prova de adulteração.
- Saldo consistente sob concorrência, com ledger de dupla entrada e lock otimista.
- Performance medida, não presumida: teste de carga, controle de queries e metas de front-end.
- Escala horizontal: API stateless rodando em 3 instâncias atrás de um load balancer, com o estado compartilhado no H2 em modo servidor e no Redis.

**Métricas**

| Métrica                                                                         | Meta                                          |
| ------------------------------------------------------------------------------- | --------------------------------------------- |
| Comandos do clone até a aplicação rodando                                       | ≤ 2                                           |
| Cobertura de testes do backend (domínio e serviços)                             | ≥ 80% das linhas                              |
| Cenários E2E no Playwright                                                      | ≥ 12 fluxos críticos, verdes no CI            |
| Transações criadas por 50 requisições paralelas com a mesma chave               | Exatamente 1                                  |
| Saldo após 100 transferências concorrentes                                      | Igual à soma do ledger, nunca negativo        |
| Latência p95 de transferências, 100 usuários virtuais por 5 min (k6)            | < 200 ms, com erro < 1%                       |
| Queries SQL por endpoint                                                        | Teto fixo por endpoint, verificado em teste   |
| Lighthouse (performance e acessibilidade)                                       | ≥ 90                                          |
| Pipeline de CI                                                                  | Build, testes e E2E a cada push               |
| Throughput de transferências, de 1 para 3 instâncias (k6)                       | ≥ 2x, ou o gargalo identificado e documentado |
| Transferências perdidas ou duplicadas ao derrubar uma instância durante a carga | 0                                             |

**Não-objetivos**

- Integração com bancos, PIX real ou gateway de pagamento.
- Multimoeda e câmbio.
- Autoescala em Kubernetes (fica como extensão opcional; o projeto escala com Docker Compose).
- Aplicativo mobile.

## Personas e user stories

Três personas cobrem o produto: quem usa a carteira, quem a opera e quem avalia o portfólio.

| Persona       | Quem é                      | O que precisa                                                |
| ------------- | --------------------------- | ------------------------------------------------------------ |
| Usuário       | Cliente da carteira         | Movimentar dinheiro com segurança e sem cobranças duplicadas |
| Administrador | Operador da plataforma      | Conter fraude e acompanhar a saúde do sistema                |
| Avaliador     | Recrutador ou entrevistador | Testar rápido, sem cadastro, e ver os conceitos em ação      |

| ID    | Como          | Quero                                                      | Para                                      |
| ----- | ------------- | ---------------------------------------------------------- | ----------------------------------------- |
| US-01 | Usuário       | Criar conta com e-mail e senha                             | Acessar a carteira                        |
| US-02 | Usuário       | Fazer login e permanecer autenticado com segurança         | Não digitar a senha a toda hora           |
| US-03 | Usuário       | Depositar saldo simulado                                   | Ter dinheiro para transferir              |
| US-04 | Usuário       | Transferir para outro usuário pelo e-mail                  | Pagar alguém                              |
| US-05 | Usuário       | Ver o extrato paginado e filtrado                          | Acompanhar minhas movimentações           |
| US-06 | Usuário       | Reenviar uma transferência com a conexão instável          | Ter certeza de que ela não será duplicada |
| US-07 | Administrador | Bloquear e desbloquear contas                              | Conter fraude                             |
| US-08 | Administrador | Estornar uma transferência                                 | Corrigir erros sem apagar histórico       |
| US-09 | Administrador | Ver o estado dos circuit breakers e ligar falhas simuladas | Demonstrar e acompanhar a resiliência     |
| US-10 | Avaliador     | Entrar com contas demo já populadas                        | Testar sem cadastro                       |

## Requisitos funcionais

Dezessete requisitos, treze deles obrigatórios para a primeira versão. Prioridade em MoSCoW: Must (obrigatório) e Should (desejável).

| ID    | Requisito                       | Regras                                                                                                                                                                                                                                                                                                 | Prioridade |
| ----- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| RF-01 | Cadastro                        | Nome, e-mail e CPF fictício com formato validado. Resposta sempre genérica (202), exista o e-mail ou não. Conta ativada pelo link de confirmação, onde o usuário confere o resumo do cadastro e define a senha (mínimo de 8 caracteres).                                                              | Must       |
| RF-02 | Login                           | Devolve access token JWT (15 min) e refresh token (7 dias) em cookie httpOnly, rotacionado a cada uso. Erro genérico para e-mail inexistente ou senha errada.                                                                                                                                          | Must       |
| RF-03 | Logout                          | Revoga o refresh token atual.                                                                                                                                                                                                                                                                          | Must       |
| RF-04 | Depósito simulado               | Valor > 0 e ≤ R$ 10.000 por operação. Exige `Idempotency-Key`.                                                                                                                                                                                                                                         | Must       |
| RF-05 | Transferência                   | Destino: qualquer usuário cadastrado e ativo, pelo e-mail ou CPF, confirmado pela busca de destinatário (RF-17); valor > 0; saldo suficiente; destino ativo e diferente do remetente; limite diário de R$ 5.000; passa pelo antifraude. Exige `Idempotency-Key`. Acima de R$ 1.000, exige código TOTP. | Must       |
| RF-06 | Consulta de transferência       | O remetente ou o destinatário consulta uma transferência pelo ID.                                                                                                                                                                                                                                      | Must       |
| RF-07 | Extrato                         | Paginado por cursor, filtrável por período e tipo, do mais recente para o mais antigo.                                                                                                                                                                                                                 | Must       |
| RF-08 | Saldo                           | Coluna atualizada na mesma transação dos lançamentos; deve sempre bater com a soma do ledger.                                                                                                                                                                                                          | Must       |
| RF-09 | Estorno                         | Admin estorna uma transferência gerando lançamentos inversos; nada é apagado. Exige `Idempotency-Key`.                                                                                                                                                                                                 | Should     |
| RF-10 | Notificação                     | Após a transferência, remetente e destinatário são notificados de forma assíncrona via outbox.                                                                                                                                                                                                         | Should     |
| RF-11 | Admin: usuários                 | Listar, buscar, bloquear e desbloquear contas. Conta bloqueada não envia nem recebe.                                                                                                                                                                                                                   | Must       |
| RF-12 | Admin: resiliência              | Painel com o estado dos circuit breakers e bulkheads, e controles de taxa de falha e latência dos serviços simulados.                                                                                                                                                                                  | Must       |
| RF-13 | Dados demo                      | Seed com dois usuários e um admin demo, já confirmados, com histórico de transações.                                                                                                                                                                                                                   | Must       |
| RF-14 | Recuperação de senha            | Resposta sempre genérica. Link com token de uso único, válido por 30 min; só o hash do token é guardado. A troca revoga todas as sessões.                                                                                                                                                              | Should     |
| RF-15 | Autenticação em dois fatores    | O usuário ativa o TOTP lendo um QR code e confirma com um código. Transferências acima de R$ 1.000 e troca de senha exigem o código (step-up). 5 códigos errados em 15 min bloqueiam a verificação e geram registro na auditoria.                                                                      | Must       |
| RF-16 | Admin: integridade da auditoria | Verifica a cadeia de hashes do log de auditoria e aponta o primeiro registro adulterado.                                                                                                                                                                                                               | Should     |
| RF-17 | Busca de destinatário           | Usuário autenticado informa e-mail ou CPF e recebe o nome mascarado ("Ana S\*\*\*\*") de um usuário cadastrado e ativo, para conferir antes de confirmar. Também lista os 5 destinatários mais recentes. Não encontrado ou inativo: mensagem única, sem dizer qual dos dois.                           | Must       |

E-mails de confirmação, recuperação de senha e notificações saem pelo serviço de notificação simulado e ficam visíveis numa caixa de entrada de demonstração, habilitada só nos perfis `dev` e `e2e`.

## Requisitos não funcionais

Estes são os requisitos que o portfólio existe para demonstrar. Cada um tem uma implementação definida e um teste que prova o comportamento.

### Idempotência

- Header `Idempotency-Key` (UUID v4) obrigatório em `POST` de depósito, transferência e estorno. Sem ele, a resposta é 400.
- Tabela `idempotency_keys` com chave única composta (chave + usuário), hash SHA-256 do corpo, status (`PROCESSING` ou `COMPLETED`), status HTTP e corpo da resposta, e expiração de 24 h.
- Fluxo:
  1. A requisição tenta inserir a chave com status `PROCESSING`. A constraint única garante que só uma vence uma corrida.
  2. Mesma chave e mesmo corpo, já concluída: devolve a resposta salva com o header `Idempotent-Replayed: true`.
  3. Mesma chave com corpo diferente: 422.
  4. Mesma chave ainda em `PROCESSING`: 409.
- Implementada como anotação `@Idempotent` com interceptor reutilizável, sem lógica espalhada nos controllers.
- Um job `@Scheduled` remove chaves expiradas.

### Circuit breaker

- Resilience4j nos clientes de antifraude e de notificação, que chamam os serviços simulados por HTTP para manter o cenário realista.
- Configuração do antifraude: janela deslizante de 10 chamadas; abre com ≥ 50% de falhas ou de chamadas lentas (> 2 s); fica 15 s aberto; libera 3 chamadas em half-open.
- Retry com backoff exponencial (2 tentativas) só para erros transitórios, envolvendo o circuit breaker.
- Fallback do antifraude, como regra de negócio documentada: transferências até R$ 500 são aprovadas com `fraudCheck = SKIPPED` para revisão posterior; acima disso, a resposta é 503 com mensagem clara.
- Fallback da notificação: o evento fica na outbox e é reenviado quando o circuito fechar.
- Estado exposto no Actuator e no painel admin.

### Rate limit

Bucket4j com buckets no Redis, um por chave, compartilhados por todas as instâncias: o limite é global, e não multiplicado pelo número de instâncias.

| Escopo                | Chave       | Limite                                                      |
| --------------------- | ----------- | ----------------------------------------------------------- |
| Login                 | IP + e-mail | 5 tentativas a cada 15 min                                  |
| Cadastro              | IP          | 3 por hora                                                  |
| Transferências        | Usuário     | 10 por minuto                                               |
| API autenticada       | Usuário     | 100 por minuto                                              |
| API pública           | IP          | 30 por minuto                                               |
| Busca de destinatário | Usuário     | 20 por minuto e 200 por dia                                 |
| Código TOTP errado    | Usuário     | 5 erros a cada 15 min; depois, bloqueio até o fim da janela |

- Ao exceder: 429 com `Retry-After`, `X-RateLimit-Limit` e `X-RateLimit-Remaining`; corpo no formato Problem Details (RFC 9457).
- O frontend mostra uma contagem regressiva até liberar.

### Segurança

- Spring Security stateless com JWT. Access token de 15 min guardado em memória no frontend, nunca em `localStorage`.
- Refresh token em cookie httpOnly, `Secure`, `SameSite=Strict`, rotacionado a cada uso. Reuso de um token antigo revoga toda a família.
- Senhas com BCrypt (custo 12). Segredo do JWT em variável de ambiente.
- Perfis `USER` e `ADMIN` com `@PreAuthorize`. Checagem de propriedade em todo recurso para evitar IDOR.
- Bean Validation em todos os DTOs. Dinheiro sempre em `BigDecimal` com escala 2, nunca `double`.
- CORS restrito à origem do frontend; headers de segurança (CSP, `X-Content-Type-Options`, `X-Frame-Options`).
- Console do H2 habilitado só no perfil `dev`.
- Erros sem stack trace para o cliente; logs sem senha, token ou CPF completo.
- Tabela `audit_log` para login, falha de login, transferência, estorno e bloqueio.

### Segurança avançada

- **Step-up com TOTP (RFC 6238):** transferências acima de R$ 1.000 e troca de senha exigem o código do autenticador no header `X-TOTP-Code`. Sem ele, a API responde 403 com `type: totp-required` e o frontend abre o modal do código. Um código já usado não vale de novo na mesma janela. Como um código de 6 dígitos tem só 1 milhão de combinações, 5 códigos errados em 15 minutos bloqueiam a verificação TOTP do usuário até o fim da janela (429 com `type: totp-locked` e `Retry-After`). Cada erro e cada bloqueio são registrados na auditoria. O contador fica no Redis, então vale para todas as instâncias.
- **Anti-enumeração:** login, cadastro e recuperação de senha devolvem o mesmo status e a mesma mensagem, exista o e-mail ou não. Para igualar o tempo de resposta, o login roda o BCrypt contra um hash fictício quando o usuário não existe; o cadastro com e-mail já usado envia um aviso ao dono em vez de um erro.
- **CSRF no refresh:** além do `SameSite=Strict`, padrão double-submit com `CookieCsrfTokenRepository`: cookie `XSRF-TOKEN` legível pelo JavaScript e header `X-XSRF-TOKEN` obrigatório em `/auth/refresh` e `/auth/logout`, os únicos endpoints que dependem de cookie.
- **Criptografia em repouso:** CPF e segredo TOTP cifrados com AES-256-GCM via `AttributeConverter` do JPA, com IV aleatório por registro e chave em variável de ambiente. Unicidade e busca pelo CPF usam um blind index (`HMAC-SHA256` do CPF) em coluna separada.
- **Auditoria à prova de adulteração:** cada registro guarda `hash = SHA-256(prev_hash + conteúdo)`. A inserção é serializada para manter a ordem da cadeia, e `GET /admin/audit/verify` recalcula tudo e aponta o primeiro registro alterado.

A busca de destinatário (RF-17) revela, por natureza, se um e-mail ou CPF está cadastrado. É o mesmo trade-off da consulta de chave PIX, aceito de forma explícita e contido assim: só para usuários autenticados, com rate limit próprio, nome sempre mascarado e cada consulta registrada na auditoria.

### Concorrência e consistência

- Ledger de dupla entrada: cada movimentação gera um débito e um crédito de mesmo valor, na mesma transação.
- Lock otimista (`@Version`) na conta. Em conflito, até 3 novas tentativas; depois, 409.
- Saldo nunca negativo: validado no serviço e garantido por `CHECK (balance >= 0)` no banco.
- Outbox pattern: o evento de notificação é gravado na mesma transação da transferência e publicado por um job.

### Observabilidade

- Actuator com health (incluindo os circuit breakers) e métricas.
- Logs estruturados em JSON com `correlationId` no MDC, propagado pelo header `X-Correlation-Id`.
- Métricas próprias: transferências por status, replays idempotentes e respostas 429 emitidas.

### Performance

- **Paginação por cursor no extrato:** cursor opaco (Base64 de `created_at` + `id`) e consulta `WHERE (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT ?`. O custo por página é constante e nenhum item é pulado ou repetido quando entram transações novas, ao contrário do `OFFSET`.
- **Índices desenhados para as consultas:** `ledger_entries (account_id, created_at DESC, id DESC)` para o extrato, `transactions (source_account_id, created_at)` para o limite diário e `idempotency_keys (expires_at)` para a limpeza. O `EXPLAIN` antes e depois fica registrado em ADR.
- **N+1 sob controle:** leituras por projeções em DTO e `@EntityGraph`, com `spring.jpa.open-in-view=false`. Testes com datasource-proxy falham se um endpoint passar do número de queries esperado.
- **Bulkhead:** Resilience4j limita a 10 as chamadas simultâneas ao antifraude. O excedente cai direto no fallback, em vez de prender threads enquanto o serviço está lento.
- **Virtual threads:** `spring.threads.virtual.enabled=true`, com benchmark k6 comparando throughput e p95 com e sem, sob latência simulada no antifraude.
- **ETag e 304:** `/accounts/me` e a primeira página do extrato devolvem ETag derivada da `version` da conta. O polling do frontend envia `If-None-Match` e recebe 304 sem corpo quando nada mudou.
- **Teste de carga:** cenários k6 versionados em `load/`, com meta de p95 < 200 ms e erro < 1% em transferências, 100 usuários virtuais por 5 minutos. Resultados e gráficos no README.
- **Frontend:** rotas de admin com `React.lazy`; assets com hash servidos com `Cache-Control: public, max-age=31536000, immutable` e `index.html` com `no-cache`; Lighthouse ≥ 90 verificado no CI com Lighthouse CI.
- **Sem optimistic updates em operações financeiras:** a interface só mostra o novo saldo depois da confirmação do servidor. Decisão registrada em ADR.

### Escala horizontal

A API não guarda estado em memória: qualquer instância atende qualquer requisição. A demonstração sobe 3 instâncias atrás de um Nginx com `docker compose up --scale api=3`.

| Estado                    | Onde fica                 | Por que funciona com várias instâncias                                                    |
| ------------------------- | ------------------------- | ----------------------------------------------------------------------------------------- |
| Sessão do usuário         | JWT no cliente            | Nenhuma instância guarda sessão; todas validam com a mesma chave                          |
| Refresh tokens            | H2 em modo servidor       | Rotação e detecção de reuso numa única tabela                                             |
| Chaves de idempotência    | H2 em modo servidor       | A chave primária composta resolve a corrida mesmo entre instâncias diferentes             |
| Saldo e ledger            | H2 em modo servidor       | Lock otimista pela coluna `version`                                                       |
| Buckets de rate limit     | Redis                     | Limite global, não multiplicado pelo número de instâncias                                 |
| Códigos TOTP já usados    | Redis com TTL             | Um código usado numa instância é recusado nas outras; o contador de erros também é global |
| Configuração de caos      | H2 em modo servidor       | Todas as instâncias simulam a mesma falha                                                 |
| Estado do circuit breaker | Memória de cada instância | Proposital: cada instância se protege sozinha; o painel mostra o estado por instância     |

- **Banco compartilhado:** o H2 roda em modo servidor (TCP) num container próprio, em vez de embarcado em cada instância.
- **Jobs agendados:** outbox e limpeza de chaves rodam com ShedLock (provedor JDBC), para que só uma instância execute cada job por vez.
- **Cadeia de auditoria:** a ordem é garantida no banco, com `SELECT ... FOR UPDATE` numa linha de controle que guarda o último hash, e não com um lock em memória.
- **Load balancer:** Nginx em round-robin, com health checks passivos (`max_fails`). O readiness do Actuator fica exposto para um orquestrador futuro.
- **Encerramento gracioso:** `server.shutdown=graceful`; a instância conclui as requisições em andamento antes de sair.
- **Distribuição visível:** cada resposta traz o header `X-Instance-Id`, e o painel admin mostra qual instância atendeu cada chamada.
- **Prova:** k6 contra 1 e 3 instâncias, comparando o throughput, e um teste que derruba uma instância no meio da carga sem perder nem duplicar transferências.

Com um único H2, o banco vira o gargalo e o ponto único de falha da demonstração. Isso é esperado e está registrado em Riscos.

## Stack e arquitetura

Um monólito modular em Spring Boot, replicado em várias instâncias atrás de um Nginx, servindo uma SPA React. O estado compartilhado fica no H2 em modo servidor e no Redis. As barreiras de rate limit, autenticação e idempotência ficam na borda, antes de qualquer regra de negócio.

&#91;embedded content: arquitetura · SPA, monólito Spring Boot, H2 e serviços simulados\]

Os serviços externos são simulados no próprio app, com controles de falha e latência, para que o circuit breaker possa ser demonstrado ao vivo. O painel admin lê o estado dos circuitos pelo Actuator.

| Camada              | Tecnologia                                                                 | Uso                                                                   |
| ------------------- | -------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| Backend             | Java 21 (LTS) + Spring Boot 3.x                                            | Web, Validation, Data JPA, Security, Actuator                         |
| Banco               | H2 + Flyway                                                                | Migrations versionadas; `MODE=PostgreSQL`                             |
| Autenticação        | Spring Security + OAuth2 Resource Server (Nimbus JWT)                      | Emissão e validação de JWT; CSRF double-submit no refresh             |
| 2FA                 | Biblioteca TOTP (RFC 6238) + gerador de QR code                            | Step-up em operações sensíveis                                        |
| Criptografia        | JCA (AES-GCM, HMAC-SHA256)                                                 | Campos cifrados e blind index                                         |
| Resiliência         | Resilience4j                                                               | Circuit breaker, bulkhead, retry e time limiter                       |
| Rate limit          | Bucket4j + Redis                                                           | Buckets compartilhados entre instâncias                               |
| Documentação        | springdoc-openapi                                                          | Swagger UI em `/swagger-ui`                                           |
| Frontend            | React + TypeScript + Vite                                                  | SPA com rotas carregadas sob demanda                                  |
| Estilo              | Tailwind CSS                                                               | Utilitários e tema                                                    |
| Dados e formulários | TanStack Query, React Hook Form, Zod, Axios                                | Cache de servidor, validação e interceptors                           |
| Testes              | JUnit 5, Mockito, WireMock, ArchUnit, datasource-proxy, Vitest, Playwright | Unitário, integração, queries, arquitetura e E2E                      |
| Performance         | k6, Lighthouse CI                                                          | Carga no backend e qualidade do front-end                             |
| Infra               | Docker Compose, GitHub Actions                                             | Execução local e CI                                                   |
| Escala              | Nginx, H2 em modo servidor, Redis, ShedLock                                | Load balancer, banco e estado compartilhados, jobs com execução única |

Versões mais novas (Java 25, Spring Boot 4) também servem; o importante é fixá-las no README.

**Estrutura do repositório**

```text
wallet/
├── backend/            Spring Boot (Maven), pacotes por funcionalidade:
│                       auth, account, transfer, ledger, idempotency,
│                       resilience, ratelimit, admin, mock, shared
├── frontend/           React + Vite + Tailwind
│   └── e2e/            testes Playwright
├── docs/adr/           decisões de arquitetura
├── docker-compose.yml
└── README.md
```

## Modelo de dados

Nove tabelas, versionadas com Flyway. O ledger é a fonte da verdade: lançamentos são imutáveis e o saldo da conta é um valor derivado, mantido na mesma transação.

| Tabela             | Campos principais                                                                                                                  | Observações                                                                                                                 |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `users`            | id (UUID), name, email, password\_hash, document\_enc, document\_hash, totp\_secret\_enc, totp\_enabled, role, status, created\_at | E-mail único; status `PENDING`, `ACTIVE` ou `BLOCKED`; `*_enc` cifrados com AES-GCM; `document_hash` é o blind index, único |
| `accounts`         | id, user\_id, balance, daily\_limit, version, created\_at                                                                          | 1:1 com usuário; `DECIMAL(19,2)`; `CHECK (balance >= 0)`; `version` para lock otimista e ETag                               |
| `transactions`     | id, type, status, amount, source\_account\_id, target\_account\_id, fraud\_check, idempotency\_key, created\_at                    | type: `DEPOSIT`, `TRANSFER`, `REVERSAL`; fraud\_check: `APPROVED`, `SKIPPED`, `DENIED`                                      |
| `ledger_entries`   | id, transaction\_id, account\_id, direction, amount, balance\_after, created\_at                                                   | `DEBIT` ou `CREDIT`; imutável, sem update nem delete                                                                        |
| `idempotency_keys` | key, user\_id, request\_hash, status, response\_code, response\_body, created\_at, expires\_at                                     | Chave primária composta (key, user\_id)                                                                                     |
| `outbox_events`    | id, aggregate\_id, type, payload, status, attempts, next\_attempt\_at                                                              | Payload em JSON; reprocessado com backoff                                                                                   |
| `refresh_tokens`   | id, user\_id, token\_hash, family\_id, expires\_at, revoked                                                                        | Guarda só o hash; `family_id` detecta reuso                                                                                 |
| `user_tokens`      | id, user\_id, purpose, token\_hash, expires\_at, used\_at                                                                          | purpose: `EMAIL_CONFIRMATION` ou `PASSWORD_RESET`; uso único                                                                |
| `audit_log`        | id, user\_id, action, ip, details, prev\_hash, hash, created\_at                                                                   | Somente inserção; `hash` encadeia com o registro anterior                                                                   |

**Invariantes**

- Depósitos debitam uma conta de sistema (`EXTERNAL_FUNDS`), mantendo a dupla entrada sem exceções.
- Para toda transação, a soma dos débitos é igual à soma dos créditos.
- Para toda conta, `balance` é igual a créditos menos débitos no ledger. Um teste de integração verifica isso após cada cenário concorrente.

## API

REST versionada em `/api/v1`, documentada com springdoc-openapi (Swagger UI). Todo erro segue Problem Details (RFC 9457) com um `type` estável, como `insufficient-balance`.

| Método | Endpoint                         | Acesso        | Idempotency-Key | Descrição                                                                       |
| ------ | -------------------------------- | ------------- | --------------- | ------------------------------------------------------------------------------- |
| POST   | `/auth/register`                 | Público       | Não             | Cadastra e envia o link de confirmação; resposta sempre 202                     |
| POST   | `/auth/confirm`                  | Público       | Não             | Ativa a conta com o token do link                                               |
| POST   | `/auth/login`                    | Público       | Não             | Devolve access token e define os cookies de refresh e XSRF                      |
| POST   | `/auth/refresh`                  | Cookie + XSRF | Não             | Rotaciona o refresh e devolve novo access token                                 |
| POST   | `/auth/logout`                   | Cookie + XSRF | Não             | Revoga o refresh token                                                          |
| POST   | `/auth/password/forgot`          | Público       | Não             | Envia o link de recuperação; resposta sempre 202                                |
| POST   | `/auth/password/reset`           | Público       | Não             | Troca a senha com o token do link e revoga as sessões                           |
| POST   | `/auth/2fa/setup`                | USER          | Não             | Gera o segredo TOTP e devolve o QR code                                         |
| POST   | `/auth/2fa/enable`               | USER          | Não             | Confirma o primeiro código e ativa o 2FA                                        |
| GET    | `/accounts/me`                   | USER          | Não             | Saldo, limite diário e quanto já foi usado hoje; com ETag                       |
| POST   | `/deposits`                      | USER          | Sim             | Depósito simulado                                                               |
| POST   | `/transfers`                     | USER          | Sim             | Nova transferência; `X-TOTP-Code` acima de R$ 1.000                             |
| GET    | `/transfers/{id}`                | USER (dono)   | Não             | Detalhe da transferência                                                        |
| GET    | `/statement`                     | USER          | Não             | Extrato com `cursor`, `size`, `from`, `to`, `type`; devolve `nextCursor`        |
| GET    | `/admin/users`                   | ADMIN         | Não             | Lista e busca usuários                                                          |
| PATCH  | `/admin/users/{id}/status`       | ADMIN         | Não             | Bloqueia ou desbloqueia                                                         |
| POST   | `/admin/transfers/{id}/reversal` | ADMIN         | Sim             | Estorna uma transferência                                                       |
| GET    | `/admin/resilience`              | ADMIN         | Não             | Estado dos circuit breakers e bulkheads                                         |
| PUT    | `/admin/chaos`                   | ADMIN         | Não             | Define taxa de falha e latência de um serviço simulado                          |
| GET    | `/admin/audit/verify`            | ADMIN         | Não             | Verifica a cadeia de hashes da auditoria                                        |
| GET    | /recipients/lookup               | USER          | Não             | Busca por e-mail ou CPF; devolve um ID opaco do destinatário e o nome mascarado |
| GET    | /recipients/recent               | USER          | Não             | Os 5 destinatários mais recentes do usuário                                     |

Os serviços simulados ficam fora de `/api`, em `/mock/antifraud/check` e `/mock/notifications`, acessíveis só internamente. A caixa de entrada de demonstração fica em `/mock/inbox`, só nos perfis `dev` e `e2e`.

**Códigos de erro**

| Status | Quando                                                                                                                                 |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------- |
| 202    | Cadastro e recuperação de senha, com resposta idêntica exista o e-mail ou não                                                          |
| 304    | ETag igual ao `If-None-Match`: nada mudou                                                                                              |
| 400    | Falha de validação, `Idempotency-Key` ausente ou cursor inválido                                                                       |
| 401    | Token ausente, inválido ou expirado; credenciais inválidas (mensagem genérica)                                                         |
| 403    | Perfil sem permissão, recurso de outro usuário, conta bloqueada, token XSRF inválido ou código TOTP exigido (`totp-required`)          |
| 404    | Recurso inexistente                                                                                                                    |
| 409    | Chave ainda em processamento ou conflito de concorrência após as novas tentativas                                                      |
| 422    | Saldo insuficiente, limite diário excedido, transferência para si mesmo, chave reutilizada com corpo diferente ou código TOTP inválido |
| 429    | Rate limit excedido ou verificação TOTP bloqueada (`totp-locked`)                                                                      |
| 503    | Antifraude indisponível ou bulkhead cheio, com valor acima do limite do fallback                                                       |

**Exemplo**

```http
POST /api/v1/transfers
Authorization: Bearer <access-token>
Idempotency-Key: 6f1c2d3e-8a9b-4c5d-9e0f-1a2b3c4d5e6f
Content-Type: application/json

{ "targetEmail": "ana@demo.com", "amount": 150.00, "description": "Almoço" }
```

## Frontend

SPA em React + TypeScript com Vite e Tailwind CSS. Estado do servidor com TanStack Query, formulários com React Hook Form + Zod, rotas com React Router e um cliente Axios com interceptors para refresh de token, `Idempotency-Key` e `X-Correlation-Id`.

| Tela                  | Rota                                  | Conteúdo                                                                                                      | Comportamentos-chave                                                                                                                                       |
| --------------------- | ------------------------------------- | ------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Login                 | `/login`                              | E-mail, senha, atalhos para contas demo                                                                       | Contagem regressiva ao receber 429; erro genérico                                                                                                          |
| Cadastro              | `/register`                           | Nome, e-mail, CPF, senha                                                                                      | Validação em tempo real com Zod; mesma mensagem final em qualquer caso                                                                                     |
| Confirmação           | `/confirm`                            | Resultado da ativação                                                                                         | Lê o token da URL e o remove do histórico                                                                                                                  |
| Recuperar senha       | `/forgot-password`, `/reset-password` | Pedido do link e nova senha                                                                                   | Mensagem genérica no pedido                                                                                                                                |
| Dashboard             | `/`                                   | Saldo, limite diário usado, últimas transações                                                                | Valores em BRL com `Intl.NumberFormat`; polling com ETag                                                                                                   |
| Depositar             | `/deposit`                            | Valor                                                                                                         | Mesma proteção idempotente da transferência                                                                                                                |
| Transferir            | `/transfer`                           | Busca do destinatário por e-mail ou CPF, recentes, valor, descrição, tela de confirmação com o nome mascarado | Chave gerada ao abrir a confirmação e reaproveitada em retries e duplo clique; botão desabilitado durante o envio; modal de código TOTP quando a API pedir |
| Extrato               | `/statement`                          | Lista com rolagem infinita e filtros                                                                          | Paginação por cursor; filtros refletidos na URL                                                                                                            |
| Segurança da conta    | `/settings/security`                  | Ativação do 2FA com QR code, troca de senha                                                                   | Troca de senha exige código TOTP                                                                                                                           |
| Caixa de entrada demo | `/inbox`                              | E-mails enviados pelo serviço simulado                                                                        | Só nos perfis `dev` e `e2e`                                                                                                                                |
| Admin: usuários       | `/admin/users`                        | Busca, status, bloquear e desbloquear                                                                         | Rota protegida por perfil; carregada sob demanda                                                                                                           |
| Admin: resiliência    | `/admin/resilience`                   | Estado de cada circuit breaker e bulkhead, taxa de falhas, controles de caos                                  | Atualização a cada 2 s, sem recarregar a página                                                                                                            |
| Admin: auditoria      | `/admin/audit`                        | Últimos eventos e botão de verificar integridade                                                              | Destaca o primeiro registro adulterado                                                                                                                     |

**Diretrizes**

- Seletores estáveis para testes via `data-testid` e papéis ARIA.
- Feedback para todo erro da API, mapeado pelo `type` do Problem Details.
- Layout responsivo e acessível: contraste, foco visível, navegação por teclado.
- Banner "Ambiente de demonstração, sem dinheiro real" em todas as telas.

## Estratégia de testes

Cada conceito do projeto tem um teste que prova o comportamento, não só a configuração. Os testes de concorrência e os E2E são a vitrine.

| Camada                | Ferramentas                          | O que cobre                                                                                                                                                                                                                                                   |
| --------------------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Unitário (backend)    | JUnit 5, Mockito, AssertJ            | Regras de domínio: saldo, limite diário, geração do ledger, hash da requisição, codificação do cursor                                                                                                                                                         |
| Integração            | `@SpringBootTest`, MockMvc, H2       | Endpoints, migrations, 401/403, IDOR, Problem Details, ETag e 304                                                                                                                                                                                             |
| Concorrência          | `ExecutorService` + `CountDownLatch` | 50 requisições paralelas com a mesma chave criam 1 transação; 100 transferências cruzadas mantêm o saldo igual ao ledger                                                                                                                                      |
| Resiliência           | WireMock                             | Erros 500 e timeouts abrem o circuito; fallback correto; half-open volta a fechar; bulkhead cheio cai no fallback                                                                                                                                             |
| Rate limit            | Integração                           | A 6ª tentativa de login devolve 429 com `Retry-After`                                                                                                                                                                                                         |
| Segurança             | Integração                           | Mesma resposta e tempo semelhante para e-mail existente ou não; refresh sem XSRF devolve 403; CPF gravado cifrado; registro de auditoria alterado é detectado; TOTP reusado é recusado; o 6º código errado em 15 min devolve 429 e gera registro na auditoria |
| Queries               | datasource-proxy                     | Teto de queries por endpoint, falhando em caso de N+1                                                                                                                                                                                                         |
| Arquitetura           | ArchUnit                             | Controllers não acessam repositórios; dependências entre módulos respeitadas                                                                                                                                                                                  |
| Carga                 | k6                                   | p95 e taxa de erro de transferências; comparação com e sem virtual threads                                                                                                                                                                                    |
| Unitário (frontend)   | Vitest, Testing Library              | Componentes, formatação de moeda, hooks de API                                                                                                                                                                                                                |
| Front-end (qualidade) | Lighthouse CI                        | Performance e acessibilidade ≥ 90                                                                                                                                                                                                                             |
| E2E                   | Playwright                           | Fluxos críticos abaixo                                                                                                                                                                                                                                        |
| Escala                | k6, Docker Compose                   | Throughput com 1 e 3 instâncias; instância derrubada no meio da carga sem perda nem duplicação; mesma Idempotency-Key enviada a instâncias diferentes gera 1 transação; rate limit global entre instâncias                                                    |

**Cenários E2E com Playwright**

1. Cadastro, confirmação pela caixa de entrada demo, login e dashboard com saldo zero.
2. Depósito atualiza o saldo no dashboard.
3. Busca do destinatário mostra o nome mascarado, e a transferência aparece no extrato dos dois usuários (dois contextos de navegador).
4. Saldo insuficiente exibe o erro correto e não altera o saldo.
5. Duplo clique em "Confirmar" gera uma única transação (contando requisições com `page.route`).
6. Falha de rede na primeira tentativa, abortada com `page.route`, seguida de retry não duplica a transferência.
7. Cinco logins errados bloqueiam temporariamente e exibem a contagem regressiva.
8. Admin liga falhas no antifraude, o painel mostra o circuito `OPEN`, uma transferência pequena é aprovada com aviso e uma grande é recusada.
9. Usuário comum não acessa rotas `/admin`.
10. Refresh de token transparente após a expiração do access token, com o relógio controlado por `page.clock`.
11. Usuário ativa o 2FA; uma transferência acima de R$ 1.000 abre o modal e só conclui com o código correto (gerado no teste a partir do segredo).
12. Recuperação de senha pela caixa de entrada demo; a sessão antiga deixa de funcionar após a troca.

**Configuração do Playwright**

- `webServer` sobe o backend no perfil `e2e` (com seed) e o frontend.
- Projetos Chromium e Firefox; trace e vídeo na primeira nova tentativa.
- Cada teste cria seus próprios usuários pela API, sem depender da ordem de execução e sem `waitForTimeout`.
- Roda no GitHub Actions, publicando o relatório HTML como artefato.

## Roadmap e critérios de aceite

Oito fases, cada uma entregável sozinha: se o projeto parar no meio, o que existe já está publicável. As durações são estimativas para dedicação de meio período.

1. **Fundação (\~1 semana).** Monorepo, Maven, Vite, Flyway, Docker Compose e GitHub Actions.
   - Aceite: pipeline verde com um endpoint de health e um teste Playwright trivial.
2. **Núcleo da carteira (\~2 semanas).** Cadastro, login JWT, contas, depósito, busca de destinatário, transferência, extrato com paginação por cursor, ledger e lock otimista.
   - Aceite: teste de 100 transferências concorrentes passa; cenários E2E 1 a 4 verdes.
3. **Idempotência (\~1 semana).** Anotação `@Idempotent`, tabela de chaves, replay e limpeza; chave gerada no frontend.
   - Aceite: teste de 50 requisições paralelas passa; cenários E2E 5 e 6 verdes.
4. **Resiliência (\~1–2 semanas).** Serviços simulados, circuit breaker, bulkhead, retry, fallbacks, outbox e painel admin.
   - Aceite: testes com WireMock passam; cenário E2E 8 verde.
5. **Rate limit e segurança base (\~1 semana).** Bucket4j, rotação de refresh token, CSRF double-submit, anti-enumeração, auditoria e headers de segurança.
   - Aceite: cenários E2E 7, 9 e 10 verdes; scan de baseline do OWASP ZAP sem alertas altos.
6. **Segurança avançada e performance (\~2 semanas).** 2FA com step-up, recuperação de senha, criptografia em repouso, cadeia de auditoria, índices, testes de N+1, ETag, virtual threads e teste de carga.
   - Aceite: cenários E2E 11 e 12 verdes; metas de p95 e Lighthouse atingidas; teto de queries verificado em todos os endpoints.
7. **Escala horizontal (\~1 semana).** H2 em modo servidor, Redis para rate limit e TOTP, ShedLock nos jobs, Nginx com 3 instâncias, encerramento gracioso e `X-Instance-Id`.
   - Aceite: k6 compara 1 e 3 instâncias; derrubar uma instância durante a carga não perde nem duplica transferências; toda a suíte E2E verde contra o ambiente com 3 instâncias.
8. **Vitrine (\~1 semana).** README com GIFs da demo, resultados do k6, diagrama, ADRs, Swagger e deploy público.
   - Aceite: um avaliador roda o projeto em até 2 comandos e entende os conceitos pelo README.

**Definição de pronto, em qualquer fase**

- [ ] Testes novos escritos e o CI verde
- [ ] Cobertura do domínio ≥ 80%
- [ ] Decisões relevantes registradas em ADR (`docs/adr/`)
- [ ] README atualizado com o que a fase entregou

## Riscos, limitações e o que mudaria em produção

As simplificações são escolhas conscientes para um portfólio. Documentá-las no README mostra que o autor sabe onde está cada limite.

| Limitação no projeto                                              | Em produção                                                                             |
| ----------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Um único H2 em modo servidor: gargalo e ponto único de falha      | PostgreSQL gerenciado com failover e réplicas de leitura, com Testcontainers nos testes |
| Números de carga medidos com H2, otimistas e limitados pelo banco | Repetir o k6 contra PostgreSQL em ambiente parecido com o de produção                   |
| Um único Redis                                                    | Redis com Sentinel ou Cluster                                                           |
| Nginx e réplicas fixas no Docker Compose                          | Kubernetes com HPA, escalando por CPU ou latência                                       |
| Chaves de idempotência no banco principal                         | Redis com TTL, ou tabela própria com particionamento                                    |
| Outbox publicada por polling com ShedLock                         | CDC (Debezium) ou mensageria (Kafka, RabbitMQ)                                          |
| Serviços externos simulados no próprio app                        | Serviços reais, com testes de contrato (Pact)                                           |
| Segredo do JWT e chave de criptografia em variáveis de ambiente   | Cofre de segredos ou KMS com envelope encryption e rotação de chaves; JWT em RS256      |
| Cadeia de auditoria no mesmo banco que ela protege                | Cópia periódica dos hashes para armazenamento imutável (WORM)                           |
| Caixa de entrada simulada                                         | Provedor de e-mail transacional real                                                    |
| Lock otimista em contas muito disputadas                          | Lock pessimista ou fila por conta para contas de alto volume                            |

**Riscos do desenvolvimento**

- **Escopo crescer demais.** Mitigação: seguir as fases e só começar a próxima com a anterior aceita.
- **H2 se comportar diferente do PostgreSQL.** Mitigação: usar `MODE=PostgreSQL` e evitar SQL específico de um banco.
- **Testes E2E instáveis.** Mitigação: dados isolados por teste, esperas por estado da tela em vez de tempo fixo, trace habilitado no CI.
- **Conceitos que não aparecem para quem avalia.** Mitigação: painel de resiliência, GIFs no README e uma seção "Como testar cada conceito em 1 minuto".
