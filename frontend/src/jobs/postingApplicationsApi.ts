import { backendUrl } from '../constants'
import type { Job } from './jobApi'
import { normalizeStatus } from './postingApplicationsView'
import type { PostingEntry, ReceivedApplication, ReceivedState } from './postingApplicationsView'

// Reads the signed-in recruiter's postings and the applications received on
// each one. Both endpoints derive the owner from the session.

export type PostingApplicationsResult =
  | { status: 'ready'; entries: PostingEntry[] }
  | { status: 'unauthenticated' }
  | { status: 'forbidden' }

class SessionExpired extends Error {}

function get(path: string, signal: AbortSignal): Promise<Response> {
  return fetch(`${backendUrl}${path}`, {
    credentials: 'include', headers: { Accept: 'application/json' }, cache: 'no-store', signal,
  })
}

function parseJob(value: unknown): Job | null {
  if (typeof value !== 'object' || value === null) return null
  const record = value as Record<string, unknown>
  if (typeof record.id !== 'number' || !Number.isSafeInteger(record.id)) return null
  for (const field of ['jobName', 'company', 'dateCreated', 'deadline']) {
    if (typeof record[field] !== 'string' || !(record[field] as string).trim()) return null
  }
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
  }
}

function parseApplication(value: unknown): ReceivedApplication | null {
  if (typeof value !== 'object' || value === null) return null
  const record = value as Record<string, unknown>
  if (typeof record.id !== 'number' || !Number.isSafeInteger(record.id)) return null
  return {
    id: record.id,
    status: normalizeStatus(record.status),
    appliedAt: typeof record.appliedAt === 'string' ? record.appliedAt : null,
  }
}

// One posting's applications. A failure here is reported on that posting only,
// except an expired session, which affects the whole page.
export async function loadReceivedApplications(
  jobId: number,
  signal: AbortSignal,
): Promise<ReceivedState> {
  try {
    const response = await get(`/api/v1/account/job-postings/${jobId}/applications`, signal)
    if (response.status === 401) throw new SessionExpired()
    if (!response.ok || response.redirected) return { status: 'error' }
    const data: unknown = await response.json()
    if (!Array.isArray(data)) return { status: 'error' }
    const applications = data.map(parseApplication)
    if (applications.some((application) => application === null)) return { status: 'error' }
    return { status: 'ready', applications: applications as ReceivedApplication[] }
  } catch (error) {
    if (error instanceof SessionExpired) throw error
    if (signal.aborted) throw error
    return { status: 'error' }
  }
}

export async function loadPostingApplications(
  signal: AbortSignal,
): Promise<PostingApplicationsResult> {
  const response = await get('/api/v1/account/job-postings', signal)
  if (response.status === 401) return { status: 'unauthenticated' }
  if (response.status === 403) return { status: 'forbidden' }
  if (!response.ok || response.redirected) throw new Error('Job postings unavailable')

  const data: unknown = await response.json()
  if (!Array.isArray(data)) throw new Error('Invalid job postings')
  const jobs = data.map(parseJob)
  if (jobs.some((job) => job === null)) throw new Error('Invalid job posting records')

  try {
    const entries = await Promise.all((jobs as Job[]).map(async (job) => ({
      job,
      received: await loadReceivedApplications(job.id, signal),
    })))
    return { status: 'ready', entries }
  } catch (error) {
    if (error instanceof SessionExpired) return { status: 'unauthenticated' }
    throw error
  }
}

export function isSessionExpired(error: unknown): boolean {
  return error instanceof SessionExpired
}
