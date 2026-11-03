# Spec: Internationalization (i18n)

- **Status:** Done
- **Phase:** 2
- **PRD:** Cross-cutting (UI, API errors, e-mails)
- **Depends on:** [nfr-application-base](../nfr-application-base/spec.md), [FR-01](../fr-01-registration/spec.md)
- **Plan:** [plan.md](plan.md)
- **ADR:** [0007](../../adr/0007-multilingual-product.md)

## Goal

Let people use PortWallet in Brazilian Portuguese, English, Spanish or French. That covers everything a user can see: the interface, the API error titles and messages, and the e-mails.

## Scope

**Includes**

- Languages: `pt-BR` (default and fallback), `en`, `es`, `fr`.
- Frontend: every UI text through react-i18next, detection of the browser language, a language switcher on every page, the choice saved in the browser, `<html lang>`, and number and date formats that follow the language.
- API: Problem Details `title`, field messages in `errors` and other user-facing texts in responses (the registration message) follow `Accept-Language`.
- E-mails: written in the language of the person who receives them, saved on the user at registration.

**Later**

- Translating the OpenAPI documentation: it is developer documentation and stays in Portuguese (see Decisions).
- A language preference in the user's profile, editable after login: comes with the profile screen, if one is ever built. Until then the language saved at registration is used for e-mails.

## Behavior

1. The supported languages are `pt-BR`, `en`, `es` and `fr`. `pt-BR` is the default and the fallback for any text or language that is missing.
2. **First visit:** the frontend picks the first supported language from the browser's preferred languages (`navigator.languages`). A regional variant counts for its language (`en-US` and `en-GB` → `en`, `es-AR` → `es`, `fr-CA` → `fr`, any `pt-*` → `pt-BR`). With no match, `pt-BR`.
3. **Switcher:** every page has a language selector in the footer, labeled in the current language, with each language written in its own name (Português (Brasil), English, Español, Français). Changing it updates the whole page at once, without reloading, and keeps what was typed in the forms.
4. The choice is saved in `localStorage` (`portwallet.language`) and wins over the browser language on later visits. It is a preference, not a secret; when storage is unavailable the app still works and only forgets the choice.
5. `<html lang>` always holds the current language.
6. Numbers, money and dates follow the current language. Money is always BRL and dates are always shown in `America/Sao_Paulo`, whatever the language.
7. Validation messages of the forms follow the current language, including errors already on screen when the language changes. Messages that came from the API are shown as received.
8. Every request the frontend makes carries `Accept-Language` with the current language.
9. **API:** the language of a response comes from `Accept-Language`: exact match first, then the language without region (`en-US` → `en`), then `pt-BR`. Without the header, `pt-BR`. `pt-PT` answers in `pt-BR`.
10. The Problem Details `title` depends only on `type` and the language. `type`, `status` and the other fields never change with the language.
11. The messages in `errors[]` of `validation-error` follow the language, including Bean Validation constraints, custom constraints (`@ValidCpf`, `@MaxUtf8Bytes`) and the errors raised by the framework (malformed JSON, missing parameter, wrong type).
12. 401 and 403 written by Spring Security follow the same rule as the other errors.
13. The response of `POST /api/v1/auth/register` follows the language. The message is still the same in every case for a given language, so it reveals nothing about the account (FR-01).
14. **E-mails:** registration saves the language of the request on the user. Registering a `PENDING` e-mail again replaces the language too (latest submission wins, as with name and CPF). Every e-mail to a user is written in the user's saved language, never in the language of whoever triggered it: the "registration attempt" warning to an existing owner uses the owner's language.
15. Users created before this feature have `pt-BR`.

## API

No new endpoint. Every endpoint reads `Accept-Language`.

```http
POST /api/v1/auth/confirm/summary
Accept-Language: en
```

Response `400`:

```json
{ "type": "invalid-token", "title": "Invalid or expired link", "status": 400, "instance": "/api/v1/auth/confirm/summary" }
```

## Data model

`V6__user_language.sql`: column `users.language VARCHAR(5) NOT NULL DEFAULT 'PT_BR'` with `CHECK (language IN ('PT_BR', 'EN', 'ES', 'FR'))`, the same style as `role` and `status`. Existing rows get `PT_BR`.

## Backend

