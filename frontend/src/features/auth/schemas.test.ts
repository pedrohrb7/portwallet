import { describe, expect, it } from 'vitest'
import { z } from 'zod'
import { registerSchema } from './schemas'

const valid = {
  name: 'Ana Souza',
  email: 'ana@demo.com',
  document: '529.982.247-25',
  password: 's3nh@forte',
}

function errorsFor(input: Record<string, string>) {
  const result = registerSchema.safeParse(input)
  return result.success ? {} : z.flattenError(result.error).fieldErrors
}

describe('registerSchema', () => {
  it('accepts valid data and normalizes the e-mail', () => {
    const result = registerSchema.parse({ ...valid, email: '  Ana@Demo.COM ' })
    expect(result.email).toBe('ana@demo.com')
  })

  it('rejects a short name', () => {
    expect(errorsFor({ ...valid, name: ' A ' }).name).toEqual(['Informe o nome completo'])
  })

  it('rejects an invalid e-mail', () => {
    expect(errorsFor({ ...valid, email: 'ana@' }).email).toEqual(['E-mail inválido'])
  })

  it('rejects a CPF with wrong verification digits', () => {
    expect(errorsFor({ ...valid, document: '529.982.247-24' }).document).toEqual(['CPF inválido'])
  })

  it('rejects a short password', () => {
    expect(errorsFor({ ...valid, password: 'short' }).password).toEqual([
      'Use pelo menos 8 caracteres',
    ])
  })

  it('rejects a password over 72 bytes even with fewer characters', () => {
    expect(errorsFor({ ...valid, password: 'ç'.repeat(40) }).password).toEqual([
      'Senha longa demais',
    ])
  })
})
