import { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { i18n } from '../i18n'
import { problemKey, toProblem } from './problem'

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

  it('keeps a value that is already a Problem', () => {
    expect(toProblem({ type: 'invalid-token', status: 400 })).toEqual({
      type: 'invalid-token',
      status: 400,
    })
  })

  it('maps non-axios errors to an internal error', () => {
    expect(toProblem(new Error('boom')).type).toBe('internal-error')
  })
})

describe('problemKey', () => {
  it('picks the message of known types', () => {
    expect(problemKey({ type: 'insufficient-balance', status: 422 })).toBe(
      'problem.insufficient-balance',
    )
    expect(problemKey({ type: 'network-error', status: 0 })).toBe('problem.network-error')
  })

  it('falls back to a generic message for unknown types', () => {
    expect(problemKey({ type: 'something-new', status: 400 })).toBe('problem.fallback')
    expect(problemKey({ type: 'internal-error', status: 500 })).toBe('problem.fallback')
    expect(problemKey({ type: 'constructor', status: 400 })).toBe('problem.fallback')
  })

  it('has a message in every language', () => {
    const key = problemKey({ type: 'invalid-token', status: 400 })
    expect(i18n.t(key, { lng: 'pt-BR' })).toBe('Link inválido ou expirado.')
    expect(i18n.t(key, { lng: 'en' })).toBe('Invalid or expired link.')
    expect(i18n.t(key, { lng: 'es' })).toBe('Enlace no válido o caducado.')
    expect(i18n.t(key, { lng: 'fr' })).toBe('Lien invalide ou expiré.')
  })
})
