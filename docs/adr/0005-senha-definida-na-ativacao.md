# 5. Senha definida na ativação

Data: 2026-11-03

## Status

Aceita. Substitui a decisão "Senha na ativação contra pré-sequestro de conta" da [spec do RF-01](../features/rf-01-cadastro/spec.md), em que a ativação conferia a senha escolhida no cadastro.

## Contexto

Quem cadastra primeiro o e-mail de outra pessoa não pode acabar escolhendo a senha da conta que o dono ativa depois (pré-sequestro de conta). A primeira correção exigia, na ativação, a mesma senha do cadastro mais recente. Era segura, mas a senha trafegava duas vezes, ficava gravada antes da verificação do e-mail, a ativação virava uma segunda rota que confere senha (com rate limit e cuidado de logs próprios) e existia um erro só para isso (`wrong-password`).

## Decisão

- O cadastro pede só nome, e-mail e CPF. Usuários `PENDING` não têm senha.
- A senha é **definida** na ativação, por quem tem o link enviado ao e-mail. A ativação nunca confere senha; `wrong-password` deixa de existir.
- O banco garante o invariante: `CHECK (status = 'PENDING' OR password_hash IS NOT NULL)`. Cadastros pendentes que já tinham senha a perdem na migration.
- Recadastro de e-mail `PENDING` continua substituindo nome e CPF e invalidando os links anteriores. Para que o dono do e-mail perceba uma troca feita por outra pessoa, o e-mail de confirmação mostra o nome e o final do CPF, e a tela de ativação mostra um resumo mascarado (`POST /auth/confirm/summary`, token no corpo) antes de pedir a senha.

## Consequências

- O acesso à conta fica protegido sem nenhuma rota além do login conferindo senha.
- A integridade dos dados (nome e CPF) depende de o dono conferir o resumo. A alternativa sem essa dependência, um registro por envio de cadastro associado ao próprio token, fica documentada no [refactor](../backlog/refactor/2026-10-30-senha-na-ativacao.md) caso o risco se mostre relevante.
- O RF-01 do PRD mudou: a senha saiu do cadastro.
- A máscara de nome (`shared.privacy.NameMasker`) foi antecipada do RF-17 e passou para `shared`, porque `auth` e `account` a usam.
