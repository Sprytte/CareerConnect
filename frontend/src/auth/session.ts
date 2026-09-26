import { useEffect, useState } from 'react'

// This URL is public configuration. Auth0 client secrets belong on the backend.
const backendUrl = (import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080').replace(/\/$/, '')

export const loginUrl = `${backendUrl}/oauth2/authorization/okta`
export const logoutUrl = `${backendUrl}/api/v1/careercontact/logout`

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

function isSessionUser(value: unknown): value is SessionUser {
  if (typeof value !== 'object' || value === null) return false

  const user = value as Record<string, unknown>
  return typeof user.userId === 'string'
    && typeof user.name === 'string'
    && Array.isArray(user.roles)
    && user.roles.every((role) => typeof role === 'string')
}

async function checkSession(signal: AbortSignal): Promise<SessionState> {
  // The backend must validate its session and return 401 when there is none.
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

export function useSession(): SessionState {
  const [session, setSession] = useState<SessionState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()

    checkSession(controller.signal)
      .then(setSession)
      .catch(() => {
        if (!controller.signal.aborted) setSession({ status: 'unavailable' })
      })

    return () => controller.abort()
  }, [])

  return session
}
