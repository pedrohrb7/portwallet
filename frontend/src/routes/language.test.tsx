import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authClient, rootClient } from '../api/client'
import { sessionStore } from '../features/auth/sessionStore'
import { languageStorageKey } from '../i18n/languages'
import { mockAdapter } from '../test/axiosMock'
import { renderRoutes } from '../test/renderRoutes'
import { routes } from './router'

describe('language switcher', () => {
  const originalRoot = rootClient.defaults.adapter
  const originalAuth = authClient.defaults.adapter

  beforeEach(() => {
    mockAdapter(rootClient, () => ({ status: 200, data: { status: 'UP' } }))
    mockAdapter(authClient, () => ({ status: 401, data: { type: 'unauthorized', status: 401 } }))
  })

  afterEach(() => {
    rootClient.defaults.adapter = originalRoot
    authClient.defaults.adapter = originalAuth
    sessionStore.reset()
  })

  it('is on every page, labeled in the current language, with each language in its own name', () => {
    renderRoutes(routes, '/nao-existe')

    const select = screen.getByRole('combobox', { name: 'Idioma' })
    expect(select).toHaveValue('pt-BR')
    expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual([
      'Português (Brasil)',
      'English',
      'Español',
      'Français',
    ])
  })

  it('translates the whole page at once and remembers the choice', async () => {
    const user = userEvent.setup()
    renderRoutes(routes, '/')
    expect(await screen.findByText('Online')).toBeInTheDocument()

    await user.selectOptions(screen.getByTestId('language-select'), 'en')

    expect(screen.getByTestId('demo-banner')).toHaveTextContent('Demo environment, no real money')
    expect(screen.getByRole('link', { name: 'Create account' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Language' })).toHaveValue('en')
    expect(document.documentElement.lang).toBe('en')
    expect(localStorage.getItem(languageStorageKey)).toBe('en')
  })

  it('keeps what was typed and translates the errors already on screen', async () => {
    const user = userEvent.setup()
    renderRoutes(routes, '/register')
    await user.type(screen.getByLabelText('E-mail'), 'ana@')
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))
    expect(await screen.findByText('Informe o nome completo')).toBeInTheDocument()
    expect(screen.getByText('E-mail inválido')).toBeInTheDocument()

    await user.selectOptions(screen.getByTestId('language-select'), 'es')

    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('ana@')
    expect(screen.getByText('Introduce tu nombre completo')).toBeInTheDocument()
    expect(screen.getByText('Correo electrónico no válido')).toBeInTheDocument()
    expect(screen.queryByText('Informe o nome completo')).not.toBeInTheDocument()
  })

  it('uses French typography', async () => {
    const user = userEvent.setup()
    renderRoutes(routes, '/register')

    await user.selectOptions(screen.getByTestId('language-select'), 'fr')

    expect(screen.getByRole('heading', { name: 'Créer un compte' })).toBeInTheDocument()
    const signIn = screen.getByRole('link', { name: 'Se connecter' })
    expect(signIn).toHaveAttribute('href', '/login')
    // Raw text: Testing Library's matchers would normalize the no-break space away.
    expect(signIn.parentElement?.textContent).toBe('Vous avez déjà un compte\u00a0? Se connecter')
  })
})
