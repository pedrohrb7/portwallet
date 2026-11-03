/** Languages of the product (docs/features/nfr-i18n), each named in its own language for the switcher. */
export const languages = [
  { code: 'pt-BR', name: 'Português (Brasil)' },
  { code: 'en', name: 'English' },
  { code: 'es', name: 'Español' },
  { code: 'fr', name: 'Français' },
] as const

export type Language = (typeof languages)[number]['code']

export const defaultLanguage: Language = 'pt-BR'

export const languageCodes: readonly Language[] = languages.map((language) => language.code)

/** Where the language chosen in the switcher is kept. Only an explicit choice is saved, never a detected one. */
export const languageStorageKey = 'portwallet.language'

const primarySubtag = (tag: string) => tag.toLowerCase().split(/[-_]/)[0]

/**
 * The supported language for a BCP 47 tag: an exact match first, then the same language in any region
 * (`en-GB` → `en`, `pt-PT` → `pt-BR`). Undefined when the language is not supported.
 */
export function matchLanguage(tag: string | null | undefined): Language | undefined {
  if (!tag) {
    return undefined
  }
  const lower = tag.toLowerCase()
  return (
    languageCodes.find((code) => code.toLowerCase() === lower) ??
    languageCodes.find((code) => primarySubtag(code) === primarySubtag(tag))
  )
}
