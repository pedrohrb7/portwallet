# Plano: RF-01 Cadastro e confirmação

- **Status:** Aprovada
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rnf-base-aplicacao](../rnf-base-aplicacao/plan.md)

## Passos

### 1. Migrations

- [ ] `V2__users.sql`, `V3__accounts.sql`, `V4__mock_inbox.sql`
- **Arquivos:** `backend/src/main/resources/db/migration/`
- **Validação:** `./mvnw verify` (o `ddl-auto=validate` falha se entidade e schema divergirem)

### 2. Criptografia de campos

- [ ] `CryptoProperties` validadas (chaves de 32 bytes), `FieldEncryptor`, `BlindIndex`, `EncryptedStringConverter`
- [ ] Chaves de desenvolvimento nos perfis `dev`, `e2e` e `test`; variáveis no `docker-compose.yml`
- **Arquivos:** `auth/infra/crypto/**`, `application.yml`, `docker-compose.yml`
- **Validação:** `FieldEncryptorTest`, `BlindIndexTest`

### 3. Domínio

- [ ] `Cpf`, `User`, `UserToken`, enums, repositórios
- **Arquivos:** `auth/domain/**`, `auth/infra/**`
- **Validação:** `CpfTest`

### 4. Notificação e caixa de entrada

- [ ] `NotificationGateway`, `InboxNotificationGateway` (`dev`, `e2e`), `LoggingNotificationGateway` (demais perfis)
- [ ] `InboxController` e templates dos e-mails (confirmação e aviso de tentativa de cadastro)
- **Arquivos:** `shared/notification/**`, `mock/inbox/**`
- **Validação:** `InboxControllerTest` (lista por destinatário, 404 fora de `dev` e `e2e`)

### 5. Cadastro

- [ ] `RegisterRequest` com Bean Validation e validador `@ValidCpf`
- [ ] `RegistrationService` com os quatro casos e `AuthController.register`
- **Arquivos:** `auth/api/**`, `auth/application/RegistrationService.java`
- **Validação:** `RegistrationTest` com os quatro casos e corpo idêntico; CPF cifrado no banco

### 6. Confirmação

- [ ] `AccountProvisioning` no módulo `account` (cria a conta `USER` com saldo zero e limite padrão)
- [ ] `ConfirmationService` e `AuthController.confirm`
- **Arquivos:** `account/application/**`, `account/domain/Account.java`, `auth/application/ConfirmationService.java`
- **Validação:** `ConfirmationTest` (ativação e conta criada; token reutilizado, expirado e desconhecido)

### 7. Frontend

- [ ] `schemas.ts` com validação de CPF
- [ ] Páginas `/register`, `/confirm`, `/inbox` e `VITE_DEMO_INBOX` em `.env.development`
- **Arquivos:** `frontend/src/features/auth/**`, `frontend/src/features/inbox/**`, `frontend/src/routes/router.tsx`
- **Validação:** `schemas.test.ts`, `ConfirmPage.test.tsx`

### 8. E2E

- [ ] Helpers em `e2e/support/`: `uniqueEmail()`, `validCpf()`, `confirmationLink(email)` lendo `/mock/inbox`
- [ ] `e2e/register.spec.ts`: cadastro, confirmação pelo link da caixa de entrada
- **Validação:** `npm run e2e` verde em Chromium e Firefox

## Verificação final

- [ ] `./mvnw verify` verde
- [ ] `npm run lint && npm run typecheck && npm test` verdes
- [ ] E2E verde em Chromium e Firefox
- [ ] Telas revisadas em desktop e mobile
- [ ] Questão do perfil do `docker compose` resolvida e registrada
- [ ] Spec atualizada com o que mudou na implementação

## Riscos

- Diferença entre o validador de CPF do frontend e o do backend: os dois usam a mesma lista de casos de teste.
