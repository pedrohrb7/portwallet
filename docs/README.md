# Documentação

Toda mudança relevante no PortWallet nasce de uma feature documentada aqui. O [PRD](../PRD.md) diz **o que** construir; as specs dizem **como**; os planos dizem **em que ordem**; os ADRs registram **por que** uma decisão foi tomada.

```text
docs/
├── README.md           este índice
├── convencoes.md       regras transversais (API, dinheiro, erros, código)
├── _templates/         modelos de spec e plano
├── adr/                decisões de arquitetura (imutáveis depois de aceitas)
└── features/
    └── <id>-<slug>/
        ├── spec.md     especificação técnica
        └── plan.md     plano de implementação
```

## Fluxo de trabalho

1. **Spec.** Copie `_templates/spec.md` para `features/<id>-<slug>/spec.md` e descreva o comportamento, o contrato da API, o modelo de dados e os testes que provam a feature. Status `Rascunho`.
2. **Revisão.** Dúvidas ficam na seção "Questões em aberto". Quando não houver mais nenhuma, a spec passa para `Aprovada`.
3. **Plano.** Copie `_templates/plan.md` e quebre a spec em passos pequenos e verificáveis, cada um com os arquivos afetados e o teste que o valida.
4. **Implementação.** Marque os passos do plano conforme forem concluídos. Se a implementação mudar uma decisão, atualize a spec no mesmo trabalho; spec desatualizada é bug.
5. **Encerramento.** Com os critérios de aceite atendidos, spec e plano passam para `Concluída`, e as decisões de longo prazo viram ADR.

### Status

| Status | Significado |
| --- | --- |
| Rascunho | Em escrita ou com questões em aberto |
| Aprovada | Pronta para implementar |
| Em implementação | Plano em execução |
| Concluída | Critérios de aceite atendidos e CI verde |
| Substituída | Trocada por outra feature, indicada no topo do documento |

### Nomes

- `rf-NN-<slug>`: requisito funcional do PRD (ex.: `rf-05-transferencia`).
- `rnf-<slug>`: requisito não funcional ou trabalho transversal (ex.: `rnf-ledger-concorrencia`).
- Slugs em português, minúsculos, em kebab-case.

### Spec ou ADR?

A spec descreve uma feature e evolui com ela. O ADR registra uma decisão que vale para além de uma feature (escolha de biblioteca, formato de dados, trade-off de arquitetura) e não é editado depois de aceito. Uma spec pode citar ADRs; quando ela toma uma decisão desse porte, a decisão vira ADR ao final.

## Features

| Fase | Feature | Spec | Plano | Status |
| --- | --- | --- | --- | --- |
| 1 | Fundação | [spec](features/rnf-fundacao/spec.md) | [plano](features/rnf-fundacao/plan.md) | Concluída |
| 2 | Base da aplicação (erros, segurança, shell do frontend) | [spec](features/rnf-base-aplicacao/spec.md) | [plano](features/rnf-base-aplicacao/plan.md) | Rascunho |
| 2 | RF-01 Cadastro e confirmação | [spec](features/rf-01-cadastro/spec.md) | [plano](features/rf-01-cadastro/plan.md) | Rascunho |
| 2 | RF-02 Login e refresh | [spec](features/rf-02-login/spec.md) | [plano](features/rf-02-login/plan.md) | Rascunho |
| 2 | RF-03 Logout | [spec](features/rf-03-logout/spec.md) | [plano](features/rf-03-logout/plan.md) | Rascunho |
| 2 | Ledger e concorrência | [spec](features/rnf-ledger-concorrencia/spec.md) | [plano](features/rnf-ledger-concorrencia/plan.md) | Rascunho |
| 2 | RF-08 Saldo | [spec](features/rf-08-saldo/spec.md) | [plano](features/rf-08-saldo/plan.md) | Rascunho |
| 2 | RF-04 Depósito simulado | [spec](features/rf-04-deposito/spec.md) | [plano](features/rf-04-deposito/plan.md) | Rascunho |
| 2 | RF-17 Busca de destinatário | [spec](features/rf-17-busca-destinatario/spec.md) | [plano](features/rf-17-busca-destinatario/plan.md) | Rascunho |
| 2 | RF-05 Transferência | [spec](features/rf-05-transferencia/spec.md) | [plano](features/rf-05-transferencia/plan.md) | Rascunho |
| 2 | RF-06 Consulta de transferência | [spec](features/rf-06-consulta-transferencia/spec.md) | [plano](features/rf-06-consulta-transferencia/plan.md) | Rascunho |
| 2 | RF-07 Extrato | [spec](features/rf-07-extrato/spec.md) | [plano](features/rf-07-extrato/plan.md) | Rascunho |

A ordem da tabela é a ordem de implementação da Fase 2: cada feature depende apenas das que estão acima dela.

## ADRs

| ADR | Título |
| --- | --- |
| [0001](adr/0001-registrar-decisoes-de-arquitetura.md) | Registrar decisões de arquitetura |
| [0002](adr/0002-fundacao-stack-e-estrutura.md) | Fundação: stack, versões e estrutura do repositório |
| [0003](adr/0003-specs-e-planos-por-feature.md) | Specs e planos por feature |
