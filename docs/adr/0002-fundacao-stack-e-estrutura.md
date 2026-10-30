# 2. Fundação: stack, versões e estrutura do repositório

Data: 2026-10-30

## Status

Aceita

## Contexto

O PRD aceita Java 21 + Spring Boot 3.x ou Java 25 + Spring Boot 4, desde que as versões fiquem fixadas. A meta é rodar a aplicação em até 2 comandos depois do clone, e o CI precisa provar build, testes e E2E a cada push.

## Decisão

- **Backend:** Java 21 (LTS) e Spring Boot 3.5.16. O ecossistema citado no PRD (springdoc, Resilience4j, Bucket4j, ShedLock) tem suporte maduro nessa linha.
- **Ferramentas Java fixadas:** `.sdkmanrc` com Temurin 21.0.13 e Maven 3.9.16 para desenvolvimento local; Maven Wrapper (`./mvnw`) no CI e no Docker, para que ninguém dependa de um Maven instalado.
- **Frontend:** React 19, TypeScript, Vite 8, Tailwind CSS 4, npm e Node 24 (`.nvmrc`). Lint com oxlint e formatação com Prettier.
- **Monorepo na raiz:** `backend/`, `frontend/` e `docs/` direto na raiz do repositório, sem a pasta `wallet/` intermediária do PRD.
- **Banco na fase 1:** H2 em memória com `MODE=PostgreSQL`, schema versionado pelo Flyway e `ddl-auto=validate`, para o Hibernate nunca alterar o schema. O H2 em modo servidor entra na fase 7.
- **Health:** Actuator expõe apenas `health` (sem detalhes dos componentes) e `info`, com probes de liveness e readiness. O healthcheck do container usa a readiness.
- **Docker Compose:** o `web` (Nginx com o build do Vite) é a única porta publicada e faz proxy de `/api` e `/actuator/health` para o `api`. Assets com hash recebem `Cache-Control: immutable` e o `index.html`, `no-cache`, como pede o PRD.
- **Testes:** Mockito carregado como agente da JVM no Surefire, e não por auto-attach, que deixará de funcionar em JDKs futuros. JaCoCo gera o relatório de cobertura no `verify`.
- **E2E:** o Playwright sobe o backend e o Vite pelo `webServer`; no CI usa o jar gerado no job de backend, e localmente o `./mvnw spring-boot:run`.

## Consequências

- Atualizar Java ou Spring Boot exige mudar `.sdkmanrc`, `pom.xml`, os Dockerfiles, o CI e a tabela de versões do README.
- O H2 em memória perde os dados a cada reinício até a fase 7, o que é aceitável enquanto não existe seed.
