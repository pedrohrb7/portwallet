import { login as requestLogin, type LoginRequest } from '../../api/auth'
import { refreshSession } from '../../api/client'
import { sessionStore, type SessionUser } from './sessionStore'

/** Signs in and keeps the session in memory. Rejects with the API error, so the caller can show it. */
export async function login(request: LoginRequest): Promise<SessionUser> {
  const response = await requestLogin(request)
  sessionStore.authenticate(response)
  return response.user
}

/**
 * Restores the session on page load from the refresh cookie, which survives reloads while the access token does not.
 * Runs only while the session is still unknown, so a second call (Strict Mode, a remount) sends no extra request.
 */
export function restore(): Promise<boolean> {
  if (sessionStore.getState().status !== 'loading') {
    return Promise.resolve(sessionStore.getState().status === 'authenticated')
  }
  return refreshSession()
}
