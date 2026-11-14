import { backendUrl } from '../constants'

// Task 10.2: send a resume file to the backend and receive AI-generated
// feedback. The backend owns the AI provider and its key; the browser only
// uploads the file and displays the structured result.

export type ResumeFeedback = {
  fileName: string
  summary: string
  strengths: string[]
  improvements: string[]
}

export type FeedbackErrorKind =
  | 'unauthenticated'
  | 'invalid-file'
  | 'unreadable'
  | 'busy'
  | 'not-configured'
  | 'not-available'
  | 'timeout'
  | 'failed'

export class ResumeFeedbackError extends Error {
  readonly kind: FeedbackErrorKind

  constructor(kind: FeedbackErrorKind, message: string) {
    super(message)
    this.name = 'ResumeFeedbackError'
    this.kind = kind
  }
}

// The page accepts a replacement so its screens can be previewed with
// fixtures, the same way the applications page accepts a loader.
export type FeedbackRequester = (file: File, signal: AbortSignal) => Promise<ResumeFeedback>

export const maxResumeBytes = 5 * 1024 * 1024
export const feedbackTimeoutMs = 90_000

export function resumeFileProblem(file: File): string | null {
  const name = file.name.toLowerCase()
  if (!name.endsWith('.pdf') && !name.endsWith('.docx')) {
    return 'Choose a PDF or DOCX file.'
  }
  if (file.size === 0) return 'That file is empty. Choose another file.'
  if (file.size > maxResumeBytes) return 'Choose a file that is 5 MB or smaller.'
  return null
}

export function formatFileSize(bytes: number): string {
  return bytes < 1024 * 1024
    ? `${Math.max(1, Math.round(bytes / 1024))} KB`
    : `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function textList(value: unknown): string[] | null {
  if (!Array.isArray(value)) return null
  const items: string[] = []
  for (const item of value) {
    if (typeof item !== 'string') return null
    if (item.trim()) items.push(item.trim())
  }
  return items
}

export function parseResumeFeedback(value: unknown, fallbackName: string): ResumeFeedback | null {
  if (typeof value !== 'object' || value === null) return null
  const record = value as Record<string, unknown>
  const strengths = textList(record.strengths)
  const improvements = textList(record.improvements)
  if (typeof record.summary !== 'string' || !record.summary.trim() || !strengths || !improvements) {
    return null
  }
  return {
    fileName: typeof record.fileName === 'string' && record.fileName.trim()
      ? record.fileName.trim()
      : fallbackName,
    summary: record.summary.trim(),
    strengths,
    improvements,
  }
}

function errorFor(status: number): ResumeFeedbackError {
  if (status === 401) {
    return new ResumeFeedbackError(
      'unauthenticated', 'Your session has expired. Sign in again to request feedback.')
  }
  if (status === 400 || status === 413 || status === 415) {
    return new ResumeFeedbackError(
      'invalid-file', 'That file was not accepted. Use a PDF or DOCX resume up to 5 MB.')
  }
  if (status === 422) {
    return new ResumeFeedbackError(
      'unreadable', 'We could not read any text in that file. If it is a scanned image, export a text-based PDF or DOCX and try again.')
  }
  if (status === 429) {
    return new ResumeFeedbackError(
      'busy', 'The AI service is busy right now. Wait a minute and try again.')
  }
  if (status === 503) {
    return new ResumeFeedbackError(
      'not-configured', 'AI feedback is not set up on this server yet. Please try again later.')
  }
  if (status === 404 || status === 405) {
    return new ResumeFeedbackError(
      'not-available', 'AI resume feedback is not available yet. Please check back soon.')
  }
  return new ResumeFeedbackError(
    'failed', 'The AI service could not produce feedback this time. Please try again.')
}

export const requestResumeFeedback: FeedbackRequester = async (file, signal) => {
  const body = new FormData()
  body.append('file', file)

  let response: Response
  try {
    response = await fetch(`${backendUrl}/api/v1/resumes/feedback`, {
      method: 'POST',
      credentials: 'include',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
      body,
      signal,
    })
  } catch (error) {
    if (signal.aborted) throw error
    throw new ResumeFeedbackError(
      'failed', 'We could not reach CareerConnect. Check your connection and try again.')
  }

  if (!response.ok || response.redirected) throw errorFor(response.redirected ? 401 : response.status)

  const feedback = parseResumeFeedback(await response.json().catch(() => null), file.name)
  if (!feedback) {
    throw new ResumeFeedbackError(
      'failed', 'The AI service returned feedback we could not read. Please try again.')
  }
  return feedback
}

export function feedbackErrorMessage(error: unknown): string {
  return error instanceof ResumeFeedbackError
    ? error.message
    : 'Something went wrong while requesting feedback. Please try again.'
}
