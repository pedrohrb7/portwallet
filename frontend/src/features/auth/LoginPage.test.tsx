import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authClient } from '../../api/client'
import { mockAdapter } from '../../test/axiosMock'
import { renderRoutes } from '../../test/renderRoutes'
import { LoginPage } from './LoginPage'
import { sessionStore } from './sessionStore'

const user = { id: 'u-1', name: 'Ana Souza', email: 'ana@demo.com', role: 'USER' as const }

function Landing() {
  const location = useLocation()
  return <p>landed on {location.pathname + location.search}</p>
}

function renderLogin(from?: string) {
  const routes = [
    { path: '/login', element: <LoginPage /> },
    { path: '*', element: <Landing /> },
  ]
  return renderRoutes(routes, { pathname: '/login', state: from ? { from } : null })
}

describe('LoginPage', () => {
  let requests: unknown[]
  let respond: () => { status: number; data: unknown } | undefined
  const originalAdapter = authClient.defaults.adapter

  beforeEach(() => {
    requests = []
    respond = () => ({ status: 200, data: { accessToken: 't', expiresIn: 900, user } })
    sessionStore.clear()
    mockAdapter(authClient, (config) => {
      requests.push(JSON.parse(config.data as string))
      return respond()
    })
  })

  afterEach(() => {
    authClient.defaults.adapter = originalAdapter
    sessionStore.reset()
  })

  async function signIn(email = 'Ana@Demo.com ', password = 's3nh@forte') {
    const userEvents = userEvent.setup()
    await userEvents.type(screen.getByLabelText('E-mail'), email)
    await userEvents.type(screen.getByLabelText('Senha'), password)
    await userEvents.click(screen.getByRole('button', { name: 'Entrar' }))
    return userEvents
  }

  it('sends the normalized e-mail with the password and opens the session', async () => {
    renderLogin()
    await signIn()

    expect(await screen.findByText('landed on /')).toBeInTheDocument()
    expect(requests).toEqual([{ email: 'ana@demo.com', password: 's3nh@forte' }])
    expect(sessionStore.getState()).toEqual({ status: 'authenticated', user })
  })

  it('returns to the page the user was sent away from', async () => {
    renderLogin('/statement?type=TRANSFER')
    await signIn()

    expect(await screen.findByText('landed on /statement?type=TRANSFER')).toBeInTheDocument()
  })

  it('never redirects outside the app', async () => {
    renderLogin('//evil.example/phish')
    await signIn()

    expect(await screen.findByText('landed on /')).toBeInTheDocument()
  })

  it('shows the generic message for wrong credentials, keeping the e-mail and clearing the password', async () => {
    respond = () => ({
      status: 401,
      data: { type: 'invalid-credentials', title: 'E-mail ou senha inválidos', status: 401 },
    })
    renderLogin()
    await signIn('ana@demo.com', 'senha-errada')

    expect(await screen.findByTestId('login-error')).toHaveTextContent('E-mail ou senha inválidos.')
    expect(screen.getByRole('alert')).toBe(screen.getByTestId('login-error'))
    expect(screen.getByLabelText('E-mail')).toHaveValue('ana@demo.com')
    expect(screen.getByLabelText('Senha')).toHaveValue('')
    expect(screen.getByLabelText('Senha')).toHaveFocus()
    expect(sessionStore.getState().status).toBe('anonymous')
  })

  it('validates the fields before sending', async () => {
    const userEvents = userEvent.setup()
    renderLogin()
    await userEvents.type(screen.getByLabelText('E-mail'), 'not-an-email')
    await userEvents.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('E-mail inválido')).toBeInTheDocument()
    expect(screen.getByText('Informe a senha')).toBeInTheDocument()
    expect(requests).toHaveLength(0)
  })

  it('disables the button while signing in', async () => {
    let release: () => void = () => {}
    authClient.defaults.adapter = (config) =>
      new Promise((resolve) => {
        release = () =>
          resolve({
            data: { accessToken: 't', expiresIn: 900, user },
            status: 200,
            statusText: 'OK',
            headers: {},
            config,
          })
      })
    renderLogin()
    await signIn()

    const button = screen.getByRole('button', { name: 'Entrando...' })
    expect(button).toBeDisabled()
    release()
    expect(await screen.findByText('landed on /')).toBeInTheDocument()
  })

  it('shows a generic error when the server is unreachable', async () => {
    respond = () => undefined
    renderLogin()
    await signIn()

    expect(await screen.findByTestId('login-error')).toHaveTextContent('Sem conexão com o servidor')
  })

  it('skips the form for a user who is already signed in', () => {
    sessionStore.authenticate({ accessToken: 't', expiresIn: 900, user })
    renderLogin()

    expect(screen.getByText('landed on /')).toBeInTheDocument()
  })

  it('links to the registration', () => {
    renderLogin()
    expect(screen.getByRole('link', { name: 'Criar conta' })).toHaveAttribute('href', '/register')
  })
})
