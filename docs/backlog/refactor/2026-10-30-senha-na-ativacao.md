# Senha definida na ativação, em vez de conferida na ativação

- **Status:** Done (2026-11-03, sem PR: trabalho local, ainda não commitado)
- **Opened:** 2026-10-30
- **Decision:** [ADR 0005](../../adr/0005-senha-definida-na-ativacao.md)
- **Scope:** backend `auth` (cadastro, confirmação, entidade `User`, e-mails), `shared.openapi`, nova migration em `users`; frontend `features/auth` (`/register`, `/confirm`) e `api/`; testes de integração, unitários e E2E do cadastro; `docs/features/rf-01-cadastro`, `docs/convencoes.md`; PRD (RF-01)

## Motivation

A correção do pré-sequestro de conta no RF-01 é segura, mas tem custos que um modelo mais simples evita:

- A senha trafega duas vezes: no cadastro e de novo na ativação.
- Uma senha escolhida antes da verificação do e-mail fica gravada, mesmo que a conta nunca seja ativada.
- A ativação é mais uma rota que confere senha, além do login, e por isso precisa de rate limit próprio (fase 5) e de cuidado com logs.
- O erro `wrong-password` existe só por causa desse desenho.

Se a senha for definida na ativação, só quem tem o link (e, portanto, acesso ao e-mail) escolhe a senha. O pré-sequestro do acesso continua impossível sem nenhuma rota além do login conferindo senha.

## Current state

- `POST /api/v1/auth/register` recebe nome, e-mail, CPF e senha. O usuário fica `PENDING` com o hash BCrypt da senha.
- Recadastro de e-mail `PENDING` substitui nome, CPF e senha pelos dados novos e invalida os links anteriores (exceto quando o CPF novo é de outro usuário).
- `POST /api/v1/auth/confirm` recebe token e senha, confere a senha com a do cadastro mais recente e, se bater, ativa o usuário e abre a conta. Senha diferente devolve 422 `wrong-password` sem consumir o token.
- A tela `/confirm` pede a senha do cadastro.

Detalhes em [`docs/features/rf-01-cadastro/spec.md`](../../features/rf-01-cadastro/spec.md), regras 3 e 6 a 9.

## Proposed change

**Regras de negócio**

1. O cadastro pede só nome, e-mail e CPF. Os quatro casos de resposta do RF-01 continuam, com o mesmo 202, e o recadastro de e-mail `PENDING` continua substituindo nome e CPF e invalidando os links anteriores.
2. Usuários `PENDING` não têm senha. Todo usuário `ACTIVE` ou `BLOCKED` tem.
3. O e-mail de confirmação mostra o nome e os dois últimos dígitos do CPF do cadastro.
4. A tela de ativação mostra um resumo do cadastro (nome e CPF mascarados) e pergunta "Estes são os seus dados?". Se não forem, orienta a refazer o cadastro, o que gera um novo link.
5. Na ativação, o usuário **define** a senha (8 a 72 caracteres, até 72 bytes, com campo de confirmação no cliente). A senha é gravada, o usuário é ativado, o token é consumido e a conta é aberta, na mesma transação.
6. A ativação nunca confere senha; `wrong-password` deixa de existir. Token inválido continua sendo 400 `invalid-token`.
7. Login de usuário `PENDING` continua falhando com `invalid-credentials`.

**Por que o resumo (regras 3 e 4).** Sem a senha no cadastro, o acesso fica protegido, mas os dados não: um atacante que recadastrasse o e-mail da vítima entre o cadastro dela e o clique faria a vítima ativar uma conta com o nome e o CPF dele. Como só o link mais recente funciona, o resumo antes da ativação deixa a vítima perceber a troca. O CPF continua verificado em silêncio no cadastro, então o resumo não abre enumeração: só aparece para quem tem o link e só com os dígitos finais.

**API**

- `POST /api/v1/auth/register`: `{ "name", "email", "document" }`, resposta 202 igual à atual.
- `POST /api/v1/auth/confirm/summary` (novo): `{ "token" }` no corpo, não na URL, para não aparecer em logs de acesso. Resposta 200 `{ "maskedName": "Ana S****", "maskedDocument": "***.***.***-25" }`; 400 `invalid-token`.
- `POST /api/v1/auth/confirm`: `{ "token", "password" }`, agora grava a senha. Erros: 400 `validation-error` e 400 `invalid-token`.