- `shared.i18n.Language`: enum of the supported languages, with the BCP 47 tag, the `Locale` and `Language.of(Locale)` (rule 9 applied to a resolved locale).
- `shared.i18n.I18nConfig`: `LocaleResolver` (`AcceptHeaderLocaleResolver` with the supported locales and `pt-BR` as default).
- `src/main/resources/messages.properties` (pt-BR, the base bundle), `messages_en.properties`, `messages_es.properties`, `messages_fr.properties`. `spring.messages.fallback-to-system-locale=false`, so a missing key falls back to pt-BR and never to the server's locale.
- Key families: `problem.<type>.title`, `validation.*` (framework and custom constraint messages), `jakarta.validation.constraints.*.message` (the standard constraints, so the wording is ours and identical across languages), `auth.register.accepted`, `email.*`.
- Messages with arguments go through `MessageFormat`, so texts use the typographic apostrophe (`’`) and never `'`.
- `shared.error.ProblemTitleAdvice` (a `ResponseBodyAdvice`) fills the `title` of every `ProblemDetail` the controllers return, from its `type` and the request language. `ApiException` and `Problems` no longer carry titles. `ProblemSecurityHandlers` resolves the language with the same `LocaleResolver`.
- `GlobalExceptionHandler` reads its field messages from the bundles.
- `User.language`, set by `User.register` and `replacePendingRegistration`. `AuthEmails` reads subject and body from the bundles in the user's language.
- `AuthController.register` resolves the language from the request (`Locale` argument) and passes it in `RegisterCommand`.
- `@ApiProblem` no longer has `title`: `OpenApiConfig` reads it from the bundles in `pt-BR`, so the documented title is always the one the API sends by default.

## Frontend

- Libraries: `i18next`, `react-i18next`, `i18next-browser-languagedetector`.
- `src/i18n/languages.ts`: supported languages, native names and `matchLanguage(tag)` (rule 2).
- `src/i18n/locales/pt-BR.ts` is the source catalog. `en.ts`, `es.ts` and `fr.ts` are typed `Catalog = typeof ptBR`, so a missing or extra key fails the type check. Keys are typed for `t()` through `src/i18n/i18next.d.ts`.
- `src/i18n/index.ts`: initializes i18next synchronously with every catalog bundled (they are small), the detector (`localStorage` then `navigator`, cached in `localStorage`) and keeps `<html lang>` in sync.
- `components/LanguageSwitcher.tsx`: native `<select>` (`data-testid="language-select"`), in the footer of `RootLayout`.
- Zod schemas return translation keys as messages; fields translate them on render (`useFieldError`). Errors set from the API are marked `type: 'server'` and shown as received.
- `api/problem.ts`: `problemKey(problem)` maps a `type` to `problem.<type>`, or `problem.fallback`.
- `api/client.ts`: every client (`api`, `authClient`, `rootClient` for `/actuator` and `/mock`) sends `Accept-Language`.
- `lib/money.ts`: `formatBRL(value, language)`.

## Security

- The language never changes what an error reveals: `type`, `status` and the generic registration message are equivalent in every language.
- `localStorage` holds only the language tag.

## Tests

| Layer | Scenario |
| --- | --- |
| Unit (frontend) | `matchLanguage` for exact, regional and unknown tags; catalogs have no empty text and use the same `{{placeholders}}` as pt-BR |
| Component | Switcher changes the texts, `<html lang>` and the saved choice; a saved choice wins over the browser language; validation errors on screen are re-translated; problem messages per language |
| Unit (frontend) | Every client sends `Accept-Language` with the current language |
| Integration (backend) | Bundles have the same keys in every language and no `'` in texts |
| Integration (backend) | Problem title per `Accept-Language` (en, es, fr, regional, unknown, none), field messages localized, 401 from the security filter localized |
| Integration (backend) | Registration message per language; confirmation e-mail in the request language; registration-attempt e-mail in the owner's language; language replaced by a new pending registration |
| E2E | Registration and activation in English end to end, e-mail included; the switcher persists across a reload |

## Acceptance criteria

- [x] No hard-coded user-facing text left in the frontend components or in backend code
- [x] All four catalogs complete (type check and tests)
- [x] Backend and frontend suites green, E2E green in Chromium and Firefox
- [x] Every screen reviewed in the four languages on desktop and mobile

## Decisions

- **react-i18next** with the catalogs as typed TypeScript objects instead of JSON: completeness is checked by the compiler, and `t()` only accepts existing keys.
- **All catalogs bundled**: four small catalogs cost less than an extra request and a loading state.
- **Schemas return keys, not texts**: the schemas stay module-level constants and errors already shown follow a language change.
- **Language saved on the user**: e-mails can be triggered by someone else (the registration-attempt warning) and, in later phases, by background jobs; neither has the recipient's request.
- **Title derived from `type`**: one source for every path (business errors, framework errors, security filter). The generic 4xx fallback, which used to say "Requisição inválida" under `validation-error`, now uses the `validation-error` title.
- **OpenAPI stays in Portuguese**: it is read by developers and changing it per request would make the document unstable for tools and caches. Its problem titles now come from the pt-BR bundle, so they cannot drift from what the API sends.

## Open questions

None.
