# Spec: RF-01 Cadastro e confirmação

- **Status:** Concluída
- **Fase:** 2
- **PRD:** RF-01; Segurança avançada (anti-enumeração); US-01
- **Depende de:** [rnf-base-aplicacao](../rnf-base-aplicacao/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Permitir que uma pessoa crie conta com nome, e-mail e CPF fictício, e a ative pelo link enviado por e-mail, definindo a senha nesse momento, sem que a resposta revele se o e-mail já estava cadastrado.

## Escopo

**Inclui**

- `POST /auth/register`, `POST /auth/confirm/summary` e `POST /auth/confirm`.
- Tabelas `users`, `user_tokens` e `accounts`; a conta é criada na confirmação.
- CPF cifrado com AES-256-GCM e blind index HMAC-SHA256 desde já.
- Serviço de notificação simulado, chamado em processo, e caixa de entrada de demonstração (`/mock/inbox`) nos perfis `dev`, `e2e` e `demo`.
- Telas `/register`, `/confirm` e `/inbox`.

**Fica para depois**

- Rate limit de cadastro (3 por hora por IP) e da ativação: fase 5.
- Igualar o tempo de resposta entre e-mail novo e existente: fase 5.
- Notificação por HTTP com circuit breaker e outbox: fase 4. A interface `NotificationGateway` criada aqui é o ponto de troca.
- Registro na auditoria: fase 5.

## Comportamento

1. Nome com 2 a 100 caracteres; e-mail válido, normalizado para minúsculas e sem espaços nas pontas; CPF com 11 dígitos (com ou sem máscara) e dígitos verificadores válidos. O cadastro **não** pede senha; um campo `password` enviado é ignorado.
2. Dados inválidos: 400 `validation-error`. A validação de formato não depende do banco, então não revela nada.
3. Com dados válidos, a resposta é **sempre** `202` com o mesmo corpo, em todos os casos abaixo:
   - **E-mail e CPF novos:** cria o usuário `PENDING`, sem senha, e envia o e-mail de confirmação.
   - **E-mail de usuário `PENDING`:** não cria outro usuário; **substitui** nome e CPF do cadastro pendente pelos dados novos, invalida os tokens de confirmação anteriores e envia um novo link. Se o CPF novo pertence a outro usuário, vale o caso do CPF repetido abaixo e o cadastro pendente não muda.
   - **E-mail de usuário `ACTIVE` ou `BLOCKED`:** não cria nada; envia ao dono um aviso de que alguém tentou cadastrar o e-mail dele.
   - **CPF já usado por outro usuário:** não cria nada e não envia nada ao e-mail informado; envia ao dono do CPF o mesmo aviso de tentativa de cadastro.
4. O token de confirmação tem 32 bytes aleatórios em Base64URL, vale 24 h e só é usado uma vez. Só o hash SHA-256 fica no banco.
5. O link de confirmação é `<APP_BASE_URL>/confirm#token=<token>`. O token vai no fragmento (`#`), que não é enviado ao servidor nem gravado em logs de acesso. O e-mail de confirmação mostra o nome completo e o CPF mascarado (`***.***.***-25`) do cadastro, e orienta a refazer o cadastro se os dados não forem do dono do e-mail.
6. Usuários `PENDING` não têm senha; todo usuário `ACTIVE` ou `BLOCKED` tem. O banco garante isso com `ck_users_password_when_active`.
7. `POST /auth/confirm/summary` recebe o token **no corpo** e, se ele é válido, devolve o nome mascarado (primeiro nome e inicial do último sobrenome, `shared.privacy.NameMasker`) e o CPF mascarado do cadastro. Não consome o token.
8. `POST /auth/confirm` recebe o token e a senha **nova** (8 a 72 caracteres, até 72 bytes em UTF-8, porque o BCrypt ignora o resto). Com token válido, grava a senha em BCrypt custo 12, ativa o usuário, marca o token como usado e cria a conta com saldo 0,00 e limite diário de R$ 5.000,00, tudo na mesma transação. Resposta 200. A ativação nunca confere senha.
9. Token desconhecido, expirado ou já usado, no resumo ou na ativação: 400 `invalid-token`, com a mesma mensagem nos três casos. Senha fora das regras: 400 `validation-error`, sem consumir o token.
10. A tela `/confirm` lê o token do fragmento e o remove da URL com `history.replaceState` ao abrir; busca o resumo e pergunta "Estes são os seus dados?", com link para refazer o cadastro; pede a senha e a repetição dela (conferida só no cliente); mostra sucesso (com link para o login) ou erro de link. Um novo link aberto na mesma aba (só o fragmento muda) recomeça o fluxo com o novo token.
11. A tela `/register` valida em tempo real com Zod (mesmas regras do backend) e, ao enviar, sempre mostra a mesma mensagem: "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta."

Juntas, as regras 3, 6 e 8 impedem o **pré-sequestro de conta**: só quem controla o e-mail escolhe a senha, então cadastrar primeiro o e-mail de outra pessoa não dá acesso à conta. A troca de nome e CPF por quem recadastra o e-mail entre o cadastro e o clique do dono aparece no e-mail e no resumo do link mais recente (regras 5 e 7), e o dono refaz o cadastro ([ADR 0005](../../adr/0005-senha-definida-na-ativacao.md)).

## API

### `POST /api/v1/auth/register`

- **Acesso:** Público

```json
{ "name": "Ana Souza", "email": "ana@demo.com", "document": "529.982.247-25" }
```

Resposta `202`:

```json
{ "message": "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta." }
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Formato inválido |

### `POST /api/v1/auth/confirm/summary`

- **Acesso:** Público

```json
{ "token": "q2Vx..." }
```

Resposta `200`:

```json
{ "maskedName": "Ana S****", "maskedDocument": "***.***.***-25" }
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Token ausente |
| 400 | `invalid-token` | Token desconhecido, expirado ou já usado |

### `POST /api/v1/auth/confirm`

- **Acesso:** Público

```json
{ "token": "q2Vx...", "password": "s3nh@forte" }
```

Resposta `200`:

```json
{ "status": "ACTIVE" }
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Token ou senha ausentes, senha com menos de 8 caracteres ou acima de 72 bytes |
| 400 | `invalid-token` | Token desconhecido, expirado ou já usado |

### `GET /mock/inbox`

- **Acesso:** Público, só onde `portwallet.mock.inbox.enabled=true` (perfis `dev`, `e2e`, `demo` e `test`). Nos demais, o controller nem existe e a rota responde 404 `not-found`.
- **Query:** `to` (e-mail válido, obrigatório; sem diferenciar maiúsculas de minúsculas)

```json
[
  {
    "id": "8d0c...",
    "to": "ana@demo.com",
    "subject": "Ative sua conta PortWallet",
    "body": "Olá, Ana! Para ativar sua conta, acesse: http://localhost:5173/confirm#token=q2Vx...",
    "link": "http://localhost:5173/confirm#token=q2Vx...",
    "createdAt": "2026-10-30T15:00:00Z"
  }
]
```

Do mais recente para o mais antigo. O campo `link` existe para os testes e para a tela não precisarem extrair a URL do texto.

## Modelo de dados

`V2__users.sql`

```sql
CREATE TABLE users (
    id                UUID         PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    email             VARCHAR(254) NOT NULL UNIQUE,
    password_hash     VARCHAR(60)  NOT NULL,
    document_enc      VARCHAR(255) NOT NULL,
    document_hash     VARCHAR(64)  NOT NULL UNIQUE,
    totp_secret_enc   VARCHAR(255),
    totp_enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    role              VARCHAR(10)  NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    status            VARCHAR(10)  NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED')),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    purpose     VARCHAR(20) NOT NULL CHECK (purpose IN ('EMAIL_CONFIRMATION', 'PASSWORD_RESET')),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_user_tokens_user_purpose ON user_tokens (user_id, purpose);
```

`V3__accounts.sql`

```sql
CREATE TABLE accounts (
    id           UUID          PRIMARY KEY,
    user_id      UUID          UNIQUE REFERENCES users (id),
    kind         VARCHAR(10)   NOT NULL CHECK (kind IN ('USER', 'SYSTEM')),
    balance      DECIMAL(19,2) NOT NULL,
    daily_limit  DECIMAL(19,2) NOT NULL,
    version      BIGINT        NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_accounts_owner CHECK ((kind = 'USER' AND user_id IS NOT NULL) OR (kind = 'SYSTEM' AND user_id IS NULL)),
    CONSTRAINT ck_accounts_balance CHECK (kind = 'SYSTEM' OR balance >= 0)
);
```

`V5__password_set_on_activation.sql`

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
UPDATE users SET password_hash = NULL WHERE status = 'PENDING';
ALTER TABLE users ADD CONSTRAINT ck_users_password_when_active
    CHECK (status = 'PENDING' OR password_hash IS NOT NULL);
```

A conta de sistema `EXTERNAL_FUNDS` e o uso de `balance` e `version` são definidos em [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/spec.md).

`V4__mock_inbox.sql`

```sql
CREATE TABLE mock_inbox_messages (
    id          UUID          PRIMARY KEY,
    seq         BIGINT        GENERATED ALWAYS AS IDENTITY,
    recipient   VARCHAR(254)  NOT NULL,
    subject     VARCHAR(200)  NOT NULL,
    body        VARCHAR(4000) NOT NULL,
    link        VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_mock_inbox_recipient ON mock_inbox_messages (recipient, seq DESC);
```

A caixa de entrada fica no banco, e não em memória, para continuar funcionando com várias instâncias na fase 7. A ordem usa a coluna de identidade `seq`, e não `created_at`: duas mensagens no mesmo instante teriam ordem indefinida.

As migrations usam constraints nomeadas (`uq_users_email`, `ck_accounts_balance` etc.), para que as mensagens de erro do banco sejam reconhecíveis.

## Backend

- `auth.domain`: `User`, `UserStatus`, `UserToken`, `TokenPurpose`, `Cpf` (valor com normalização e validação dos dígitos, que nunca imprime o número completo). `Role` fica em `shared.security`, criado na base da aplicação.
- `auth.application.RegistrationService`: regras 3 e 4. Roda num `TransactionTemplate`; se dois cadastros simultâneos disputam o mesmo e-mail ou CPF, a violação de unicidade do perdedor é absorvida e a resposta continua a mesma.
- `auth.application.ConfirmationService`: regras 6 a 9. `summary` devolve o resumo mascarado sem consumir o token; `confirm` consome o token, grava a senha com o `PasswordEncoder` (`User.activate(passwordHash)`) e cria a conta por `account.application.AccountProvisioning` (`Propagation.MANDATORY`, para nunca existir usuário ativo sem conta). O token é consumido com `UPDATE ... WHERE used_at IS NULL`, que garante o uso único mesmo com confirmações simultâneas.
- `auth.application.AuthEmails`: textos do e-mail de confirmação (com nome e CPF mascarado) e do aviso de tentativa de cadastro (um só aviso para e-mail e CPF repetidos).
- `auth.application.PasswordConfig`: `BCryptPasswordEncoder` com custo 12.
- `auth.api`: `AuthController`, `RegisterRequest` (normaliza nome, e-mail e CPF antes da validação), `@ValidCpf`. A senha da ativação (`ConfirmRequest`) também é limitada a 72 bytes em UTF-8 por `shared.validation.MaxUtf8Bytes`, porque o BCrypt ignora o que passa disso e um caractere acentuado ocupa dois bytes.
- `shared.crypto`: `FieldEncryptor` (AES-256-GCM, IV aleatório de 12 bytes, saída `Base64(iv || ciphertext || tag)`), `BlindIndex` (HMAC-SHA256 em hex), `EncryptedStringConverter` (usado em `document_enc` e, na fase 6, em `totp_secret_enc`) e `SecureTokens` (geração em Base64URL e hash SHA-256). Chaves em `PORTWALLET_CRYPTO_ENCRYPTION_KEY` e `PORTWALLET_CRYPTO_BLIND_INDEX_KEY` (32 bytes em Base64, diferentes entre si), validadas por `CryptoProperties`: a aplicação não sobe sem elas. Há valores de desenvolvimento nos perfis `dev`, `e2e` e `test`, e o `docker-compose.yml` passa valores de demonstração.
- `shared.privacy.NameMasker` (regra 7; também usado pelo RF-17) e `Cpf.masked()`.
- `shared.money.MoneyConverter`: `AttributeConverter` de `Money` para `DECIMAL(19,2)`, usado em `Account`.
- `shared.notification.NotificationGateway` com `send(Email)`. Com `portwallet.mock.inbox.enabled=true`, a implementação é `mock.inbox.InboxNotificationGateway`, que grava em `mock_inbox_messages` por `InboxStore`. Sem a propriedade, `LoggingNotificationGateway` só registra no log o assunto e o destinatário mascarado (`shared.logging.Masking`).
- `mock.inbox.InboxController`, condicionado à mesma propriedade.
- `portwallet.app-base-url` (`APP_BASE_URL`, padrão `http://localhost:5173`; no `docker compose`, `http://localhost:3000`) para montar os links.

## Frontend

- `/register` (`RegisterPage`): formulário com `name`, `email` e `document` (máscara de CPF aplicada ao digitar), sem senha; validação ao sair de cada campo e depois a cada mudança; erros de campo com `aria-invalid` e `aria-describedby`; erros de campo devolvidos pela API aparecem no campo; botão desabilitado durante o envio; `data-testid="register-success"` na mensagem final, com link para a caixa de entrada demo quando ela existe.
- `/confirm` (`ConfirmPage`): carrega o resumo (`data-testid="confirm-summary"`, com tentar de novo em falha de rede), formulário "Crie uma senha" e "Repita a senha" (`activationSchema`), sucesso (`data-testid="confirm-success"`) e erro de link (`data-testid="confirm-error"`, sem formulário). O token é lido no primeiro render (leitura pura, segura no Strict Mode), fica só em memória e sai da URL num effect ao abrir a página; um `hashchange` com novo token remonta o fluxo.
- `/inbox` (`InboxPage`): campo de e-mail (`data-testid="inbox-email"`), e-mail pesquisado refletido na URL (`/inbox?to=...`) e lista de mensagens (`data-testid="inbox-message"`) com assunto, data em São Paulo, corpo e link. A rota só existe quando `VITE_DEMO_INBOX=true` (`.env.development` e build do `docker compose`), e usa um layout mais largo e alinhado ao topo (`PublicWideLayout`).
- Tela inicial com links para criar conta e para a caixa de entrada demo.
- `src/features/auth/schemas.ts`: esquemas Zod de cadastro e de ativação (mesmas regras do backend, incluindo os 72 bytes da senha, e senhas iguais). `src/lib/cpf.ts`: validação e máscara de CPF, testadas com os mesmos casos do backend.
- Componentes compartilhados: `TextField`, `Button` (desabilitado durante o envio) e `Alert`.
- Proxy de `/mock` no Vite e no Nginx.

## Segurança

- A resposta do cadastro não muda com o estado do e-mail ou do CPF (regra 3).
- A senha trafega uma vez só, na ativação, e só é gravada depois que o dono do e-mail abre o link.
- O token do resumo vai no corpo da requisição, não na URL.
- Logs nunca mostram senha, token nem CPF; e-mail aparece mascarado (`a***@demo.com`).
- O CPF em claro só existe em memória durante a requisição.

## Testes

| Camada | Cenário |
| --- | --- |
| Unitário | `CpfTest`: com e sem máscara, dígitos errados, sequências repetidas, máscara parcial, CPFs gerados. `CryptoTest`: cifras diferentes para o mesmo texto, ida e volta, adulteração e chave errada rejeitadas, blind index determinístico e dependente da chave, tokens, validação das chaves e app que não sobe sem elas |
| Integração | `RegistrationTest`: os quatro casos da regra 3 com corpo idêntico e exatamente o e-mail esperado (com nome e CPF mascarado); usuário pendente sem senha, mesmo se uma for enviada; CPF cifrado; e-mail normalizado; link antigo invalidado ao reenviar; pré-sequestro (atacante cadastra antes da vítima e não ganha acesso, e o CPF dele é liberado; atacante recadastra depois e os dados dele aparecem no resumo do link mais recente até a vítima refazer o cadastro); cadastro pendente intacto quando o CPF novo é de outro usuário; validação de campos. `ConfirmationTest`: resumo mascarado sem consumir o link; resumo de link inválido ou usado; ativação grava a senha em BCrypt custo 12 e abre a conta de saldo 0,00 e limite 5.000,00; token reutilizado não troca a senha; token expirado e desconhecido dão `invalid-token`; senha curta ou acima de 72 bytes dá `validation-error` sem consumir o link; campos ausentes; o banco recusa usuário ativo sem senha. `InboxControllerTest`: lista por destinatário, mais recente primeiro, e 404 com a caixa desligada. `NameMaskerTest`, `CpfTest` (máscara) |
| Unitário (frontend) | `cpf.test.ts` (mesmos casos do backend), `schemas.test.ts` (cadastro e ativação), `RegisterPage.test.tsx` (máscara, dados normalizados, sem campo de senha, validação local, erro de campo da API, falha de rede), `ConfirmPage.test.tsx` em Strict Mode (token removido da URL ao abrir, resumo, ativação com a nova senha, senhas diferentes e senha curta sem chamar a API, link inválido no resumo e na ativação, novo link na mesma aba, nova tentativa após falha de rede, ausência de token) |
| E2E | `register.spec.ts`: cadastro pela interface sem senha, e-mail com os dados, link pela caixa de entrada demo, resumo mascarado e definição da senha, sem o token na barra de endereço; link usado duas vezes; recadastro troca o resumo do link mais recente e invalida o anterior; validação local do cadastro e da repetição da senha sem chamar a API |

## Critérios de aceite

- [x] Resposta de cadastro idêntica em todos os casos
- [x] CPF cifrado e único pelo blind index
- [x] Conta criada só na confirmação
- [x] Fluxo completo pela caixa de entrada demo no E2E

## Decisões

- **Criptografia do CPF antecipada da fase 6 para cá:** a coluna `document_hash` é necessária para a unicidade e para a busca de destinatário (RF-17) desta fase, e gravar o CPF em claro agora exigiria migrar dados depois.
- **Conta criada na confirmação:** usuários `PENDING` não aparecem em buscas nem têm saldo, o que simplifica as regras de destino.
- **Token no fragmento da URL:** não aparece em logs de servidor nem no header `Referer`.
- **Perfil `demo` com caixa de entrada:** o `docker compose` sobe no perfil `demo`, para quem avalia conseguir confirmar um cadastro. O PRD previa a caixa só em `dev` e `e2e`; a exceção vale apenas para a demonstração local.
- **Caixa de entrada ligada por propriedade, não por perfil:** `portwallet.mock.inbox.enabled` concentra a decisão num lugar só, e os testes conseguem desligá-la sem trocar de perfil.
- **Criptografia em `shared.crypto`, não em `auth.infra.crypto`:** a entidade `User`, em `domain`, precisa do conversor, e o ArchUnit proíbe `domain` de depender de `infra`. O pacote também será usado pelo TOTP na fase 6.
- **Um só aviso de tentativa de cadastro:** e-mail e CPF repetidos geram o mesmo texto, que não diz qual dado foi usado.
- **Senha na ativação contra pré-sequestro de conta (substituída):** a primeira correção exigia, na ativação, a senha do cadastro mais recente (422 `wrong-password`). Foi substituída pela senha definida na ativação, com resumo dos dados antes de ativar ([ADR 0005](../../adr/0005-senha-definida-na-ativacao.md)).

## Questões em aberto

- Nenhuma.
