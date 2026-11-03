# 7. Multilingual product

Date: 2026-11-03

## Status

Accepted. Amends [ADR 0006](0006-english-docs-portuguese-ui.md): the product is no longer Portuguese only. The documentation rules of ADR 0006 still apply.

## Context

ADR 0006 kept the product in Brazilian Portuguese, the language of its users. The project is also a portfolio for the international market, and a reviewer who does not read Portuguese could not use the demo. The product rules stay Brazilian: CPF, BRL amounts and the `America/Sao_Paulo` time zone.

## Decision

- The product speaks **Brazilian Portuguese (default and fallback), English, Spanish and French**. That covers the UI, the API texts a user can see (Problem Details `title`, field messages, the registration message) and the e-mails.
- **Frontend:** react-i18next, with typed catalogs in TypeScript (`src/i18n/locales`), so a missing key fails the type check. The language comes from an explicit choice in the switcher (saved in `localStorage`), then from the browser languages, then `pt-BR`. Every request sends it in `Accept-Language`.
- **Backend:** Spring `MessageSource` with `messages*.properties`, and the language of each request from `Accept-Language`. A Problem Details `title` is a function of its `type` and the language only; `type`, `status` and every other field stay the same in all languages, so clients keep choosing messages by `type`.
- **E-mails** use the language saved on the user at registration, not the language of whoever triggers them.
- CPF, BRL and `America/Sao_Paulo` do not change with the language: only how numbers and dates are written does.
- The OpenAPI document stays in Portuguese: it is developer documentation, not product text.

The details are in the [nfr-i18n spec](../features/nfr-i18n/spec.md).

## Consequences

- Every new user-facing text is a key in all four catalogs of the frontend, or in all four bundles of the backend. Tests fail when a language is missing a key or a placeholder.
- Tests assert texts in the default language unless they choose another one; the Playwright suites run with a `pt-BR` browser.
- Documentation keeps quoting UI texts in Portuguese, the default language.
- A fifth language is a new catalog, a new bundle, a value of `Language` and a migration of the `ck_users_language` constraint.
