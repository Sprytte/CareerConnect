import { backendUrl } from '../constants'
import { isApplicationsResult } from './applications'
import type { ApplicationsLoader } from './applications'

export const loadApplications: ApplicationsLoader = async (signal) => {
  const response = await fetch(`${backendUrl}/api/v1/account/applications`, {
    credentials: 'include', headers: { Accept: 'application/json' }, cache: 'no-store', signal,
  })
  if (response.status === 401) return { status: 'unauthenticated' }
  if (response.status === 403) return { status: 'forbidden' }
  if (!response.ok || response.redirected) throw new Error('Application list unavailable')
  const data: unknown = await response.json()
  if (!Array.isArray(data)) throw new Error('Invalid application list')
  const applications = data.map((item: unknown) => {
    if (typeof item !== 'object' || item === null) return item
    const record = item as Record<string, unknown>
    return { ...record, id: typeof record.id === 'number' && Number.isSafeInteger(record.id) && record.id > 0 ? String(record.id) : record.id }
  })
  const result = { status: 'ready', applications }
  if (!isApplicationsResult(result)) throw new Error('Invalid application records')
  return result
}
