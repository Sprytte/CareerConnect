import { createContext, useContext } from 'react'

// Public configuration only. Auth0 client secrets belong on the backend.
const backendUrl = (
  import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'
).replace(/\/$/, '')

export const loginUrl = `${backendUrl}/oauth2/authorization/okta`
export const logoutUrl = `${backendUrl}/api/v1/careercontact/logout`

// This response shape is provisional until the backend contract is confirmed.
export type SessionUser = {
  userId: string
  name: string
  roles: string[]
}

export type SessionState =
  | { status: 'loading' }
  | { status: 'signed-out' }
  | { status: 'signed-in'; user: SessionUser }
  | { status: 'unavailable' }

export const SessionContext = createContext<SessionState | undefined>(undefined)

function isSessionUser(value: unknown): value is SessionUser {
  if (typeof value !== 'object' || value === null) return false

  const user = value as Record<string, unknown>

  return typeof user.userId === 'string'
    && typeof user.name === 'string'
    && Array.isArray(user.roles)
    && user.roles.every((role) => typeof role === 'string')
}

export async function checkSession(
  signal: AbortSignal,
): Promise<SessionState> {
  // The backend must validate its session and return 401 when signed out.
  // A readable "isAuthenticated" cookie is not proof of authentication.
  const response = await fetch(`${backendUrl}/api/v1/cc/security/me`, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
    signal,
  })

  if (response.status === 401) return { status: 'signed-out' }
  if (!response.ok) return { status: 'unavailable' }

  const data: unknown = await response.json()

  return isSessionUser(data)
    ? { status: 'signed-in', user: data }
    : { status: 'unavailable' }
}

// Consumers share the provider's result instead of fetching independently.
export function useSession(): SessionState {
  const session = useContext(SessionContext)

  if (session === undefined) {
    throw new Error('useSession must be used within SessionProvider')
  }

  return session
}