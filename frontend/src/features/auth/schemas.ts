import { z } from 'zod'
import { validationMessage as message } from '../../i18n/fieldErrors'
import { isValidCpf } from '../../lib/cpf'

const utf8Bytes = (value: string) => new TextEncoder().encode(value).length

// Messages are translation keys, translated when shown (useFieldErrorText), so they follow language changes.

/** Same rules as the backend RegisterRequest. */
export const registerSchema = z.object({
  name: z.string().trim().min(2, message('nameRequired')).max(100, message('nameTooLong')),
  email: z
    .string()
    .trim()
    .toLowerCase()
    .min(1, message('emailRequired'))
    .max(254, message('emailTooLong'))
    .pipe(z.email(message('emailInvalid'))),
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
