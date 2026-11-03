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

// Messages are translation keys; the components translate them (see RegisterPage and ConfirmPage tests).
describe('registerSchema', () => {
  it('accepts valid data and normalizes the e-mail', () => {
    const result = registerSchema.parse({ ...valid, email: '  Ana@Demo.COM ' })
    expect(result.email).toBe('ana@demo.com')
  })

  it('rejects a short name', () => {
    expect(errorsFor({ ...valid, name: ' A ' }).name).toEqual(['validation.nameRequired'])
  })

  it('rejects an invalid e-mail', () => {
    expect(errorsFor({ ...valid, email: 'ana@' }).email).toEqual(['validation.emailInvalid'])
  })

  it('rejects a CPF with wrong verification digits', () => {
    expect(errorsFor({ ...valid, document: '529.982.247-24' }).document).toEqual([
      'validation.cpfInvalid',
    ])
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
      ['validation.passwordTooShort'],
    )
  })

  it('rejects a password over 72 bytes even with fewer characters', () => {
    const long = 'ç'.repeat(40)
    expect(activationErrors({ password: long, passwordConfirmation: long }).password).toEqual([
      'validation.passwordTooManyBytes',
    ])
  })

  it('rejects a confirmation that differs from the password', () => {
    expect(
      activationErrors({ password, passwordConfirmation: 's3nh@fort' }).passwordConfirmation,
    ).toEqual(['validation.passwordMismatch'])
  })

  it('asks to repeat the password when the confirmation is empty', () => {
    expect(activationErrors({ password, passwordConfirmation: '' }).passwordConfirmation).toContain(
      'validation.passwordConfirmationRequired',
    )
  })
})
