import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach } from 'vitest'
import { i18n } from '../i18n'

// jsdom reports an English browser; tests run in the default language unless they switch it.
beforeEach(async () => {
  await i18n.changeLanguage('pt-BR')
  localStorage.clear()
})

afterEach(() => {
  cleanup()
})
