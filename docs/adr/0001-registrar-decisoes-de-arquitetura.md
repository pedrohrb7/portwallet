# 1. Registrar decisões de arquitetura

Data: 2026-10-30

## Status

Aceita

## Contexto

O projeto é um portfólio avaliado por recrutadores e entrevistadores. Mostrar por que cada escolha foi feita vale tanto quanto o código, e a definição de pronto do PRD exige que decisões relevantes sejam registradas.

## Decisão

Usar Architecture Decision Records no formato de Michael Nygard, em `docs/adr/`, numerados em sequência (`NNNN-titulo-em-kebab-case.md`) e escritos em português. Um ADR aceito não é editado: quando a decisão muda, um novo ADR a substitui e o antigo passa a `Substituída por N`.

## Consequências

- Cada fase do roadmap registra suas decisões relevantes antes de ser considerada pronta.
- O histórico das decisões fica versionado junto com o código.
