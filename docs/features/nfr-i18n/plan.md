# Plan: Internationalization (i18n)

- **Status:** Done
- **Spec:** [spec.md](spec.md)
- **Prerequisites:** [nfr-application-base](../nfr-application-base/plan.md), [FR-01](../fr-01-registration/plan.md)

## Steps

### 1. Backend language and bundles

- [x] `shared.i18n.Language` and `I18nConfig` (`AcceptHeaderLocaleResolver`)
- [x] `messages.properties` (pt-BR) and `messages_en`, `messages_es`, `messages_fr`; `spring.messages.fallback-to-system-locale=false`
- **Files:** `shared/i18n/*`, `src/main/resources/messages*.properties`, `application.yml`
- **Validation:** `LanguageTest`, `MessageBundlesTest`

### 2. Localized errors

- [x] Titles out of `ApiException` and `Problems`; `ProblemTitles` and `ProblemTitleAdvice`
- [x] `GlobalExceptionHandler` field messages and constraint messages (`@ValidCpf`, `@MaxUtf8Bytes`, standard constraints) from the bundles
- [x] `ProblemSecurityHandlers` with the same `LocaleResolver`
- [x] `@ApiProblem` without `title`; `OpenApiConfig` reads it from the pt-BR bundle
- **Files:** `shared/error/*`, `shared/security/*`, `shared/openapi/*`, `auth/api/ValidCpf.java`, `shared/validation/MaxUtf8Bytes.java`
- **Validation:** `LocalizedResponsesTest`, `ProblemDetailsTest`, `OpenApiDocsContract`

### 3. Language of the user and e-mails

- [x] `V6__user_language.sql`, `User.language`, `RegisterCommand.language`
- [x] `AuthController.register` resolves the language; `AuthEmails` from the bundles in the recipient's language
- **Files:** `auth/**`, `db/migration/V6__user_language.sql`
- **Validation:** `RegistrationTest`

### 4. Frontend i18n

- [x] i18next, react-i18next and the language detector; typed catalogs in `src/i18n`
- [x] Every component through `t()`/`<Trans>`; Zod messages as keys; `problemKey`
- [x] `LanguageSwitcher` in the `RootLayout` footer; `<html lang>`
- [x] `Accept-Language` on every client (`rootClient` added for `/actuator` and `/mock`); `formatBRL` and inbox dates per language
- **Files:** `frontend/src/**`
- **Validation:** `languages.test.ts`, `catalogs.test.ts`, `detection.test.ts`, `language.test.tsx`, `client.test.ts`, component tests

### 5. E2E

- [x] Playwright `locale: 'pt-BR'` for the existing suites; `e2e/i18n.spec.ts`
- **Validation:** `npm run e2e` in Chromium and Firefox

### 6. Documentation

- [x] ADR 0007, conventions, docs index, READMEs, FR-01 manual

## Final verification

- [x] `./mvnw verify` green
- [x] `npm run lint && npm run typecheck && npm test` green
- [x] E2E green in Chromium and Firefox
- [x] Screens reviewed in the four languages on desktop and mobile
- [x] Spec updated with what changed during implementation
- [x] READMEs (`README.md` and `README.pt-BR.md`) and the `docs/` index updated

## Risks

- Translations drift when a text changes in one language only: catalog and bundle tests check keys and placeholders, not meaning; reviews of user-facing changes look at all four languages.
