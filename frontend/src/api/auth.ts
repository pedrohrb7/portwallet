import { authClient } from './client'

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

/** Sets the password of the account and activates it. */
export async function confirmEmail(token: string, password: string): Promise<void> {
  await authClient.post('/auth/confirm', { token, password })
}
