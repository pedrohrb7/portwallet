# 3. Specs e planos por feature

Data: 2026-10-30

## Status

Aceita

## Contexto

O PRD descreve o produto inteiro, mas não detalha contratos de API, modelo de dados nem a ordem de implementação. Sem um lugar para esse detalhe, as decisões ficam espalhadas no código e o avaliador do portfólio não vê o raciocínio por trás delas.

## Decisão

- Cada requisito do PRD vira uma feature em `docs/features/<id>-<slug>/`, com uma `spec.md` (como funciona) e um `plan.md` (passos de implementação). Requisitos funcionais usam `rf-NN-<slug>`; trabalho transversal usa `rnf-<slug>`.
- Os documentos partem dos modelos em `docs/_templates/` e seguem o ciclo de status descrito em `docs/README.md`.
- Regras que valem para todas as features ficam em `docs/convencoes.md`, e não repetidas em cada spec.
- A spec é atualizada junto com o código quando a implementação muda uma decisão.

## Consequências

- Cada fase do roadmap corresponde a um grupo de features, listado no índice de `docs/README.md`.
- Specs pequenas e focadas facilitam a revisão, mas exigem disciplina para manter as referências cruzadas corretas.
- Decisões de longo prazo tomadas numa spec ainda precisam virar ADR, porque a spec evolui e o ADR não.
