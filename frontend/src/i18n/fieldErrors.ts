import type { ParseKeys } from 'i18next'
import type { FieldError } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { Catalog } from './locales/pt-BR'

/** Error type of the field errors returned by the API, whose message is already in the user's language. */
export const SERVER_ERROR = 'server'

export type ValidationKey = `validation.${keyof Catalog['validation']}`

/** A Zod message: a translation key, translated when the error is shown, so it follows language changes. */
export const validationMessage = (key: keyof Catalog['validation']): ValidationKey =>
  `validation.${key}`

/** Text of a field error: client errors carry a translation key, server errors the text itself. */
export function useFieldErrorText() {
  const { t } = useTranslation()
  return (error: FieldError | undefined): string | undefined => {
    if (!error?.message) {
      return undefined
    }
    return error.type === SERVER_ERROR ? error.message : t(error.message as ParseKeys)
  }
}
