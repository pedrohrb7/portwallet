import { expect, type APIRequestContext } from '@playwright/test'
import { uniqueEmail, validCpf } from './data.ts'
import { linkFromInbox } from './inbox.ts'

export const PASSWORD = 's3nh@forte'

export type ActiveUser = { name: string; email: string; password: string }

/**
 * Registers and activates a new user through the API, the way the registration flow does: registration, the link
 * from the demo inbox, and the activation that sets the password. Each call creates a different user.
 */
export async function createActiveUser(
  request: APIRequestContext,
  name = 'Ana Souza',
): Promise<ActiveUser> {
  const email = uniqueEmail()
  const registration = await request.post('/api/v1/auth/register', {
    data: { name, email, document: validCpf() },
  })
  expect(registration.status()).toBe(202)

  const link = await linkFromInbox(request, email, 'Ative sua conta PortWallet')
  const token = new URLSearchParams(new URL(link).hash.slice(1)).get('token')
  expect(token).not.toBeNull()

  const activation = await request.post('/api/v1/auth/confirm', {
    data: { token, password: PASSWORD },
  })
  expect(activation.ok()).toBe(true)
  return { name, email, password: PASSWORD }
}
