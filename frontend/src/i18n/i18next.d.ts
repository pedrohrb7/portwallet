import 'i18next'
import type { Catalog } from './locales/pt-BR'

// Typed keys: `t()` and `<Trans i18nKey>` only accept keys that exist in the source catalog.
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation'
    resources: { translation: Catalog }
  }
}
