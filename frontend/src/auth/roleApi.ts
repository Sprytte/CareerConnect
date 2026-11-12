import { backendUrl } from '../constants'
import { parseSessionUser, type SessionUser } from './session'
import type { UserRole } from './roles'

export class RoleError extends Error {
  readonly sessionExpired: boolean

  constructor(message: string, sessionExpired = false) {
    super(message)
    this.name = 'RoleError'
    this.sessionExpired = sessionExpired
  }
}

export async function saveRole(
  userId: string,
  role: UserRole,
  signal: AbortSignal,
): Promise<SessionUser> {
  const response = await fetch(
    `${backendUrl}/api/v1/users/${encodeURIComponent(userId)}/role`,
    {
      method: 'PUT',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ role }),
      signal,
    },
  )

  if (response.status === 401) {
    throw new RoleError(
      'Your session has expired. Sign in again to choose your account type.',
      true,
    )
  }
  if (response.status === 403) {
    throw new RoleError('You can only choose the account type for your own account.')
  }
  if (response.status === 409) {
    throw new RoleError(
      'This account type has already been set. Reload the page to continue.',
    )
  }
  if (response.status === 400) {
    throw new RoleError('Choose Job seeker or Recruiter to continue.')
  }
  if (!response.ok) {
    throw new RoleError('We could not save your account type. Please try again.')
  }

  let data: unknown
  try {
    data = await response.json()
  } catch {
    throw new RoleError(
      'We could not confirm your account type. Please try again.',
    )
  }

  const user = parseSessionUser(data)
  if (!user || user.role !== role) {
    throw new RoleError(
      'We could not confirm your account type. Please try again.',
    )
  }

  return user
}

export function roleErrorMessage(error: unknown): string {
  return error instanceof RoleError
    ? error.message
    : 'We could not connect to your account. Check your connection and try again.'
}
