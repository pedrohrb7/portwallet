import { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { messageFor, toProblem } from './problem'

const config = { headers: {} } as InternalAxiosRequestConfig

function responseError(status: number, data: unknown) {
  return new AxiosError('failed', AxiosError.ERR_BAD_RESPONSE, config, null, {
    status,
    data,
    statusText: '',
    headers: {},
    config,
  })
}

describe('toProblem', () => {
  it('returns the Problem Details body sent by the API', () => {
    const body = { type: 'insufficient-balance', title: 'Saldo insuficiente', status: 422 }
    expect(toProblem(responseError(422, body))).toEqual(body)
  })

  it('maps a missing response to a network error', () => {
    expect(toProblem(new AxiosError('Network Error', AxiosError.ERR_NETWORK, config))).toEqual({
      type: 'network-error',
      status: 0,
    })
  })

  it('maps a body that is not Problem Details to an internal error', () => {
    expect(toProblem(responseError(502, '<html>Bad Gateway</html>'))).toEqual({
      type: 'internal-error',
      status: 502,
    })
  })

  it('maps non-axios errors to an internal error', () => {
    expect(toProblem(new Error('boom')).type).toBe('internal-error')
  })
})

describe('messageFor', () => {
  it('translates known types', () => {
    expect(messageFor({ type: 'insufficient-balance', status: 422 })).toBe('Saldo insuficiente.')
  })

  it('falls back to a generic message for unknown types', () => {
    expect(messageFor({ type: 'something-new', status: 400 })).toBe(
      'Algo deu errado. Tente novamente em instantes.',
    )
  })
})
