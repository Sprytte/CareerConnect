// Explicit .ts extensions let Node run this module directly in the unit tests.
import { applicationStatuses } from '../applications/applications.ts'
import type { ApplicationStatus } from '../applications/applications.ts'
import type { Job } from './jobApi.ts'

// Task 11.3: filtering and sorting for the recruiter's "applications received"
// page. Everything here is a pure function of the loaded data so the rules can
// be checked without a browser.

export type ReceivedApplication = {
  id: number
  status: string | null // One of applicationStatuses when recognised.
  appliedAt: string | null // Local ISO date-time from the backend.
}

export type ReceivedState =
  | { status: 'ready'; applications: ReceivedApplication[] }
  | { status: 'error' }

export type PostingEntry = { job: Job; received: ReceivedState }

export const postingStatuses = ['Open', 'Closed'] as const
export type PostingStatus = (typeof postingStatuses)[number]

export type DateField = 'posted' | 'deadline' | 'applied'

export type Filters = {
  postingStatus: 'All' | PostingStatus
  applicationStatus: 'All' | ApplicationStatus
  dateField: DateField
  from: string // YYYY-MM-DD or ''
  to: string // YYYY-MM-DD or ''
}

export const postingSorts = [
  { value: 'posted-desc', label: 'Date posted: newest first' },
  { value: 'posted-asc', label: 'Date posted: oldest first' },
  { value: 'deadline-asc', label: 'Deadline: soonest first' },
  { value: 'deadline-desc', label: 'Deadline: latest first' },
  { value: 'status-open', label: 'Status: open postings first' },
  { value: 'status-closed', label: 'Status: closed postings first' },
  { value: 'applications-desc', label: 'Most applications first' },
] as const
export type PostingSort = (typeof postingSorts)[number]['value']

export const applicationSorts = [
  { value: 'applied-desc', label: 'Date applied: newest first' },
  { value: 'applied-asc', label: 'Date applied: oldest first' },
  { value: 'status', label: 'Status: Applied to Rejected' },
] as const
export type ApplicationSort = (typeof applicationSorts)[number]['value']

export type Sorting = { postings: PostingSort; applications: ApplicationSort }

export const defaultFilters: Filters = {
  postingStatus: 'All', applicationStatus: 'All', dateField: 'posted', from: '', to: '',
}
export const defaultSorting: Sorting = { postings: 'posted-desc', applications: 'applied-desc' }

export type VisiblePosting = {
  job: Job
  status: PostingStatus
  // null when this posting's applications could not be loaded.
  total: number | null
  applications: ReceivedApplication[]
}

export type View = {
  postings: VisiblePosting[]
  totalPostings: number
  visibleApplications: number
  totalApplications: number
}

// The recruiter endpoint returns stored values, including older ones.
export function normalizeStatus(value: unknown): string | null {
  if (typeof value !== 'string' || !value.trim()) return null
  switch (value.trim().toUpperCase()) {
    case 'SUBMITTED':
    case 'APPLIED': return 'Applied'
    case 'INTERVIEW': return 'Interview'
    case 'OFFER':
    case 'OFFERED': return 'Offered'
    case 'REJECT':
    case 'REJECTED': return 'Rejected'
    default: return value.trim()
  }
}

// A posting accepts applications until its deadline. An unreadable deadline
// is treated as closed, matching what an applicant would be able to do.
export function postingStatus(job: Job, now: number): PostingStatus {
  const deadline = new Date(job.deadline).getTime()
  return Number.isFinite(deadline) && deadline > now ? 'Open' : 'Closed'
}

function isDay(value: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(value)
}

export function dateRangeProblem(filters: Pick<Filters, 'from' | 'to'>): string | null {
  if (filters.from && !isDay(filters.from)) return 'Enter a valid start date.'
  if (filters.to && !isDay(filters.to)) return 'Enter a valid end date.'
  if (filters.from && filters.to && filters.from > filters.to) {
    return 'The start date must be on or before the end date.'
  }
  return null
}

