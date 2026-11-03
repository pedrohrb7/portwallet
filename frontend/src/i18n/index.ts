import i18next, { type InitOptions } from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import {
  defaultLanguage,
  languageCodes,
  languageStorageKey,
  matchLanguage,
  type Language,
} from './languages'
import { en } from './locales/en'
import { es } from './locales/es'
import { fr } from './locales/fr'
import { ptBR } from './locales/pt-BR'

/** Shared by the app instance and by tests that need a fresh detection. */
export const i18nOptions: InitOptions = {
  resources: {
    'pt-BR': { translation: ptBR },
    en: { translation: en },
    es: { translation: es },
    fr: { translation: fr },
  },
  supportedLngs: [...languageCodes],
  fallbackLng: defaultLanguage,
  load: 'currentOnly',
  // Every catalog is bundled, so initialization is synchronous and the first render is already translated.
  initAsync: false,
  // React already escapes what it renders.
  interpolation: { escapeValue: false },
  detection: {
    order: ['localStorage', 'navigator'],
    lookupLocalStorage: languageStorageKey,
    // Only an explicit choice in the switcher is saved (saveLanguage), never a detected language.
    caches: [],
    // `en-US` → `en`, `pt-PT` → `pt-BR`; an unsupported tag is kept as is, so i18next skips it.
    convertDetectedLanguage: (tag) => matchLanguage(tag) ?? tag,
  },
}

export const i18n = i18next.createInstance()

i18n.on('languageChanged', () => {
  document.documentElement.lang = currentLanguage()
})

void i18n.use(LanguageDetector).use(initReactI18next).init(i18nOptions)

/** The language in use, always one of the supported ones. */
export function currentLanguage(): Language {
  return matchLanguage(i18n.resolvedLanguage) ?? defaultLanguage
}

/** Switches the language and remembers the choice. Without storage, the choice lasts until the page is closed. */
export async function chooseLanguage(language: Language): Promise<void> {
  await i18n.changeLanguage(language)
  try {
    localStorage.setItem(languageStorageKey, language)
  } catch {
    // Storage blocked or full: the language still changes for this visit.
  }
}
