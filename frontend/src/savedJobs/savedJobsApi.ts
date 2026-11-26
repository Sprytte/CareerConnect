import { backendUrl } from '../constants'
import type { Job } from '../jobs/jobApi'

// Task 9.3: saved (favourite) jobs of the signed-in user. The backend derives
// the account from the session; the browser never sends a user ID.

export type SavedJob = {
  jobId: number
  savedAt: string | null // Local ISO date-time from the backend.
  job: Job | null // null when the posting was deleted after it was saved.
}

export type SavedJobsResult =
  | { status: 'ready'; savedJobs: SavedJob[] }
  | { status: 'unauthenticated' }
  | { status: 'forbidden' } // Saved jobs are for job seeker accounts only.
  | { status: 'unavailable' } // The backend does not provide saved jobs yet.

export type SavedJobErrorKind =
  | 'unauthenticated' | 'forbidden' | 'not-found' | 'closed' | 'unavailable' | 'failed'

export class SavedJobError extends Error {
  readonly kind: SavedJobErrorKind

  constructor(kind: SavedJobErrorKind, message: string) {
    super(message)
    this.name = 'SavedJobError'
    this.kind = kind
  }
}

const savedJobsUrl = `${backendUrl}/api/v1/account/saved-jobs`

function parseJob(value: unknown): Job | null {
  if (typeof value !== 'object' || value === null) return null
  const record = value as Record<string, unknown>
  if (typeof record.id !== 'number' || !Number.isSafeInteger(record.id)) return null
  if (typeof record.jobName !== 'string' || !record.jobName.trim()) return null
  const text = (field: string) => (typeof record[field] === 'string' ? (record[field] as string) : '')
  return {
    id: record.id,
    userId: text('userId'),
    jobName: text('jobName'),
    company: text('company'),
    description: text('description'),
    location: text('location'),
    category: text('category'),
    salary: text('salary'),
    dateCreated: text('dateCreated'),
    deadline: text('deadline'),
    archived: record.archived === true,
  }
}

function parseSavedJob(value: unknown): SavedJob | null {
  if (typeof value !== 'object' || value === null) return null
  const record = value as Record<string, unknown>
  if (typeof record.jobId !== 'number' || !Number.isSafeInteger(record.jobId) || record.jobId <= 0) return null
  const job = record.job == null ? null : parseJob(record.job)
  if (record.job != null && (job === null || job.id !== record.jobId)) return null
  return {
    jobId: record.jobId,
    savedAt: typeof record.savedAt === 'string' ? record.savedAt : null,
    job,
  }
}

export async function loadSavedJobs(signal: AbortSignal): Promise<SavedJobsResult> {
  const response = await fetch(savedJobsUrl, {
    credentials: 'include', headers: { Accept: 'application/json' }, cache: 'no-store', signal,
  })
  if (response.status === 401) return { status: 'unauthenticated' }
  if (response.status === 403) return { status: 'forbidden' }
  if (response.status === 404 || response.status === 405) return { status: 'unavailable' }
  if (!response.ok || response.redirected) throw new Error('Saved jobs unavailable')

  const data: unknown = await response.json()
  if (!Array.isArray(data)) throw new Error('Invalid saved jobs')
  const savedJobs = data.map(parseSavedJob)
  const ids = new Set(savedJobs.map((item) => item?.jobId))
  // A malformed or duplicated record must not become a misleading list.
  if (savedJobs.some((item) => item === null) || ids.size !== savedJobs.length) {
    throw new Error('Invalid saved job records')
  }
  return { status: 'ready', savedJobs: savedJobs as SavedJob[] }
}

async function change(jobId: number, method: 'PUT' | 'DELETE', signal?: AbortSignal): Promise<void> {
  let response: Response
  try {
    response = await fetch(`${savedJobsUrl}/${jobId}`, {
      method, credentials: 'include', headers: { Accept: 'application/json' }, cache: 'no-store', signal,
    })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new SavedJobError(
      'failed', 'We could not reach CareerConnect. Check your connection and try again.')
  }
  if (response.status === 401) {
    throw new SavedJobError('unauthenticated', 'Your session has expired. Sign in again to continue.')
  }
  if (response.status === 403) {
    throw new SavedJobError('forbidden', 'Saved jobs are only available to job seeker accounts.')
  }
  if (response.status === 400 && method === 'PUT') {
    // The backend refuses archived postings, like it does for applications.
    throw new SavedJobError('closed', 'This job posting is closed, so it can no longer be saved.')
  }
  if (response.status === 404 && method === 'PUT') {
    throw new SavedJobError('not-found', 'This job posting is no longer available, so it could not be saved.')
  }
  if (response.status === 404 || response.status === 405) {
    throw new SavedJobError('unavailable', 'Saved jobs are not available yet. Please check back soon.')
  }
  if (!response.ok || response.redirected) {
    throw new SavedJobError(
      'failed',
      method === 'PUT'
        ? 'We could not save this job. Please try again.'
        : 'We could not remove this job. Please try again.',
    )
  }
}

export function saveJob(jobId: number, signal?: AbortSignal): Promise<void> {
  return change(jobId, 'PUT', signal)
}

export function removeSavedJob(jobId: number, signal?: AbortSignal): Promise<void> {
  return change(jobId, 'DELETE', signal)
}

export function savedJobErrorMessage(error: unknown): string {
  return error instanceof SavedJobError
    ? error.message
    : 'Something went wrong. Please try again.'
}

export function isOpen(job: Job, now: number): boolean {
  if (job.archived) return false
  const deadline = new Date(job.deadline).getTime()
  return Number.isFinite(deadline) && deadline > now
}
