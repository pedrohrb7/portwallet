import { act, screen } from '@testing-library/react'
import { useLocation } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import { sessionStore } from '../features/auth/sessionStore'
import { renderRoutes } from '../test/renderRoutes'
import { RequireAuth } from './RequireAuth'

function LoginProbe() {
  const location = useLocation()
  return <p>login page, from {(location.state as { from?: string } | null)?.from}</p>
}

const routes = [
  { path: '/login', element: <LoginProbe /> },
  { element: <RequireAuth />, children: [{ path: '/statement', element: <p>private page</p> }] },
]

const user = { id: 'u-1', name: 'Ana Souza', email: 'ana@demo.com', role: 'USER' as const }

describe('RequireAuth', () => {
  afterEach(() => {
    sessionStore.reset()
  })

  it('waits while the session is being restored', () => {
    renderRoutes(routes, '/statement')
    expect(screen.getByRole('status')).toHaveTextContent('Carregando...')
  })

  it('redirects anonymous users to /login, remembering where they were going', () => {
    sessionStore.clear()
    renderRoutes(routes, '/statement?type=TRANSFER')
    expect(screen.getByText('login page, from /statement?type=TRANSFER')).toBeInTheDocument()
  })

  it('renders the page for authenticated users', () => {
    sessionStore.authenticate({ accessToken: 't', expiresIn: 900, user })
    renderRoutes(routes, '/statement')
    expect(screen.getByText('private page')).toBeInTheDocument()
  })

  it('shows the page as soon as the restore finishes', () => {
    renderRoutes(routes, '/statement')
    act(() => sessionStore.authenticate({ accessToken: 't', expiresIn: 900, user }))
    expect(screen.getByText('private page')).toBeInTheDocument()
  })
})
