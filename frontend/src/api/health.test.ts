import axios from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import { mockAdapter } from '../test/axiosMock'
import { fetchHealth } from './health'

describe('fetchHealth', () => {
  const originalAdapter = axios.defaults.adapter

  afterEach(() => {
    axios.defaults.adapter = originalAdapter
  })

  it('returns UP when the API is healthy', async () => {
    mockAdapter(axios, () => ({ status: 200, data: { status: 'UP' } }))
    await expect(fetchHealth()).resolves.toBe('UP')
  })

  it('returns DOWN when the API reports itself unhealthy', async () => {
    mockAdapter(axios, () => ({ status: 503, data: { status: 'DOWN' } }))
    await expect(fetchHealth()).resolves.toBe('DOWN')
  })

  it('fails when the API is unreachable', async () => {
    mockAdapter(axios, () => undefined)
    await expect(fetchHealth()).rejects.toThrow()
  })
})
