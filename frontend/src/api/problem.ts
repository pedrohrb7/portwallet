import { isAxiosError } from 'axios'

export type FieldError = { field: string; message: string }

/** Problem Details (RFC 9457) as returned by the API. */
export type Problem = {
  type: string
  title?: string
  status: number
  detail?: string
  errors?: FieldError[]
  [property: string]: unknown
}

const NETWORK_ERROR = 'network-error'

const messages: Record<string, string> = {
  'validation-error': 'Confira os dados informados.',
  'invalid-cursor': 'Não foi possível carregar mais itens. Recarregue a página.',
  'invalid-token': 'Link inválido ou expirado.',
  unauthorized: 'Sua sessão expirou. Entre novamente.',
  'invalid-credentials': 'E-mail ou senha inválidos.',
  'wrong-password': 'Senha incorreta. Use a senha escolhida no cadastro mais recente.',
  'invalid-refresh-token': 'Sua sessão expirou. Entre novamente.',
  forbidden: 'Você não tem permissão para acessar isto.',
  'account-blocked': 'Sua conta está bloqueada para movimentações.',
  'not-found': 'Não encontrado.',
  'recipient-not-found': 'Destinatário não encontrado ou indisponível.',
  'concurrency-conflict': 'Muitas operações ao mesmo tempo. Tente novamente.',
  'insufficient-balance': 'Saldo insuficiente.',
  'daily-limit-exceeded': 'Valor acima do limite diário disponível.',
  'self-transfer': 'Não é possível transferir para a própria conta.',
  'recipient-unavailable': 'Destinatário indisponível. Busque novamente.',
  [NETWORK_ERROR]: 'Sem conexão com o servidor. Verifique sua internet e tente novamente.',
}

const fallbackMessage = 'Algo deu errado. Tente novamente em instantes.'

function isProblem(value: unknown): value is Problem {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Problem).type === 'string' &&
    typeof (value as Problem).status === 'number'
  )
}

/** Normalizes any error thrown by the API client into a Problem. */
export function toProblem(error: unknown): Problem {
  if (isProblem(error)) {
    return error
  }
  if (isAxiosError(error)) {
    if (!error.response) {
      return { type: NETWORK_ERROR, status: 0 }
    }
    if (isProblem(error.response.data)) {
      return error.response.data
    }
    return { type: 'internal-error', status: error.response.status }
  }
  return { type: 'internal-error', status: 0 }
}

export function messageFor(problem: Problem): string {
  return messages[problem.type] ?? fallbackMessage
}
