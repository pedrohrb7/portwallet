import { authClient } from './client'

export type RegisterRequest = {
  name: string
  email: string
  document: string
  password: string
}

export async function register(request: RegisterRequest): Promise<string> {
  const response = await authClient.post<{ message: string }>('/auth/register', request)
  return response.data.message
}

export async function confirmEmail(token: string, password: string): Promise<void> {
  await authClient.post('/auth/confirm', { token, password })
}
