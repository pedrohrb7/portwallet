# Spec: Fundação

- **Status:** Concluída
- **Fase:** 1
- **PRD:** Roadmap, fase 1; Stack e arquitetura
- **Depende de:** nenhuma
- **Plano:** [plan.md](plan.md)
- **ADR:** [0002](../../adr/0002-fundacao-stack-e-estrutura.md)

## Objetivo

Criar o esqueleto do monorepo com backend, frontend, banco versionado, containers e CI, de modo que toda fase seguinte só acrescente features sobre uma base que já builda, testa e sobe sozinha.

## Escopo

**Inclui**

- Backend Spring Boot com Actuator, Flyway e H2.
- Frontend React + Vite + Tailwind com tela inicial e status da API.
- Docker Compose com Nginx na borda.
- CI no GitHub Actions com build, testes, E2E e smoke test do Docker.

**Fica para depois**

- H2 em modo servidor, Redis e múltiplas instâncias: fase 7.
- ArchUnit e datasource-proxy: fase 2 em diante.

## Comportamento

1. `docker compose up --build` sobe a aplicação em `http://localhost:3000` sem nenhuma configuração prévia.
2. `GET /actuator/health` responde `200 {"status":"UP"}` sem detalhes dos componentes.
3. As probes `/actuator/health/liveness` e `/actuator/health/readiness` respondem 200; o healthcheck do container usa a readiness.
4. O Flyway aplica as migrations na subida e o Hibernate apenas valida o schema (`ddl-auto=validate`).
5. O console do H2 existe só no perfil `dev`.
6. Toda tela mostra o banner "Ambiente de demonstração, sem dinheiro real".
7. A tela inicial mostra o estado da API: Online, Fora do ar ou Indisponível.
8. O Nginx serve assets com hash com `Cache-Control: public, max-age=31536000, immutable` e o `index.html` com `no-cache`, e devolve o `index.html` para rotas desconhecidas.

## API

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| GET | `/actuator/health` | Público | Estado geral |
| GET | `/actuator/health/liveness` | Público | Probe de liveness |
| GET | `/actuator/health/readiness` | Público | Probe de readiness |

O Nginx expõe apenas `/api/` e `/actuator/health`; o resto do Actuator não sai do container.

## Modelo de dados

- `V1__baseline.sql`: tabela `app_metadata (meta_key, meta_value)`, usada só para provar que o Flyway rodou.

## Backend

- `com.portwallet.PortWalletApplication`.
- `application.yml`: datasource configurável por `DATABASE_URL`, `open-in-view=false`, virtual threads, `shutdown=graceful`, erros sem stack trace, perfil `dev` com console do H2.

## Frontend

- `DemoBanner` (`data-testid="demo-banner"`, `role="note"`).
- `ApiStatus` (`data-testid="api-status"`, atributo `data-status` com `LOADING`, `UP`, `DOWN` ou `UNKNOWN`).
- Proxy do Vite para `/api` e `/actuator/health` (`API_URL`, padrão `http://localhost:8080`).

## Testes

| Camada | Cenário |
| --- | --- |
| Integração | Health `UP`; probes expostas; health sem `components`; migration aplicada; console do H2 desligado fora do `dev` |
| Unitário (frontend) | Banner visível; status Online com `UP`; Fora do ar com `DOWN` |
| E2E | Página inicial mostra banner, título e API `UP` (Chromium e Firefox) |
| CI | `docker compose up --wait` e health pela borda do Nginx |

## Critérios de aceite

- [x] Pipeline com endpoint de health e um teste Playwright trivial
- [x] Aplicação rodando em 2 comandos depois do clone
- [x] Versões fixadas no README

## Decisões

Registradas no [ADR 0002](../../adr/0002-fundacao-stack-e-estrutura.md).

## Questões em aberto

- Nenhuma. O pipeline rodou verde no GitHub Actions.
