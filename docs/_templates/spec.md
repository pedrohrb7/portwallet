# Spec: <ID> <Nome da feature>

- **Status:** Rascunho
- **Fase:** <N>
- **PRD:** <RF-NN, seção do PRD>
- **Depende de:** <features, ou "nenhuma">
- **Plano:** [plan.md](plan.md)

## Objetivo

<Uma ou duas frases: o que a feature entrega e para quem.>

## Escopo

**Inclui**

- <...>

**Fica para depois**

- <item>: <fase ou feature onde entra>

## Comportamento

<Regras de negócio numeradas, do caminho feliz às bordas. Cada regra deve poder virar um teste.>

1. <...>

## API

### `<MÉTODO> /api/v1/<rota>`

- **Acesso:** <Público | USER | ADMIN>
- **Headers:** <obrigatórios>

Requisição:

```json
{}
```

Resposta `<status>`:

```json
{}
```

| Status | `type` | Quando |
| --- | --- | --- |
| <...> | <...> | <...> |

## Modelo de dados

<Migrations novas (`V<N>__<nome>.sql`), tabelas, colunas, constraints e índices.>

## Backend

<Pacotes, classes principais e responsabilidades.>

## Frontend

<Rotas, componentes, estados de tela e `data-testid`.>

## Segurança

<Autorização, checagem de propriedade, dados sensíveis, o que nunca vai para log.>

## Testes

| Camada | Cenário |
| --- | --- |
| Unitário | <...> |
| Integração | <...> |
| E2E | <...> |

## Critérios de aceite

- [ ] <...>

## Decisões

<Escolhas feitas nesta spec e o motivo. As de longo prazo viram ADR ao concluir.>

## Questões em aberto

- <...>
