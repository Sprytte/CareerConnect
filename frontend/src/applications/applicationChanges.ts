import { backendUrl } from '../constants'
import type { JobApplication } from './applications'

// Task 14.2: edit (change the attached resume) or withdraw an application.
// The backend re-checks ownership and the posting deadline on every write, so
// these helpers only translate its answers into messages a person can act on.

export type ResumeOption = { id: number; fileName: string }

export type ChangeAvailability = 'open' | 'closed' | 'unknown'

export type ChangeErrorKind =
  | 'unauthenticated'
  | 'forbidden'
  | 'not-found'
  | 'deadline'
  | 'invalid'
  | 'unavailable'

export class ApplicationChangeError extends Error {
  readonly kind: ChangeErrorKind

  constructor(kind: ChangeErrorKind, message: string) {
    super(message)
    this.name = 'ApplicationChangeError'
    this.kind = kind
  }
}

export const maxResumeBytes = 5 * 1024 * 1024

// 'unknown' means the backend did not say (an older build): the actions stay
// available and the server's own deadline check decides.
export function changeAvailability(
  application: JobApplication,
  now: number,
): ChangeAvailability {
  if (application.modifiable === false) return 'closed'
  if (application.deadline) {
    const deadline = new Date(application.deadline).getTime()
    if (Number.isFinite(deadline)) return deadline > now ? 'open' : 'closed'
  }
  return application.modifiable === true ? 'open' : 'unknown'
}

export function formatDeadline(value: string | null | undefined): string | null {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  return new Intl.DateTimeFormat('en-CA', {
    month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit',
  }).format(date)
}

export function resumeFileProblem(file: File): string | null {
  const name = file.name.toLowerCase()
  if (!name.endsWith('.pdf') && !name.endsWith('.docx')) {
    return 'Choose a PDF or DOCX file.'
  }
  if (file.size === 0) return 'That file is empty. Choose another file.'
  if (file.size > maxResumeBytes) return 'Choose a file that is 5 MB or smaller.'
  return null
}

function applicationUrl(applicationId: string): string {
  return `${backendUrl}/api/v1/account/applications/${encodeURIComponent(applicationId)}`
}

// Spring includes the rejection reason in the error body's message field.
async function serverMessage(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json()
    if (typeof body === 'object' && body !== null) {
      const message = (body as Record<string, unknown>).message
      if (typeof message === 'string') return message
    }
  } catch {
    // An empty or non-JSON error body carries no extra detail.
  }
  return ''
}

async function changeError(response: Response): Promise<ApplicationChangeError> {
  if (response.status === 401) {
    return new ApplicationChangeError(
      'unauthenticated', 'Your session has expired. Sign in again to continue.')
  }
  if (response.status === 403) {
    return new ApplicationChangeError(
      'forbidden', 'You do not have permission to change this application.')
  }
  if (response.status === 404) {
    return new ApplicationChangeError(
      'not-found', 'This application or its job posting is no longer available. Refresh your list.')
  }
  if (response.status === 400) {
    const message = await serverMessage(response)
    return /deadline/i.test(message)
      ? new ApplicationChangeError(
        'deadline', 'The deadline for this job has passed, so the application can no longer be changed.')
      : new ApplicationChangeError(
        'invalid', 'That change could not be saved. Check your selection and try again.')
  }
  return new ApplicationChangeError(
    'unavailable', 'We could not save your change. Please try again in a moment.')
}

async function send(url: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(url, { credentials: 'include', cache: 'no-store', ...init })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApplicationChangeError(
      'unavailable', 'We could not reach CareerConnect. Check your connection and try again.')
  }
}

export async function changeApplicationResume(
  applicationId: string,
  resumeId: number,
  signal: AbortSignal,
): Promise<void> {
  const response = await send(applicationUrl(applicationId), {
    method: 'PUT',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ resumeId }),
    signal,
  })
  if (!response.ok || response.redirected) throw await changeError(response)
}

export async function withdrawApplication(
  applicationId: string,
  signal: AbortSignal,
): Promise<void> {
  const response = await send(applicationUrl(applicationId), {
    method: 'DELETE',
    headers: { Accept: 'application/json' },
    signal,
  })
  if (response.status !== 204 || response.redirected) throw await changeError(response)
}

function parseResumeOption(value: unknown): ResumeOption | null {
  if (typeof value !== 'object' || value === null) return null
  const record = value as Record<string, unknown>
  if (typeof record.id !== 'number' || !Number.isSafeInteger(record.id) || record.id <= 0) return null
  if (typeof record.fileName !== 'string' || !record.fileName.trim()) return null
  return { id: record.id, fileName: record.fileName.trim() }
}

export async function loadResumeOptions(signal: AbortSignal): Promise<ResumeOption[]> {
  const response = await send(`${backendUrl}/api/v1/resumes`, {
    headers: { Accept: 'application/json' },
    signal,
  })
  if (response.status === 401) {
    throw new ApplicationChangeError(
      'unauthenticated', 'Your session has expired. Sign in again to continue.')
  }
  if (!response.ok || response.redirected) {
    throw new ApplicationChangeError(
      'unavailable', 'We could not load your resumes. Please try again.')
  }
  const data: unknown = await response.json().catch(() => null)
  if (!Array.isArray(data)) {
    throw new ApplicationChangeError(
      'unavailable', 'We could not load your resumes. Please try again.')
  }
  const options = data.map(parseResumeOption)
  if (options.some((option) => option === null)) {
    throw new ApplicationChangeError(
      'unavailable', 'We could not load your resumes. Please try again.')
  }
  return options as ResumeOption[]
}

export async function uploadResume(file: File, signal: AbortSignal): Promise<ResumeOption> {
  const body = new FormData()
  body.append('file', file)
  const response = await send(`${backendUrl}/api/v1/resumes`, {
    method: 'POST',
    headers: { Accept: 'application/json' },
    body,
    signal,
  })
  if (response.status === 401) {
    throw new ApplicationChangeError(
      'unauthenticated', 'Your session has expired. Sign in again to continue.')
  }
  if (response.status === 400 || response.status === 413) {
    throw new ApplicationChangeError(
      'invalid', 'That file was not accepted. Use a PDF or DOCX file up to 5 MB.')
  }
  const uploaded = response.ok && !response.redirected
    ? parseResumeOption(await response.json().catch(() => null))
    : null
  if (!uploaded) {
    throw new ApplicationChangeError(
      'unavailable', 'The resume could not be uploaded. Please try again.')
  }
  return uploaded
}

export function changeErrorMessage(error: unknown): string {
  return error instanceof ApplicationChangeError
    ? error.message
    : 'Something went wrong. Please try again.'
}