**Dados.** Nova migration (número definido na implementação):

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD CONSTRAINT ck_users_password_when_active
    CHECK (status = 'PENDING' OR password_hash IS NOT NULL);
```

**Código**

- Backend: `RegisterRequest`, `RegisterCommand`, `User.register` e `User.replacePendingRegistration` sem senha; `User.activate(passwordHash)`; `ConfirmationService.summary` e `confirm` sem `PasswordEncoder.matches`; `AuthEmails.confirmation` com nome e final do CPF; `NameMasker` antecipado do RF-17; remoção de `WrongPasswordException` e do `@ApiProblem` de `wrong-password`.
- Frontend: `/register` sem senha; `/confirm` com resumo, nova senha, confirmação da senha e link para refazer o cadastro; remoção de `wrong-password` em `problem.ts`.

**Alternativas descartadas**

- *Manter a senha conferida na ativação (estado atual):* seguro, mas com os custos da seção Motivation.
- *Associar cada envio do cadastro ao próprio token:* resolveria a troca de nome e CPF sem depender da atenção do usuário, mas exigiria uma tabela nova de cadastros pendentes. O resumo cobre o caso com menos estrutura.
- *Cadastro só com e-mail (nome e CPF também na ativação):* a verificação do CPF iria para a ativação e não poderia ser silenciosa, permitindo a quem tem um e-mail próprio descobrir se um CPF está cadastrado.

**Ordem de implementação**

1. Migration e teste de que o `CHECK` impede `ACTIVE` sem senha.
2. Cadastro sem senha e e-mail com o resumo (`RegistrationTest`).
3. Resumo e ativação com definição da senha; remoção de `wrong-password` do código, do catálogo em `docs/convencoes.md` e do OpenAPI (`ConfirmationTest`, cenários de pré-sequestro reescritos: o atacante não entra em nenhuma ordem de cadastro e a troca de dados aparece no resumo).
4. Frontend (`RegisterPage.test.tsx`, `ConfirmPage.test.tsx`, `schemas.test.ts`).
5. E2E: cadastro sem senha, resumo, definição da senha, link usado duas vezes, recadastro troca o resumo.
6. Documentação: ADR 0005, spec e plano do RF-01 atualizados (a correção anterior fica registrada como substituída), Swagger revisado.

**Verificação:** `./mvnw verify`, `npm run lint && npm run typecheck && npm test`, `npm run e2e` em Chromium e Firefox, telas de cadastro e ativação revisadas em desktop e mobile, nenhuma ocorrência de `wrong-password` no código e na documentação.

## Risks

- **Mudança no PRD:** o RF-01 deixa de pedir senha no cadastro. Precisa de aprovação antes de começar.
- **Usuários `PENDING` já existentes com senha:** continuam válidos; a ativação substitui o hash, sem migração de dados.
- **Dependência do RF-02:** o login assume que todo usuário ativo tem senha. O `CHECK` da migration garante isso e deve existir antes do RF-02. Executar este refactor antes do RF-02 evita refazer os testes do login por causa da mudança no contrato de `/auth/confirm`.
- **Troca de nome e CPF depende da atenção do usuário ao resumo:** mitigado pelo resumo também no e-mail; a alternativa sem essa dependência está registrada acima, caso o risco se mostre relevante.

## Out of scope

- Rate limit da ativação e do cadastro (fase 5).
- Recuperação de senha (RF-14).
- Qualquer mudança no fluxo de login além de usuários `PENDING` sem senha.

## Outcome

Implementado em 2026-11-03, na ordem proposta. Diferenças em relação ao proposto:

- **Migration `V5__password_set_on_activation.sql`** também apaga o hash dos usuários `PENDING` existentes, em vez de mantê-lo até a ativação: assim a regra 2 vale para todos os dados e nenhuma senha escolhida antes da verificação do e-mail fica gravada. Como consequência, `refresh_tokens` do RF-02 passou a ser `V6` e o ledger, `V7`.
- **`NameMasker` em `shared.privacy`**, e não em `account.domain` como previa o RF-17: `auth` e `account` o usam. A máscara do CPF virou `Cpf.masked()`.
- **Bug encontrado no E2E e corrigido:** abrir um segundo link de confirmação na mesma aba em que `/confirm` já estava aberta só muda o fragmento da URL, sem recarregar a página, e a tela continuava mostrando o resultado do link anterior. A tela agora escuta `hashchange` e recomeça com o novo token.
- O exemplo de 422 do teste de contrato do OpenAPI (`OpenApiDocsContract`) passou a usar `insufficient-balance`.
