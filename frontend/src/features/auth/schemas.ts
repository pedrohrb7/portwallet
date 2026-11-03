import { z } from 'zod'
import { isValidCpf } from '../../lib/cpf'

const utf8Bytes = (value: string) => new TextEncoder().encode(value).length

/** Same rules as the backend RegisterRequest. */
export const registerSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, 'Informe o nome completo')
    .max(100, 'Use no máximo 100 caracteres'),
  email: z
    .string()
    .trim()
    .toLowerCase()
    .min(1, 'Informe o e-mail')
    .max(254, 'E-mail longo demais')
    .pipe(z.email('E-mail inválido')),
  document: z.string().trim().refine(isValidCpf, 'CPF inválido'),
})

export type RegisterInput = z.input<typeof registerSchema>
export type RegisterData = z.output<typeof registerSchema>

/** Password chosen on activation: same rules as the backend ConfirmRequest, plus a confirmation field. */
export const activationSchema = z
  .object({
    password: z
      .string()
      .min(8, 'Use pelo menos 8 caracteres')
      .max(72, 'Use no máximo 72 caracteres')
      .refine((value) => utf8Bytes(value) <= 72, 'Senha longa demais'),
    passwordConfirmation: z.string().min(1, 'Repita a senha'),
  })
  .refine((data) => data.password === data.passwordConfirmation, {
    message: 'As senhas não são iguais',
    path: ['passwordConfirmation'],
  })

export type ActivationData = z.infer<typeof activationSchema>
