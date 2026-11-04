import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authClient } from '../../api/client'
import { mockAdapter } from '../../test/axiosMock'
import { login, restore } from './session'
import { sessionStore, type AuthResponse } from './sessionStore'

const user = { id: 'u-1', name: 'Ana Souza', email: 'ana@demo.com', role: 'USER' as const }
const authResponse: AuthResponse = { accessToken: 'access-1', expiresIn: 900, user }

describe('session', () => {
  const originalAdapter = authClient.defaults.adapter
  let calls: { url?: string; data?: unknown }[]

  beforeEach(() => {
    calls = []
  })

  afterEach(() => {
    authClient.defaults.adapter = originalAdapter
    sessionStore.reset()
  })

  describe('login', () => {
    it('sends the credentials and keeps the access token and the user in memory', async () => {
      mockAdapter(authClient, (config) => {
        calls.push({ url: config.url, data: JSON.parse(config.data as string) })
        return { status: 200, data: authResponse }
      })

      await expect(login({ email: 'ana@demo.com', password: 's3nh@forte' })).resolves.toEqual(user)

      expect(calls).toEqual([
        { url: '/auth/login', data: { email: 'ana@demo.com', password: 's3nh@forte' } },
      ])
      expect(sessionStore.getAccessToken()).toBe('access-1')
      expect(sessionStore.getState()).toEqual({ status: 'authenticated', user })
    })

    it('rejects with the API error and leaves the session as it was', async () => {
      sessionStore.clear()
      mockAdapter(authClient, () => ({
        status: 401,
        data: { type: 'invalid-credentials', status: 401 },
      }))

      await expect(login({ email: 'ana@demo.com', password: 'errada' })).rejects.toMatchObject({
        response: { status: 401 },
      })
      expect(sessionStore.getState().status).toBe('anonymous')
      expect(sessionStore.getAccessToken()).toBeNull()
    })
  })

  describe('restore', () => {
    it('exchanges the refresh cookie for a session on load', async () => {
      mockAdapter(authClient, (config) => {
        calls.push({ url: config.url })
        return { status: 200, data: authResponse }
      })

      await expect(restore()).resolves.toBe(true)

      expect(calls).toEqual([{ url: '/auth/refresh' }])
      expect(sessionStore.getState()).toEqual({ status: 'authenticated', user })
    })

    it('ends anonymous when there is no valid refresh cookie', async () => {
      mockAdapter(authClient, () => ({
        status: 401,
        data: { type: 'invalid-refresh-token', status: 401 },
      }))

      await expect(restore()).resolves.toBe(false)
      expect(sessionStore.getState().status).toBe('anonymous')
    })

    it('sends a single request when called twice during the same load', async () => {
      mockAdapter(authClient, (config) => {
        calls.push({ url: config.url })
        return { status: 200, data: authResponse }
      })

      await Promise.all([restore(), restore()])

      expect(calls).toHaveLength(1)
    })

    it('does nothing once the session is known', async () => {
      mockAdapter(authClient, (config) => {
        calls.push({ url: config.url })
        return { status: 200, data: authResponse }
      })
      sessionStore.clear()

      await expect(restore()).resolves.toBe(false)
      expect(calls).toHaveLength(0)
    })
  })
})
