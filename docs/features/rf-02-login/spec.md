# Spec: RF-02 Login e refresh

- **Status:** Rascunho
- **Fase:** 2
- **PRD:** RF-02; Segurança; US-02
- **Depende de:** [rf-01-cadastro](../rf-01-cadastro/spec.md)
- **Plano:** [plan.md](plan.md)

## Objetivo

Autenticar o usuário com e-mail e senha e mantê-lo conectado com segurança: access token curto em memória e refresh token rotativo em cookie httpOnly.

## Escopo

**Inclui**

- `POST /auth/login` e `POST /auth/refresh`.
- Tabela `refresh_tokens` com família de tokens.
- Tela `/login` e restauração da sessão ao recarregar a página.

**Fica para depois**

- Revogar a família inteira quando um token antigo é reutilizado: fase 5. A coluna `family_id` já nasce aqui.
- CSRF double-submit (`XSRF-TOKEN`) no refresh: fase 5.
- Rate limit de login e contagem regressiva no 429: fase 5.
- BCrypt contra hash fictício para igualar o tempo de resposta: fase 5.
- Atalhos para contas demo na tela de login: RF-13.

## Comportamento

1. Login com credenciais válidas de usuário `ACTIVE` ou `BLOCKED` devolve 200 com o access token (JWT, 15 min) e define o cookie de refresh. Usuário bloqueado entra, mas não movimenta dinheiro (RF-05).
2. E-mail inexistente, senha errada ou usuário `PENDING`: 401 `invalid-credentials`, com a mesma mensagem, "E-mail ou senha inválidos".
3. O refresh token tem 32 bytes aleatórios em Base64URL e vale 7 dias. Só o hash SHA-256 fica no banco. Cada login inicia uma nova família (`family_id`).
4. Cookie: nome `refresh_token`, `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`, `Max-Age` de 7 dias.
5. `POST /auth/refresh` com um token válido e não revogado revoga esse token, emite um novo na mesma família (com a validade original da família, sem estendê-la) e devolve um novo access token. A revogação e a emissão acontecem na mesma transação.
6. Refresh sem cookie, com token desconhecido, expirado ou revogado: 401 `invalid-refresh-token`, e o cookie é apagado (`Max-Age=0`).
7. Refresh de usuário que não está mais `ACTIVE` nem `BLOCKED`: 401 `invalid-refresh-token`.
8. O frontend chama `/auth/refresh` ao carregar; com sucesso, a sessão é restaurada sem pedir senha. Várias chamadas que recebem 401 ao mesmo tempo disparam um único refresh (ver [rnf-base-aplicacao](../rnf-base-aplicacao/spec.md)).
9. Depois do login, o usuário volta para a rota que tentou acessar, ou para `/`.

## API

### `POST /api/v1/auth/login`

- **Acesso:** Público

```json
{ "email": "ana@demo.com", "password": "s3nh@forte" }
```

Resposta `200` (mais o header `Set-Cookie: refresh_token=...`):

```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresIn": 900,
  "user": { "id": "1b7e...", "name": "Ana Souza", "email": "ana@demo.com", "role": "USER" }
}
```

| Status | `type` | Quando |
| --- | --- | --- |
| 400 | `validation-error` | Campos ausentes ou malformados |
| 401 | `invalid-credentials` | Regra 2 |

### `POST /api/v1/auth/refresh`

- **Acesso:** Cookie `refresh_token`

Resposta `200`: mesmo corpo do login, com um novo cookie.

| Status | `type` | Quando |
| --- | --- | --- |
| 401 | `invalid-refresh-token` | Regras 6 e 7 |

## Modelo de dados

`V5__refresh_tokens.sql`

```sql
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    family_id   UUID        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
```

## Backend

- `auth.domain.RefreshToken` e repositório.
- `auth.application.LoginService`: verifica a senha com `PasswordEncoder` e emite o par de tokens.
- `auth.application.RefreshTokenService`: emissão, rotação e revogação.
- `auth.api.RefreshCookie`: monta e apaga o cookie com `ResponseCookie`.
- `/auth/login` e `/auth/refresh` liberados na `SecurityFilterChain`.

## Frontend

- `/login`: e-mail e senha, erro genérico em `role="alert"` (`data-testid="login-error"`), botão desabilitado durante o envio, link para `/register`.
- `session.ts`: `login()`, `restore()` (refresh na carga) e estado do usuário atual (`name`, `role`).
- Cabeçalho do `AppLayout` mostra o nome do usuário.

## Segurança

- O refresh token nunca é legível pelo JavaScript nem aparece em corpo de resposta.
- Mensagem idêntica para todas as falhas de credencial.
- O BCrypt custo 12 vale para todas as senhas.

## Testes

| Camada | Cenário |
| --- | --- |
| Integração | Login válido devolve JWT verificável e cookie com os atributos da regra 4; e-mail inexistente, senha errada e usuário `PENDING` devolvem o mesmo corpo; refresh rotaciona (o token antigo deixa de funcionar); refresh com token expirado ou revogado apaga o cookie; refresh preserva a validade da família |
| Unitário (frontend) | `restore()` com 401 deixa a sessão `anonymous`; redirecionamento para a rota de origem |
| E2E | Cenário 1 do PRD (parte de login); recarregar a página mantém o usuário conectado |

## Critérios de aceite

- [ ] Access token só em memória; refresh só em cookie httpOnly
- [ ] Refresh rotacionado a cada uso
- [ ] Sessão restaurada ao recarregar a página

## Decisões

- **Validade da família fixa:** a rotação não estende a sessão além de 7 dias desde o login.
- **`Path=/api/v1/auth` no cookie:** o navegador só envia o refresh token aos endpoints que precisam dele.

## Questões em aberto

- Nenhuma.
