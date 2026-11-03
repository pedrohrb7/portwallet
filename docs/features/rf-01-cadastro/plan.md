# Plano: RF-01 Cadastro e confirmação

- **Status:** Concluída
- **Spec:** [spec.md](spec.md)
- **Pré-requisitos:** [rnf-base-aplicacao](../rnf-base-aplicacao/plan.md)

## Passos

### 1. Migrations

- [x] `V2__users.sql`, `V3__accounts.sql`, `V4__mock_inbox.sql`
- **Arquivos:** `backend/src/main/resources/db/migration/`
- **Validação:** `./mvnw verify` (o `ddl-auto=validate` falha se entidade e schema divergirem)

### 2. Criptografia de campos

- [x] `CryptoProperties` validadas (chaves de 32 bytes), `FieldEncryptor`, `BlindIndex`, `EncryptedStringConverter`
- [x] Chaves de desenvolvimento nos perfis `dev`, `e2e` e `test`; variáveis no `docker-compose.yml`
- **Arquivos:** `shared/crypto/**`, `application.yml`, `src/test/resources/application-test.yml`, `docker-compose.yml`
- **Validação:** `CryptoTest`

### 3. Domínio

- [x] `Cpf`, `User`, `UserToken`, enums, repositórios
- **Arquivos:** `auth/domain/**`, `auth/infra/**`
- **Validação:** `CpfTest`

### 4. Notificação e caixa de entrada

- [x] `NotificationGateway`, `InboxNotificationGateway` (`dev`, `e2e`), `LoggingNotificationGateway` (demais perfis)
- [x] `InboxController` e templates dos e-mails (confirmação e aviso de tentativa de cadastro)
- **Arquivos:** `shared/notification/**`, `mock/inbox/**`
- **Validação:** `InboxControllerTest` (lista por destinatário; 404 com `portwallet.mock.inbox.enabled=false`)

### 5. Cadastro

- [x] `RegisterRequest` com Bean Validation e validador `@ValidCpf`
- [x] `RegistrationService` com os quatro casos e `AuthController.register`
- **Arquivos:** `auth/api/**`, `auth/application/RegistrationService.java`
- **Validação:** `RegistrationTest` com os quatro casos e corpo idêntico; CPF cifrado no banco

### 6. Confirmação

- [x] `AccountProvisioning` no módulo `account` (cria a conta `USER` com saldo zero e limite padrão)
- [x] `ConfirmationService` e `AuthController.confirm`
- **Arquivos:** `account/application/**`, `account/domain/Account.java`, `auth/application/ConfirmationService.java`
- **Validação:** `ConfirmationTest` (ativação e conta criada; token reutilizado, expirado e desconhecido)

### 7. Frontend

- [x] `schemas.ts` com validação de CPF
- [x] Páginas `/register`, `/confirm`, `/inbox` e `VITE_DEMO_INBOX` em `.env.development`
- **Arquivos:** `frontend/src/features/auth/**`, `frontend/src/features/inbox/**`, `frontend/src/routes/router.tsx`
- **Validação:** `cpf.test.ts`, `schemas.test.ts`, `RegisterPage.test.tsx`, `ConfirmPage.test.tsx`

### 8. E2E

- [x] Helpers em `e2e/support/`: `uniqueEmail()`, `validCpf()`, `linkFromInbox(request, email, subject)` lendo `/mock/inbox`
- [x] `e2e/register.spec.ts`: cadastro, confirmação pelo link da caixa de entrada
- **Validação:** `npm run e2e` verde em Chromium e Firefox

### 9. Correção de segurança: pré-sequestro de conta (substituída pelo passo 10)

Apontado por revisão de segurança automática depois do passo 8.

- [x] Recadastro de e-mail `PENDING` substitui nome, CPF e senha (`User.replacePendingRegistration`), exceto quando o CPF novo é de outro usuário
- [x] `/auth/confirm` exige a senha do cadastro mais recente (`WrongPasswordException`, 422 `wrong-password`), conferida antes de consumir o token
- [x] Tela `/confirm` com formulário de senha
- **Arquivos:** `auth/domain/User.java`, `auth/application/RegistrationService.java`, `auth/application/ConfirmationService.java`, `auth/application/WrongPasswordException.java`, `auth/api/AuthController.java`, `frontend/src/features/auth/ConfirmPage.tsx`, `frontend/src/api/**`, `docs/convencoes.md`
- **Validação:** `RegistrationTest` (atacante antes e depois da vítima; CPF de outro usuário), `ConfirmationTest` (senha errada mantém o link), `ConfirmPage.test.tsx`, cenário E2E "activation requires the password of the latest registration"

## Verificação final

- [x] `./mvnw verify` verde
- [x] `npm run lint && npm run typecheck && npm test` verdes
- [x] E2E verde em Chromium e Firefox
- [x] Telas revisadas em desktop e mobile
- [x] Perfil `demo` com caixa de entrada no `docker compose`, registrado nas decisões da spec
- [x] Spec atualizada com o que mudou na implementação

## Riscos

- Diferença entre o validador de CPF do frontend e o do backend: os dois usam a mesma lista de casos de teste.
