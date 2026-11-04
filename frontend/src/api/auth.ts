import type { AuthResponse } from '../features/auth/sessionStore'
import { authClient } from './client'

export type LoginRequest = {
  email: string
  password: string
}

export type RegisterRequest = {
  name: string
  email: string
  document: string
}

/** Masked data of the registration a confirmation link belongs to. */
export type RegistrationSummary = {
  maskedName: string
  maskedDocument: string
}

export async function register(request: RegisterRequest): Promise<string> {
  const response = await authClient.post<{ message: string }>('/auth/register', request)
  return response.data.message
}

export async function confirmationSummary(token: string): Promise<RegistrationSummary> {
  // The token goes in the body, never in the URL, so it stays out of access logs.
  const response = await authClient.post<RegistrationSummary>('/auth/confirm/summary', { token })
  return response.data
}

/**
 * Exchanges the credentials for an access token. Goes through `authClient`: a 401 here means wrong credentials, not an
 * expired session, so it must not trigger a refresh. The refresh token arrives in the httpOnly cookie.
 */
export async function login(request: LoginRequest): Promise<AuthResponse> {
  const response = await authClient.post<AuthResponse>('/auth/login', request)
  return response.data
}

/** Sets the password of the account and activates it. */
export async function confirmEmail(token: string, password: string): Promise<void> {
  await authClient.post('/auth/confirm', { token, password })
}
