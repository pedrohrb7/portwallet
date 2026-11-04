import { screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authClient, rootClient } from '../api/client'
import { sessionStore } from '../features/auth/sessionStore'
import { mockAdapter } from '../test/axiosMock'
import { renderRoutes } from '../test/renderRoutes'
import { routes } from './router'

const user = { id: 'u-1', name: 'Ana Souza', email: 'ana@demo.com', role: 'USER' as const }

describe('app routes', () => {
  const originalAdapter = rootClient.defaults.adapter

  beforeEach(() => {
    mockAdapter(rootClient, () => ({ status: 200, data: { status: 'UP' } }))
    mockAdapter(authClient, () => ({ status: 401, data: { type: 'unauthorized', status: 401 } }))
  })

  afterEach(() => {
    rootClient.defaults.adapter = originalAdapter
    sessionStore.reset()
  })

  it('shows the demo banner and the API status on the home page', async () => {
    renderRoutes(routes, '/')
    expect(screen.getByTestId('demo-banner')).toHaveTextContent(
      'Ambiente de demonstração, sem dinheiro real',
    )
    expect(await screen.findByRole('heading', { name: 'PortWallet' })).toBeInTheDocument()
    expect(await screen.findByText('Online')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')
  })

  it('tries to restore the session on first load', async () => {
    renderRoutes(routes, '/')
    await screen.findByText('Online')
    expect(sessionStore.getState().status).toBe('anonymous')
  })

  it('waits for the restore before choosing the home page', async () => {
    renderRoutes(routes, '/')
    expect(screen.getByRole('status')).toHaveTextContent('Carregando...')
    expect(screen.queryByRole('heading', { name: 'PortWallet' })).not.toBeInTheDocument()
    // Waiting for the answer also keeps the pending restore from leaking into the next test.
    expect(await screen.findByRole('heading', { name: 'PortWallet' })).toBeInTheDocument()
  })

  it('shows the authenticated home with the name in the header when the session is restored', async () => {
    mockAdapter(authClient, () => ({
      status: 200,
      data: { accessToken: 't', expiresIn: 900, user },
    }))
    renderRoutes(routes, '/')

    expect(await screen.findByRole('heading', { name: 'Olá, Ana Souza' })).toBeInTheDocument()
    expect(screen.getByTestId('current-user')).toHaveTextContent('Ana Souza')
    expect(screen.queryByRole('link', { name: 'Criar conta' })).not.toBeInTheDocument()
  })

  it('shows the not found page for unknown routes, keeping the banner', () => {
    renderRoutes(routes, '/nao-existe')
    expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
    expect(screen.getByTestId('demo-banner')).toBeInTheDocument()
  })
})
