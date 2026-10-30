# PortWallet

Carteira digital fullstack de portfólio: usuários abrem conta, depositam, transferem entre si e consultam extrato, com idempotência, circuit breaker, rate limit e segurança implementados, visíveis na interface e provados por testes.

> Ambiente de demonstração, sem dinheiro real.

O escopo completo está no [PRD](PRD.md). As decisões de arquitetura ficam em [`docs/adr/`](docs/adr/).

## Status

| Fase | Entrega | Status |
| --- | --- | --- |
| 1. Fundação | Monorepo, Maven, Vite, Flyway, Docker Compose, CI, health e E2E trivial | Concluída |
| 2. Núcleo da carteira | Cadastro, login, depósito, transferência, extrato, ledger | Pendente |
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

Acesse <http://localhost:3000>. A porta pode ser trocada com `WEB_PORT=8000 docker compose up --build`.

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
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # API em http://localhost:8080
./mvnw verify                                           # testes + cobertura (target/site/jacoco)
```

No perfil `dev`, o console do H2 fica em <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:portwallet`, usuário `sa`, sem senha).

### Frontend

```sh
cd frontend
npm install
npm run dev         # http://localhost:5173, com proxy de /api para o backend
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
├── docs/adr/           decisões de arquitetura
├── .github/workflows/  pipeline de CI
└── docker-compose.yml
```

## Endpoints

| Endpoint | Descrição |
| --- | --- |
| `GET /actuator/health` | Estado geral da API, sem detalhes dos componentes |
| `GET /actuator/health/liveness` | Probe de liveness |
| `GET /actuator/health/readiness` | Probe de readiness, usada pelo healthcheck do Docker |

## CI

O GitHub Actions roda a cada push e pull request:

- **Backend:** `./mvnw verify` (testes e relatório de cobertura JaCoCo).
- **Frontend:** lint, formatação, tipos, testes unitários e build.
- **E2E:** Playwright em Chromium e Firefox contra o jar do backend; o relatório HTML é publicado como artefato.
- **Docker:** sobe o `docker compose` e verifica o health pela borda do Nginx.
