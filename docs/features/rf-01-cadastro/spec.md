# Spec: RF-01 Cadastro e confirmação

- **Status:** Rascunho
- **Fase:** 2
- **PRD:** RF-01; Segurança avançada (anti-enumeração); US-01
- **Depende de:** [rnf-base-aplicacao](../rnf-base-aplicacao/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Permitir que uma pessoa crie conta com nome, e-mail, CPF fictício e senha, e a ative pelo link enviado por e-mail, sem que a resposta revele se o e-mail já estava cadastrado.

## Escopo

**Inclui**

- `POST /auth/register` e `POST /auth/confirm`.
- Tabelas `users`, `user_tokens` e `accounts`; a conta é criada na confirmação.
- CPF cifrado com AES-256-GCM e blind index HMAC-SHA256 desde já.
- Serviço de notificação simulado, chamado em processo, e caixa de entrada de demonstração (`/mock/inbox`) nos perfis `dev` e `e2e`.
- Telas `/register`, `/confirm` e `/inbox`.

**Fica para depois**

- Rate limit de cadastro (3 por hora por IP): fase 5.
- Igualar o tempo de resposta entre e-mail novo e existente: fase 5.
- Notificação por HTTP com circuit breaker e outbox: fase 4. A interface `NotificationGateway` criada aqui é o ponto de troca.
- Registro na auditoria: fase 5.

## Comportamento

1. Nome com 2 a 100 caracteres; e-mail válido, normalizado para minúsculas e sem espaços nas pontas; CPF com 11 dígitos (com ou sem máscara) e dígitos verificadores válidos; senha com 8 a 72 caracteres (72 é o limite do BCrypt).
2. Dados inválidos: 400 `validation-error`. A validação de formato não depende do banco, então não revela nada.
3. Com dados válidos, a resposta é **sempre** `202` com o mesmo corpo, em todos os casos abaixo:
   - **E-mail e CPF novos:** cria o usuário `PENDING`, com senha em BCrypt custo 12, e envia o e-mail de confirmação.
   - **E-mail de usuário `PENDING`:** não cria nada; invalida os tokens de confirmação anteriores e envia um novo link.
   - **E-mail de usuário `ACTIVE` ou `BLOCKED`:** não cria nada; envia ao dono um aviso de que alguém tentou cadastrar o e-mail dele.
   - **CPF já usado por outro e-mail:** não cria nada e não envia nada ao e-mail informado; envia ao dono do CPF o mesmo aviso de tentativa de cadastro.
4. O token de confirmação tem 32 bytes aleatórios em Base64URL, vale 24 h e só é usado uma vez. Só o hash SHA-256 fica no banco.
5. O link de confirmação é `<APP_BASE_URL>/confirm#token=<token>`. O token vai no fragmento (`#`), que não é enviado ao servidor nem gravado em logs de acesso.
6. `POST /auth/confirm` com token válido ativa o usuário, marca o token como usado e cria a conta com saldo 0,00 e limite diário de R$ 5.000,00, tudo na mesma transação. Resposta 200.
7. Token desconhecido, expirado ou já usado: 400 `invalid-token`, com a mesma mensagem nos três casos.
8. A tela `/confirm` lê o token do fragmento, remove-o da URL com `history.replaceState` antes de chamar a API e mostra sucesso (com link para o login) ou erro.
9. A tela `/register` valida em tempo real com Zod (mesmas regras do backend) e, ao enviar, sempre mostra a mesma mensagem: "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta."

## API

### `POST /api/v1/auth/register`

- **Acesso:** Público

```json
{ "name": "Ana Souza", "email": "ana@demo.com", "document": "529.982.247-25", "password": "s3nh@forte" }
```

Resposta `202`:

```json
{ "message": "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta." }
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Formato inválido |

### `POST /api/v1/auth/confirm`

- **Acesso:** Público

```json
{ "token": "q2Vx..." }
```

Resposta `200`:

```json
{ "status": "ACTIVE" }
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Token ausente |
| 400 | `invalid-token` | Token desconhecido, expirado ou já usado |

### `GET /mock/inbox`

- **Acesso:** Público, só nos perfis `dev` e `e2e` (o bean nem existe nos outros perfis)
- **Query:** `to` (e-mail, obrigatório)

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

A conta de sistema `EXTERNAL_FUNDS` e o uso de `balance` e `version` são definidos em [rnf-ledger-concorrencia](../rnf-ledger-concorrencia/spec.md).

`V4__mock_inbox.sql`

```sql
CREATE TABLE mock_inbox_messages (
    id          UUID          PRIMARY KEY,
    recipient   VARCHAR(254)  NOT NULL,
    subject     VARCHAR(200)  NOT NULL,
    body        VARCHAR(4000) NOT NULL,
    link        VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_mock_inbox_recipient ON mock_inbox_messages (recipient, created_at DESC);
```

A caixa de entrada fica no banco, e não em memória, para continuar funcionando com várias instâncias na fase 7.

## Backend

- `auth.domain`: `User`, `UserStatus`, `Role`, `UserToken`, `TokenPurpose`, `Cpf` (valor com normalização e validação dos dígitos).
- `auth.application.RegistrationService`: regras 3 e 4.
- `auth.application.ConfirmationService`: regras 6 e 7; cria a conta por `account.application.AccountProvisioning`.
- `auth.infra.crypto.FieldEncryptor` (AES-256-GCM, IV aleatório de 12 bytes, saída `Base64(iv || ciphertext || tag)`) e `BlindIndex` (HMAC-SHA256 em hex). Chaves em `PORTWALLET_CRYPTO_ENCRYPTION_KEY` e `PORTWALLET_CRYPTO_BLIND_INDEX_KEY` (32 bytes em Base64), com valores de desenvolvimento nos perfis `dev`, `e2e` e `test`.
- `auth.infra.crypto.EncryptedStringConverter`: `AttributeConverter` usado em `document_enc` (e em `totp_secret_enc` na fase 6).
- `shared.notification.NotificationGateway` com `send(Email)`. Implementação desta fase: `mock.inbox.InboxNotificationGateway`, que grava em `mock_inbox_messages`. Fora dos perfis `dev` e `e2e`, uma implementação que só registra no log o assunto e o destinatário mascarado.
- `mock.inbox.InboxController` com `@Profile({"dev", "e2e"})`.
- `portwallet.app-base-url` (padrão `http://localhost:5173`) para montar os links.

## Frontend

- `/register`: formulário com `name`, `email`, `document` (máscara de CPF), `password`; erros de campo com `aria-describedby`; botão desabilitado durante o envio; `data-testid="register-success"` na mensagem final.
- `/confirm`: estados `confirming`, `success` (`data-testid="confirm-success"`) e `error` (`data-testid="confirm-error"`).
- `/inbox`: campo de e-mail e lista de mensagens com assunto, data e link clicável; só aparece no menu quando `import.meta.env.VITE_DEMO_INBOX === 'true'`.
- `src/features/auth/schemas.ts`: esquemas Zod compartilhados com o login, incluindo a validação de CPF.

## Segurança

- A resposta do cadastro não muda com o estado do e-mail ou do CPF (regra 3).
- Logs nunca mostram senha, token nem CPF; e-mail aparece mascarado (`a***@demo.com`).
- O CPF em claro só existe em memória durante a requisição.

## Testes

| Camada | Cenário |
| --- | --- |
| Unitário | `Cpf` aceita com e sem máscara e rejeita dígitos errados e sequências repetidas; `FieldEncryptor` gera cifras diferentes para o mesmo texto e decifra de volta; `BlindIndex` é determinístico |
| Integração | Os quatro casos da regra 3 devolvem corpo idêntico, e cada um gera exatamente o e-mail esperado na caixa de entrada; `document_enc` no banco não contém os dígitos do CPF; confirmação cria a conta com saldo 0,00; token reutilizado, expirado e desconhecido dão `invalid-token`; `/mock/inbox` responde 404 no perfil padrão |
| Unitário (frontend) | Esquema Zod de cadastro; `/confirm` remove o token da URL antes da chamada |
| E2E | Cenário 1 do PRD (parte de cadastro e confirmação): cadastro, link pela caixa de entrada demo, confirmação |

## Critérios de aceite

- [ ] Resposta de cadastro idêntica em todos os casos
- [ ] CPF cifrado e único pelo blind index
- [ ] Conta criada só na confirmação
- [ ] Fluxo completo pela caixa de entrada demo no E2E

## Decisões

- **Criptografia do CPF antecipada da fase 6 para cá:** a coluna `document_hash` é necessária para a unicidade e para a busca de destinatário (RF-17) desta fase, e gravar o CPF em claro agora exigiria migrar dados depois.
- **Conta criada na confirmação:** usuários `PENDING` não aparecem em buscas nem têm saldo, o que simplifica as regras de destino.
- **Token no fragmento da URL:** não aparece em logs de servidor nem no header `Referer`.

## Questões em aberto

- No `docker compose`, o perfil padrão não tem caixa de entrada, então quem avalia não consegue confirmar um cadastro até existirem os dados demo (RF-13). Opções: rodar o compose com o perfil `dev`, criar um perfil `demo` com caixa de entrada e seed, ou antecipar o RF-13 para a Fase 2.
