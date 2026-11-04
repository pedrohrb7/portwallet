import { z } from 'zod'
import { validationMessage as message } from '../../i18n/fieldErrors'
import { isValidCpf } from '../../lib/cpf'

const utf8Bytes = (value: string) => new TextEncoder().encode(value).length

// Messages are translation keys, translated when shown (useFieldErrorText), so they follow language changes.

const email = z
  .string()
  .trim()
  .toLowerCase()
  .min(1, message('emailRequired'))
  .max(254, message('emailTooLong'))
  .pipe(z.email(message('emailInvalid')))

/** Same rules as the backend RegisterRequest. */
export const registerSchema = z.object({
  name: z.string().trim().min(2, message('nameRequired')).max(100, message('nameTooLong')),
  email,
  document: z.string().trim().refine(isValidCpf, message('cpfInvalid')),
})

export type RegisterInput = z.input<typeof registerSchema>
export type RegisterData = z.output<typeof registerSchema>

/** Password chosen on activation: same rules as the backend ConfirmRequest, plus a confirmation field. */
export const activationSchema = z
  .object({
    password: z
      .string()
      .min(8, message('passwordTooShort'))
      .max(72, message('passwordTooLong'))
      .refine((value) => utf8Bytes(value) <= 72, message('passwordTooManyBytes')),
    passwordConfirmation: z.string().min(1, message('passwordConfirmationRequired')),
  })
  .refine((data) => data.password === data.passwordConfirmation, {
    message: message('passwordMismatch'),
    path: ['passwordConfirmation'],
  })

export type ActivationData = z.infer<typeof activationSchema>

/**
 * Same rules as the backend LoginRequest. The password is not trimmed and has no minimum length beyond being present:
 * a wrong password must reach the server and get the generic answer, not hint at the password rules.
 */
export const loginSchema = z.object({
  email,
  password: z
    .string()
    .min(1, message('passwordRequired'))
    .refine((value) => utf8Bytes(value) <= 72, message('passwordTooManyBytes')),
})

export type LoginInput = z.input<typeof loginSchema>
export type LoginData = z.output<typeof loginSchema>
