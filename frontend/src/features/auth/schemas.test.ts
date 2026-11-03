import { describe, expect, it } from 'vitest'
import { z } from 'zod'
import { activationSchema, registerSchema } from './schemas'

const valid = {
  name: 'Ana Souza',
  email: 'ana@demo.com',
  document: '529.982.247-25',
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

  it('ignores a password, which is no longer part of the registration', () => {
    expect(registerSchema.parse({ ...valid, password: 's3nh@forte' })).not.toHaveProperty(
      'password',
    )
  })
})

describe('activationSchema', () => {
  const password = 's3nh@forte'

  function activationErrors(input: { password: string; passwordConfirmation: string }) {
    const result = activationSchema.safeParse(input)
    return result.success ? {} : z.flattenError(result.error).fieldErrors
  }

  it('accepts matching passwords', () => {
    expect(activationErrors({ password, passwordConfirmation: password })).toEqual({})
  })

  it('rejects a short password', () => {
    expect(activationErrors({ password: 'short', passwordConfirmation: 'short' }).password).toEqual(
      ['Use pelo menos 8 caracteres'],
    )
  })

  it('rejects a password over 72 bytes even with fewer characters', () => {
    const long = 'ç'.repeat(40)
    expect(activationErrors({ password: long, passwordConfirmation: long }).password).toEqual([
      'Senha longa demais',
    ])
  })

  it('rejects a confirmation that differs from the password', () => {
    expect(
      activationErrors({ password, passwordConfirmation: 's3nh@fort' }).passwordConfirmation,
    ).toEqual(['As senhas não são iguais'])
  })

  it('asks to repeat the password when the confirmation is empty', () => {
    expect(activationErrors({ password, passwordConfirmation: '' }).passwordConfirmation).toContain(
      'Repita a senha',
    )
  })
})
