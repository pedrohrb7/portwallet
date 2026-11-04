import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from '../features/auth/sessionStore'
import { SessionLoading } from './SessionLoading'

export type LoginRedirectState = { from: string }

/** Guards the routes below it: waits for the session restore and sends anonymous users to /login. */
export function RequireAuth() {
  const session = useSession()
  const location = useLocation()

  if (session.status === 'loading') {
    return <SessionLoading />
  }

  if (session.status === 'anonymous') {
    const state: LoginRedirectState = { from: location.pathname + location.search }
    return <Navigate to="/login" replace state={state} />
  }

  return <Outlet />
}
