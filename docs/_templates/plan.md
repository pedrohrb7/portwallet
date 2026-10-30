# Plano: <ID> <Nome da feature>

- **Status:** Rascunho
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** <features concluídas antes desta>

## Passos

Cada passo deixa o build verde e termina com o teste que o valida.

### 1. <Título curto>

- [ ] <tarefa>
- **Arquivos:** `<caminho>`
- **Validação:** <teste ou comando que prova o passo>

### 2. <...>

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] Cenários E2E da feature verdes em Chromium e Firefox
- [ ] Tela revisada em desktop e mobile
- [ ] Spec atualizada com o que mudou na implementação
- [ ] README e índice de `docs/` atualizados

## Riscos

- <risco>: <mitigação>