export function hasActiveFilters(filters: Filters): boolean {
  return filters.postingStatus !== 'All' || filters.applicationStatus !== 'All'
    || filters.from !== '' || filters.to !== ''
}

// Backend date-times carry no zone, so their first ten characters are the
// calendar day the recruiter saw when the record was created.
function inRange(value: string | null, filters: Filters): boolean {
  if (!filters.from && !filters.to) return true
  const day = typeof value === 'string' ? value.slice(0, 10) : ''
  if (!isDay(day)) return false
  return (!filters.from || day >= filters.from) && (!filters.to || day <= filters.to)
}

function time(value: string | null): number {
  const parsed = value ? new Date(value).getTime() : Number.NaN
  return Number.isFinite(parsed) ? parsed : 0
}

function statusRank(status: string | null): number {
  const index = applicationStatuses.findIndex((known) => known === status)
  return index === -1 ? applicationStatuses.length : index
}

function sortApplications(items: ReceivedApplication[], sort: ApplicationSort): ReceivedApplication[] {
  return [...items].sort((a, b) => {
    if (sort === 'status') {
      const byStatus = statusRank(a.status) - statusRank(b.status)
      if (byStatus !== 0) return byStatus
    }
    const byDate = time(a.appliedAt) - time(b.appliedAt)
    if (byDate !== 0) return sort === 'applied-asc' ? byDate : -byDate
    return b.id - a.id
  })
}

function comparePostings(a: VisiblePosting, b: VisiblePosting, sort: PostingSort): number {
  const newestFirst = time(b.job.dateCreated) - time(a.job.dateCreated) || b.job.id - a.job.id
  switch (sort) {
    case 'posted-asc': return -newestFirst
    case 'deadline-asc': return time(a.job.deadline) - time(b.job.deadline) || newestFirst
    case 'deadline-desc': return time(b.job.deadline) - time(a.job.deadline) || newestFirst
    case 'status-open': return Number(b.status === 'Open') - Number(a.status === 'Open') || newestFirst
    case 'status-closed': return Number(a.status === 'Open') - Number(b.status === 'Open') || newestFirst
    case 'applications-desc': return (b.total ?? -1) - (a.total ?? -1) || newestFirst
    default: return newestFirst
  }
}

export function buildView(
  entries: PostingEntry[],
  filters: Filters,
  sorting: Sorting,
  now: number,
): View {
  // An invalid range is reported next to the inputs and not applied.
  const dated = dateRangeProblem(filters) === null
    ? filters
    : { ...filters, from: '', to: '' }
  const applicationDate = dated.dateField === 'applied' && (dated.from !== '' || dated.to !== '')
  const narrowsApplications = dated.applicationStatus !== 'All' || applicationDate

  const postings: VisiblePosting[] = []
  let totalApplications = 0

  for (const entry of entries) {
    const status = postingStatus(entry.job, now)
    const all = entry.received.status === 'ready' ? entry.received.applications : null
    if (all) totalApplications += all.length

    if (dated.postingStatus !== 'All' && status !== dated.postingStatus) continue
    if (dated.dateField === 'posted' && !inRange(entry.job.dateCreated, dated)) continue
    if (dated.dateField === 'deadline' && !inRange(entry.job.deadline, dated)) continue

    const matching = (all ?? []).filter((application) =>
      (dated.applicationStatus === 'All' || application.status === dated.applicationStatus)
      && (!applicationDate || inRange(application.appliedAt, dated)))

    // When the filters are about applications, a posting with none that match
    // is noise. A posting whose applications failed to load stays visible so
    // the failure is not silently hidden.
    if (narrowsApplications && all && matching.length === 0) continue

    postings.push({
      job: entry.job,
      status,
      total: all ? all.length : null,
      applications: sortApplications(matching, sorting.applications),
    })
  }

  postings.sort((a, b) => comparePostings(a, b, sorting.postings))

  return {
    postings,
    totalPostings: entries.length,
    visibleApplications: postings.reduce((sum, posting) => sum + posting.applications.length, 0),
    totalApplications,
  }
}
