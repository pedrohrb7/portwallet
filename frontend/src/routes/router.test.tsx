import { screen } from '@testing-library/react'
import axios from 'axios'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authClient } from '../api/client'
import { sessionStore } from '../features/auth/sessionStore'
import { mockAdapter } from '../test/axiosMock'
import { renderRoutes } from '../test/renderRoutes'
import { routes } from './router'

describe('app routes', () => {
  const originalAdapter = axios.defaults.adapter

  beforeEach(() => {
    mockAdapter(axios, () => ({ status: 200, data: { status: 'UP' } }))
    mockAdapter(authClient, () => ({ status: 401, data: { type: 'unauthorized', status: 401 } }))
  })

  afterEach(() => {
    axios.defaults.adapter = originalAdapter
    sessionStore.reset()
  })

  it('shows the demo banner and the API status on the home page', async () => {
    renderRoutes(routes, '/')
    expect(screen.getByTestId('demo-banner')).toHaveTextContent(
      'Ambiente de demonstração, sem dinheiro real',
    )
    expect(screen.getByRole('heading', { name: 'PortWallet' })).toBeInTheDocument()
    expect(await screen.findByText('Online')).toBeInTheDocument()
  })

  it('tries to restore the session on first load', async () => {
    renderRoutes(routes, '/')
    await screen.findByText('Online')
    expect(sessionStore.getState().status).toBe('anonymous')
  })

  it('shows the not found page for unknown routes, keeping the banner', () => {
    renderRoutes(routes, '/nao-existe')
    expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
    expect(screen.getByTestId('demo-banner')).toBeInTheDocument()
  })
})
