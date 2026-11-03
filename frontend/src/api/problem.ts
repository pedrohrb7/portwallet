import { isAxiosError } from 'axios'
import { ptBR, type Catalog } from '../i18n/locales/pt-BR'

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

type MessageType = Exclude<keyof Catalog['problem'], 'fallback'>
export type ProblemKey = `problem.${keyof Catalog['problem']}`

function hasMessage(type: string): type is MessageType {
  return type !== 'fallback' && Object.hasOwn(ptBR.problem, type)
}

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

/** Translation key of the message shown for a problem, picked by its `type`; a generic one for unknown types. */
export function problemKey(problem: Problem): ProblemKey {
  return hasMessage(problem.type) ? `problem.${problem.type}` : 'problem.fallback'
}
