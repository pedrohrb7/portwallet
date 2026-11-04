# PortWallet

[English](README.md) · **Português**

Carteira digital fullstack de portfólio: usuários abrem conta, depositam, transferem entre si e consultam extrato, com idempotência, circuit breaker, rate limit e segurança implementados, visíveis na interface e provados por testes.

> Ambiente de demonstração, sem dinheiro real. O produto é pensado para usuários brasileiros, com o português do Brasil como idioma padrão; a interface, as mensagens da API e os e-mails também estão disponíveis em inglês, espanhol e francês, escolhidos pelo navegador ou pelo seletor de idioma no rodapé de cada página ([ADR 0007](docs/adr/0007-multilingual-product.md)). O código e a documentação técnica são em inglês ([ADR 0006](docs/adr/0006-english-docs-portuguese-ui.md)).

O escopo completo está no [PRD](PRD.md). Specs técnicas, planos de implementação, manuais de uso, convenções e decisões de arquitetura ficam em [`docs/`](docs/README.md), em inglês.

## Status

| Fase | Entrega | Status |
| --- | --- | --- |
| 1. Fundação | Monorepo, Maven, Vite, Flyway, Docker Compose, CI, health e E2E trivial | Concluída |
| 2. Núcleo da carteira | Cadastro, login, depósito, transferência, extrato, ledger | Em andamento ([progresso](docs/README.md#features)) |
| 3. Idempotência | `@Idempotent`, replay, limpeza | Pendente |
| 4. Resiliência | Circuit breaker, bulkhead, outbox, painel admin | Pendente |
| 5. Rate limit e segurança base | Bucket4j, refresh rotativo, CSRF, auditoria | Pendente |
| 6. Segurança avançada e performance | TOTP, criptografia, k6, Lighthouse | Pendente |
| 7. Escala horizontal | H2 servidor, Redis, ShedLock, Nginx com 3 instâncias | Pendente |
| 8. Vitrine | GIFs, resultados, deploy público | Pendente |

## Rodando com Docker (2 comandos)

```sh
git clone <url-do-repositorio> portwallet && cd portwallet
docker compose up --build
```

Acesse <http://localhost:7000>. A porta pode ser trocada com `WEB_PORT=7500 docker compose up --build`.

O compose sobe a API no perfil `demo`. Nesse perfil, os e-mails (como o link de ativação da conta) chegam na **caixa de entrada demo**, em <http://localhost:7000/inbox>. O [manual do cadastro](docs/features/fr-01-registration/manual.md) mostra o passo a passo para criar e ativar uma conta, e o [manual do login](docs/features/fr-02-login/manual.md), para entrar nela.

### Variáveis de ambiente

O compose já passa valores de demonstração para todas elas. Fora de uma demo local, defina valores próprios: sem eles, a API só sobe nos perfis `dev` e `e2e`, que têm valores de desenvolvimento.

| Variável | Uso |
| --- | --- |
| `JWT_SECRET` | Segredo do access token (HS256), com no mínimo 32 bytes |
| `PORTWALLET_CRYPTO_ENCRYPTION_KEY` | Chave AES-256-GCM dos campos cifrados (CPF): 32 bytes em Base64 |
| `PORTWALLET_CRYPTO_BLIND_INDEX_KEY` | Chave HMAC-SHA256 do blind index do CPF: 32 bytes em Base64, diferente da anterior |
| `APP_BASE_URL` | Endereço público do frontend, usado nos links enviados por e-mail |

Para gerar uma chave: `openssl rand -base64 32`.

### Modo de desenvolvimento (console do H2)

O compose padrão é a demo pública: a API só é acessada pelo Nginx e o console do H2 fica desligado. Para inspecionar o banco, acrescente o override de desenvolvimento:

```sh
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

Ele sobe a API no perfil `dev` e publica a porta 7080 só no loopback (`127.0.0.1`). O console do H2 fica em <http://localhost:7080/h2-console>: troque a JDBC URL para `jdbc:h2:mem:portwallet` (o formulário sugere `jdbc:h2:~/test`), usuário `sa`, sem senha. O banco é em memória, então começa vazio a cada reinício da API. Para voltar à demo, rode `docker compose up -d` de novo.

## Versões fixadas

| Ferramenta | Versão |
| --- | --- |
| Java | 21 (Temurin 21.0.13) |
| Maven | 3.9.16 (via Maven Wrapper) |
| Spring Boot | 3.5.16 |
| Node.js | 24 (`frontend/.nvmrc`) |
| React | 19 |
| Vite | 8 |
| TypeScript | 7 |
| Tailwind CSS | 4 |
| Vitest | 5 |
| Playwright | 1.64 |

## Desenvolvimento local

### Pré-requisitos

O JDK e o Maven estão fixados no [`.sdkmanrc`](.sdkmanrc). Com o [SDKMAN](https://sdkman.io) instalado:

```sh
sdk env install   # instala as versões do .sdkmanrc
sdk env           # ativa as versões no shell atual
```

Node.js 24 também é necessário para o frontend.

### Backend

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # API em http://localhost:7080
./mvnw verify                                           # testes + cobertura (target/site/jacoco)
```

No perfil `dev`, o console do H2 fica em <http://localhost:7080/h2-console> (JDBC URL `jdbc:h2:mem:portwallet`, usuário `sa`, sem senha). Os e-mails simulados ficam na caixa de entrada demo, em <http://localhost:7173/inbox> com o frontend rodando.

### Frontend

```sh
cd frontend
npm install
npm run dev         # http://localhost:7173, com proxy de /api para o backend
npm test            # testes unitários (Vitest)
npm run lint        # oxlint
npm run typecheck   # tsc
npm run format      # Prettier
```

### Testes E2E

```sh
cd frontend
npx playwright install chromium firefox   # apenas na primeira vez
npm run e2e
```

O Playwright sobe o backend no perfil `e2e` e o servidor do Vite sozinho; se eles já estiverem rodando, são reaproveitados. O relatório fica em `frontend/playwright-report`.

## Estrutura

```text
portwallet/
├── backend/            Spring Boot (Maven), pacotes por funcionalidade
├── frontend/           React + Vite + Tailwind
│   └── e2e/            testes Playwright
├── docs/               specs, planos, manuais, convenções e ADRs
├── .github/workflows/  pipeline de CI
└── docker-compose.yml
```

## Documentação da API

O Swagger UI fica disponível nos perfis `dev` e `demo`, e não existe nos demais:

- Com Docker: <http://localhost:7000/swagger-ui>. A porta 7080 não é publicada: no compose, a API só é acessada pela borda do Nginx (a não ser no [modo de desenvolvimento](#modo-de-desenvolvimento-console-do-h2)).
- Com o backend rodando localmente (`./mvnw spring-boot:run`) no perfil `dev`: <http://localhost:7080/swagger-ui>

O documento OpenAPI 3.1 fica em `/v3/api-docs`. Para chamar endpoints protegidos, faça login e cole o `accessToken` em "Authorize".

## Endpoints

| Endpoint | Descrição |
| --- | --- |
| `POST /api/v1/auth/register` | Cadastro (nome, e-mail, CPF); sempre a mesma resposta 202 |
| `POST /api/v1/auth/confirm/summary` | Dados mascarados do cadastro a que um link de confirmação pertence |
| `POST /api/v1/auth/confirm` | Ativa a conta, definindo a senha, e abre a carteira |
| `GET /mock/inbox?to=<e-mail>` | Caixa de entrada demo (só nos perfis `dev`, `e2e` e `demo`) |
| `GET /actuator/health` | Estado geral da API, sem detalhes dos componentes |
| `GET /actuator/health/liveness` | Probe de liveness |
| `GET /actuator/health/readiness` | Probe de readiness, usada pelo healthcheck do Docker |

## CI

O GitHub Actions roda a cada push no `main`. Um primeiro job detecta o que mudou, e cada verificação só roda quando o código que ela cobre mudou. Mudanças só em documentação (`docs/`, arquivos `.md`) não disparam nenhuma verificação.

| Job | O que faz | Roda quando muda |
| --- | --- | --- |
| **Backend** | `./mvnw verify` (testes e relatório de cobertura JaCoCo) | `backend/` |
| **Frontend** | Lint, formatação, tipos, testes unitários e build | `frontend/` |
| **E2E** | Playwright em Chromium e Firefox contra o backend; o relatório HTML é publicado como artefato | `backend/` ou `frontend/` |
| **Docker** | Sobe o `docker compose` e verifica o health pela borda do Nginx | `backend/`, `frontend/` ou `docker-compose.yml` |

Qualquer mudança no próprio workflow (`.github/workflows/ci.yml`) roda todos os jobs. Um job pulado aparece como "skipped", e o E2E roda mesmo que o backend ou o frontend tenham sido pulados, desde que nenhum job executado tenha falhado.
