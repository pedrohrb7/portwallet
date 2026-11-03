import i18next from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { chooseLanguage, i18n, i18nOptions } from '.'
import { languageStorageKey } from './languages'

/** Language a first page load would pick, with the given browser preferences. */
async function detect(browserLanguages: string[]): Promise<string | undefined> {
  vi.spyOn(navigator, 'languages', 'get').mockReturnValue(browserLanguages)
  vi.spyOn(navigator, 'language', 'get').mockReturnValue(browserLanguages[0] ?? '')
  const instance = i18next.createInstance().use(LanguageDetector)
  await instance.init(i18nOptions)
  return instance.resolvedLanguage
}

describe('language detection', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('uses the first supported browser language, regional variants included', async () => {
    expect(await detect(['fr-CA', 'en'])).toBe('fr')
    expect(await detect(['de-DE', 'es-AR', 'en'])).toBe('es')
    expect(await detect(['en-GB'])).toBe('en')
    expect(await detect(['pt-PT'])).toBe('pt-BR')
  })

  it('falls back to pt-BR when no browser language is supported', async () => {
    expect(await detect(['de-DE', 'ja'])).toBe('pt-BR')
    expect(await detect([])).toBe('pt-BR')
  })

  it('prefers the language chosen earlier over the browser language', async () => {
    localStorage.setItem(languageStorageKey, 'es')
    expect(await detect(['fr-FR'])).toBe('es')
  })

  it('does not save a detected language, only an explicit choice', async () => {
    await detect(['fr-FR'])
    expect(localStorage.getItem(languageStorageKey)).toBeNull()

    await chooseLanguage('en')
    expect(localStorage.getItem(languageStorageKey)).toBe('en')
    expect(i18n.resolvedLanguage).toBe('en')
  })

  it('still switches the language when storage is unavailable', async () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError')
    })

    await chooseLanguage('fr')

    expect(i18n.resolvedLanguage).toBe('fr')
    expect(document.documentElement.lang).toBe('fr')
  })
})
