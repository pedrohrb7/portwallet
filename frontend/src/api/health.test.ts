import { afterEach, describe, expect, it } from 'vitest'
import { mockAdapter } from '../test/axiosMock'
import { rootClient } from './client'
import { fetchHealth } from './health'

describe('fetchHealth', () => {
  const originalAdapter = rootClient.defaults.adapter

  afterEach(() => {
    rootClient.defaults.adapter = originalAdapter
  })

  it('returns UP when the API is healthy', async () => {
    mockAdapter(rootClient, () => ({ status: 200, data: { status: 'UP' } }))
    await expect(fetchHealth()).resolves.toBe('UP')
  })

  it('returns DOWN when the API reports itself unhealthy', async () => {
    mockAdapter(rootClient, () => ({ status: 503, data: { status: 'DOWN' } }))
    await expect(fetchHealth()).resolves.toBe('DOWN')
  })

  it('fails when the API is unreachable', async () => {
    mockAdapter(rootClient, () => undefined)
    await expect(fetchHealth()).rejects.toThrow()
  })
})
