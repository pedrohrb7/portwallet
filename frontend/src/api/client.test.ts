import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { sessionStore, type AuthResponse } from '../features/auth/sessionStore'
import { mockAdapter } from '../test/axiosMock'
import { api, authClient, refreshSession } from './client'

const user = { id: 'u-1', name: 'Ana Souza', email: 'ana@demo.com', role: 'USER' as const }

function authResponse(token: string): AuthResponse {
  return { accessToken: token, expiresIn: 900, user }
}

describe('api client', () => {
  let refreshCalls: number

  beforeEach(() => {
    refreshCalls = 0
    sessionStore.authenticate(authResponse('expired-token'))
    mockAdapter(authClient, () => {
      refreshCalls++
      return { status: 200, data: authResponse('fresh-token') }
    })
  })

  afterEach(() => {
    sessionStore.reset()
  })

  it('sends the access token as a bearer header', async () => {
    let authorization: unknown
    mockAdapter(api, (config) => {
      authorization = config.headers.Authorization
      return { status: 200, data: {} }
    })

    await api.get('/accounts/me')

    expect(authorization).toBe('Bearer expired-token')
  })

  it('refreshes once for simultaneous 401 responses and retries every request', async () => {
    const seenTokens: unknown[] = []
    mockAdapter(api, (config) => {
      seenTokens.push(config.headers.Authorization)
      return config.headers.Authorization === 'Bearer fresh-token'
        ? { status: 200, data: { url: config.url } }
        : { status: 401, data: { type: 'unauthorized', status: 401 } }
    })

    const results = await Promise.all([api.get('/a'), api.get('/b'), api.get('/c')])

    expect(refreshCalls).toBe(1)
    expect(results.map((r) => r.data.url)).toEqual(['/a', '/b', '/c'])
    expect(seenTokens.filter((t) => t === 'Bearer fresh-token')).toHaveLength(3)
    expect(sessionStore.getState()).toEqual({ status: 'authenticated', user })
  })

  it('gives up after one retry and clears the session when the refresh fails', async () => {
    mockAdapter(authClient, () => {
      refreshCalls++
      return { status: 401, data: { type: 'invalid-refresh-token', status: 401 } }
    })
    mockAdapter(api, () => ({ status: 401, data: { type: 'unauthorized', status: 401 } }))

    await expect(api.get('/accounts/me')).rejects.toMatchObject({ response: { status: 401 } })

    expect(refreshCalls).toBe(1)
    expect(sessionStore.getState().status).toBe('anonymous')
    expect(sessionStore.getAccessToken()).toBeNull()
  })

  it('does not refresh on other errors', async () => {
    mockAdapter(api, () => ({ status: 422, data: { type: 'insufficient-balance', status: 422 } }))

    await expect(api.post('/transfers')).rejects.toMatchObject({ response: { status: 422 } })

    expect(refreshCalls).toBe(0)
  })

  it('restores the session from the refresh cookie', async () => {
    sessionStore.reset()

    await expect(refreshSession()).resolves.toBe(true)

    expect(sessionStore.getAccessToken()).toBe('fresh-token')
  })
})
