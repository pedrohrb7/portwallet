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
  password: z
    .string()
    .min(8, 'Use pelo menos 8 caracteres')
    .max(72, 'Use no máximo 72 caracteres')
    .refine((value) => utf8Bytes(value) <= 72, 'Senha longa demais'),
})

export type RegisterInput = z.input<typeof registerSchema>
export type RegisterData = z.output<typeof registerSchema>
