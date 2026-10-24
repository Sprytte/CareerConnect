import { backendUrl } from '../constants'

export type Job = {
  id: number
  userId: string
  jobName: string
  company: string
  description: string
  location: string
  category: string
  salary: string
  dateCreated: string
  deadline: string
}
export type JobInput = Omit<Job, 'id' | 'userId' | 'dateCreated'>

export async function jobRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${backendUrl}${path}`, {
    credentials: 'include',
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  if (!response.ok) {
    if (response.status === 409) throw new Error('You have already applied to this job.')
    if (response.status === 401) throw new Error('Please sign in and try again.')
    if (response.status === 403) throw new Error('You do not have permission to do that.')
    if (response.status === 404) throw new Error('The requested job was not found.')
    throw new Error('Something went wrong. Please try again.')
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
