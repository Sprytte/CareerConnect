import { backendUrl } from '../constants'

export type AccountProfile = {
  userId: string
  name: string
  email: string
}

export class ProfileError extends Error {
  readonly sessionExpired: boolean

  constructor(message: string, sessionExpired = false) {
    super(message)
    this.name = 'ProfileError'
    this.sessionExpired = sessionExpired
  }
}

function profileUrl(userId: string): string {
  return `${backendUrl}/api/v1/users/${encodeURIComponent(userId)}`
}

async function readProfile(
  response: Response,
  userId: string,
): Promise<AccountProfile> {
  if (response.status === 401) {
    throw new ProfileError(
      'Your session has expired. Sign in again to continue.',
      true,
    )
  }
  if (response.status === 403) {
    throw new ProfileError('You do not have permission to access this account.')
  }
  if (response.status === 409) {
    throw new ProfileError('This email address is already in use. Try another address.')
  }
  if (!response.ok) {
    throw new ProfileError(
      'We could not complete your request. Please try again.',
    )
  }

  // The current backend can return a successful status with an empty or invalid
  // body after an Auth0 failure. Status alone is not evidence of a saved profile.
  let data: unknown
  try {
    data = await response.json()
  } catch {
    throw new ProfileError(
      'We could not confirm your account details. Please try again.',
    )
  }
  if (typeof data !== 'object' || data === null) {
    throw new ProfileError(
      'We could not confirm your account details. Please try again.',
    )
  }
  const profile = data as Record<string, unknown>
  if (
    profile.userId !== userId ||
    typeof profile.name !== 'string' ||
    typeof profile.email !== 'string' ||
    !profile.name.trim() || !profile.email.trim()
  ) {
    throw new ProfileError(
      'We could not confirm your account details. Please try again.',
    )
  }
  return { userId, name: profile.name.trim(), email: profile.email.trim() }
}

export async function loadProfile(
  userId: string,
  signal: AbortSignal,
): Promise<AccountProfile> {
  const response = await fetch(profileUrl(userId), {
    credentials: 'include',
    headers: { Accept: 'application/json' },
    cache: 'no-store',
    signal,
  })
  return readProfile(response, userId)
}

export async function saveProfile(
  current: AccountProfile,
  changes: Pick<AccountProfile, 'name' | 'email'>,
  signal: AbortSignal,
): Promise<AccountProfile> {
  const response = await fetch(profileUrl(current.userId), {
    method: 'PUT',
    credentials: 'include',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: changes.name, email: changes.email }),
    signal,
  })
  const saved = await readProfile(response, current.userId)
  if (saved.name !== changes.name || saved.email !== changes.email) {
    throw new ProfileError(
      'We could not confirm your changes. Reload your account details before trying again.',
    )
  }
  return saved
}

export async function savePassword(userId: string, password: string, signal: AbortSignal): Promise<void> {
  const response = await fetch(`${profileUrl(userId)}/password`, {
    method: 'PUT', credentials: 'include', signal,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  })
  if (response.status === 401) throw new ProfileError('Your session has expired. Sign in again to continue.', true)
  if (response.status === 403) throw new ProfileError('You do not have permission to access this account.')
  if (response.status === 400) throw new ProfileError('The password does not meet your sign-in provider’s policy. Use at least 15 characters and try another password.')
  if (response.status === 409) throw new ProfileError('Change your password through your sign-in provider.')
  if (response.status !== 204) throw new ProfileError('We could not confirm the password change. Please try again.')
}

export function profileErrorMessage(error: unknown): string {
  return error instanceof ProfileError
    ? error.message
    : 'We could not connect to your account. Please check your connection and try again.'
}
