import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { sessionStore, type AuthResponse } from '../features/auth/sessionStore'

const API_BASE_URL = '/api/v1'

/** Client for every authenticated call: adds the bearer token and refreshes it once on 401. */
export const api = axios.create({ baseURL: API_BASE_URL })

/** Client for the cookie-based auth endpoints, without the interceptors (a 401 there must not trigger a refresh). */
export const authClient = axios.create({ baseURL: API_BASE_URL })

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

let refreshInFlight: Promise<boolean> | null = null

/**
 * Exchanges the refresh cookie for a new access token. Concurrent callers share the same request, so a burst of
 * 401 responses triggers a single refresh.
 */
export function refreshSession(): Promise<boolean> {
  refreshInFlight ??= authClient
    .post<AuthResponse>('/auth/refresh')
    .then((response) => {
      sessionStore.authenticate(response.data)
      return true
    })
    .catch(() => {
      sessionStore.clear()
      return false
    })
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

api.interceptors.request.use((config) => {
  const token = sessionStore.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(undefined, async (error: AxiosError) => {
  const original = error.config as RetriableConfig | undefined
  if (error.response?.status === 401 && original && !original._retried) {
    original._retried = true
    if (await refreshSession()) {
      return api(original)
    }
  }
  return Promise.reject(error)
})
