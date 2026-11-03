import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { chooseLanguage, currentLanguage } from '../i18n'
import { languages, matchLanguage } from '../i18n/languages'

/** Language selector: each language is written in its own name, and the choice is remembered. */
export function LanguageSwitcher() {
  const { t } = useTranslation()
  const id = useId()

  return (
    <div className="flex items-center gap-2 text-sm text-slate-600">
      <svg
        aria-hidden="true"
        viewBox="0 0 20 20"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        className="size-4"
      >
        <circle cx="10" cy="10" r="7.25" />
        <path d="M2.75 10h14.5M10 2.75c2 2 3 4.5 3 7.25s-1 5.25-3 7.25c-2-2-3-4.5-3-7.25s1-5.25 3-7.25Z" />
      </svg>
      <label htmlFor={id}>{t('language.label')}</label>
      <select
        id={id}
        data-testid="language-select"
        value={currentLanguage()}
        onChange={(event) => {
          const language = matchLanguage(event.target.value)
          if (language) {
            void chooseLanguage(language)
          }
        }}
        className="rounded-md border border-slate-300 bg-white py-1 pr-7 pl-2 text-sm text-slate-900 shadow-sm outline-none focus-visible:border-slate-500 focus-visible:ring-2 focus-visible:ring-slate-900/20"
      >
        {languages.map((language) => (
          <option key={language.code} value={language.code} lang={language.code}>
            {language.name}
          </option>
        ))}
      </select>
    </div>
  )
}
