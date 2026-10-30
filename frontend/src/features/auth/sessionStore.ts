import { useSyncExternalStore } from 'react'

export type Role = 'USER' | 'ADMIN'

export type SessionUser = {
  id: string
  name: string
  email: string
  role: Role
}

export type AuthResponse = {
  accessToken: string
  expiresIn: number
  user: SessionUser
}

export type SessionState =
  | { status: 'loading'; user: null }
  | { status: 'anonymous'; user: null }
  | { status: 'authenticated'; user: SessionUser }

// The access token lives only in this module's memory, never in localStorage or sessionStorage.
let accessToken: string | null = null
let state: SessionState = { status: 'loading', user: null }
const listeners = new Set<() => void>()

function emit(next: SessionState) {
  state = next
  listeners.forEach((listener) => listener())
}

export const sessionStore = {
  getState: (): SessionState => state,
  getAccessToken: (): string | null => accessToken,
  subscribe(listener: () => void) {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
  authenticate(response: AuthResponse) {
    accessToken = response.accessToken
    emit({ status: 'authenticated', user: response.user })
  },
  clear() {
    accessToken = null
    emit({ status: 'anonymous', user: null })
  },
  /** Test helper: puts the store back in its initial state. */
  reset() {
    accessToken = null
    emit({ status: 'loading', user: null })
  },
}

export function useSession(): SessionState {
  return useSyncExternalStore(sessionStore.subscribe, sessionStore.getState)
}
