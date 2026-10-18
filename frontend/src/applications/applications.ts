export const applicationStatuses = ['Applied', 'Interview', 'Offered', 'Rejected'] as const

export type ApplicationStatus = (typeof applicationStatuses)[number]

// Internal UI model, not an agreed backend response. The future adapter must
// map authenticated application records into this shape.
export type JobApplication = {
  id: string
  jobTitle: string
  company: string
  status: string | null
  appliedOn?: string | null // Calendar date, YYYY-MM-DD, when available.
}

export type ApplicationsResult =
  | { status: 'ready'; applications: JobApplication[] }
  | { status: 'unavailable' }
  | { status: 'unauthenticated' }
  | { status: 'forbidden' }

export type ApplicationsLoader = (signal: AbortSignal) => Promise<ApplicationsResult>

export function isApplicationStatus(value: string | null): value is ApplicationStatus {
  return applicationStatuses.some((status) => status === value)
}

// Validate adapter results before displaying counts or records. A malformed
// response must not become a misleading empty list or a successful status.
export function isApplicationsResult(value: unknown): value is ApplicationsResult {
  if (typeof value !== 'object' || value === null) return false
  const result = value as Record<string, unknown>
  if (result.status === 'unavailable' || result.status === 'unauthenticated' || result.status === 'forbidden') {
    return true
  }
  if (result.status !== 'ready' || !Array.isArray(result.applications)) return false

  const ids = new Set<string>()
  return result.applications.every((item: unknown) => {
    if (typeof item !== 'object' || item === null) return false
    const application = item as Record<string, unknown>
    for (const field of ['id', 'jobTitle', 'company']) {
      if (typeof application[field] !== 'string' || !application[field].trim()) return false
    }
    const id = application.id as string
    if (ids.has(id)) return false
    ids.add(id)
    return (application.status === null || typeof application.status === 'string')
      && (application.appliedOn == null || typeof application.appliedOn === 'string')
  })
}

export function formatAppliedDate(value: string | null | undefined): string | null {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null
  const date = new Date(`${value}T00:00:00Z`)
  if (Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== value) return null
  return new Intl.DateTimeFormat('en-CA', {
    month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC',
  }).format(date)
}
