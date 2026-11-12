import { createContext, useContext } from 'react'
import { backendUrl } from '../constants'
import { isUserRole, type UserRole } from './roles'

export const loginUrl = `${backendUrl}/oauth2/authorization/okta`
export const logoutUrl = `${backendUrl}/api/v1/careercontact/logout`

// Frontend session data derived from the authenticated backend /me response.
export type SessionUser = {
  userId: string
  name: string
  email?: string
  role: UserRole | null
  permissions: string[]
}

export type SessionUserUpdate = { userId: string } & Partial<
  Omit<SessionUser, 'userId'>
>

export type SessionState =
  | { status: 'loading' }
  | { status: 'signed-out' }
  | { status: 'signed-in'; user: SessionUser }
  | { status: 'unavailable' }

export const SessionContext = createContext<SessionState | undefined>(undefined)

export const SessionUserUpdateContext = createContext<
  ((update: SessionUserUpdate) => void) | undefined
>(undefined)

export function parseSessionUser(value: unknown): SessionUser | null {
  if (typeof value !== 'object' || value === null) return null

  const user = value as Record<string, unknown>

  if (typeof user.userId !== 'string' || user.userId.trim() === '') {
    return null
  }

  // OIDC profile claims may be absent. Identity comes from userId;
  // the name is only a display label, with email or neutral text as fallback.
  const name = typeof user.name === 'string' ? user.name.trim() : ''
  const email = typeof user.email === 'string' ? user.email.trim() : ''
  const role = isUserRole(user.role) ? user.role : null
  const permissions = Array.isArray(user.permissions)
    ? user.permissions.filter(
        (permission): permission is string => typeof permission === 'string',
      )
    : []

  return {
    userId: user.userId,
    name: name || email || 'Account',
    email: email || undefined,
    role,
    permissions,
  }
}

export async function checkSession(signal: AbortSignal): Promise<SessionState> {
  // Ask the backend to validate authentication. A readable cookie alone
  // does not establish that the user has an authenticated session.
  const response = await fetch(`${backendUrl}/api/v1/cc/security/me`, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
    signal,
  })

  if (response.status === 401) return { status: 'signed-out' }
  if (!response.ok) return { status: 'unavailable' }

  const data: unknown = await response.json()
  const user = parseSessionUser(data)

  return user ? { status: 'signed-in', user } : { status: 'unavailable' }
}

// Consumers share the provider's result instead of fetching independently.
export function useSession(): SessionState {
  const session = useContext(SessionContext)

  if (session === undefined) {
    throw new Error('useSession must be used within SessionProvider')
  }

  return session
}

// Fresh profile data can update the greeting without trusting form input or
// re-reading stale OIDC claims. Updates are merged so callers can change only
// the fields they own while preserving role and permissions.
export function useUpdateSessionUser(): (update: SessionUserUpdate) => void {
  const update = useContext(SessionUserUpdateContext)
  if (!update)
    throw new Error('useUpdateSessionUser must be used within SessionProvider')
  return update
}
