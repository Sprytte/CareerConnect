import { useCallback, useEffect, useRef, useState } from 'react'
import type { Job } from '../jobs/jobApi'
import {
  loadSavedJobs, removeSavedJob, SavedJobError, savedJobErrorMessage, saveJob,
} from './savedJobsApi'

// Lets a job list show and change which postings the signed-in user has saved.

export type SavedJobsNotice = { kind: 'status' | 'error'; message: string; sessionExpired?: boolean }

export type SavedJobsControl = {
  // 'off' covers signed-out visitors and a backend without saved jobs.
  status: 'off' | 'loading' | 'ready' | 'error'
  isSaved: (jobId: number) => boolean
  pendingId: number | null
  notice: SavedJobsNotice | null
  toggle: (job: Pick<Job, 'id' | 'jobName'>) => void
  retry: () => void
}

type Loaded =
  | { key: number; status: 'ready'; ids: ReadonlySet<number> }
  | { key: number; status: 'off' | 'error' }

export function useSavedJobs(enabled: boolean): SavedJobsControl {
  const [attempt, setAttempt] = useState(0)
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [pendingId, setPendingId] = useState<number | null>(null)
  const [notice, setNotice] = useState<SavedJobsNotice | null>(null)
  const mounted = useRef(true)

  useEffect(() => {
    mounted.current = true
    return () => { mounted.current = false }
  }, [])

  useEffect(() => {
    if (!enabled) return
    const controller = new AbortController()
    loadSavedJobs(controller.signal).then((result) => {
      if (controller.signal.aborted) return
      setLoaded(result.status === 'ready'
        ? { key: attempt, status: 'ready', ids: new Set(result.savedJobs.map((item) => item.jobId)) }
        : { key: attempt, status: 'off' })
    }).catch(() => {
      if (!controller.signal.aborted) setLoaded({ key: attempt, status: 'error' })
    })
    return () => controller.abort()
  }, [enabled, attempt])

  // A result from an earlier attempt or session is ignored rather than cleared
  // in an effect, so the status follows the inputs without an extra render.
  const current = enabled && loaded?.key === attempt ? loaded : null
  const status: SavedJobsControl['status'] = !enabled ? 'off' : current ? current.status : 'loading'
  const ids = current?.status === 'ready' ? current.ids : null

  const isSaved = useCallback((jobId: number) => ids?.has(jobId) ?? false, [ids])

  const toggle = useCallback((job: Pick<Job, 'id' | 'jobName'>) => {
    if (!ids || pendingId !== null) return
    const wasSaved = ids.has(job.id)
    setPendingId(job.id)
    setNotice(null)
    ;(wasSaved ? removeSavedJob(job.id) : saveJob(job.id)).then(() => {
      if (!mounted.current) return
      setLoaded((value) => {
        if (value?.status !== 'ready') return value
        const next = new Set(value.ids)
        if (wasSaved) next.delete(job.id)
        else next.add(job.id)
        return { ...value, ids: next }
      })
      setNotice({
        kind: 'status',
        message: wasSaved ? `Removed ${job.jobName} from your saved jobs.` : `Saved ${job.jobName}.`,
      })
    }).catch((error: unknown) => {
      if (!mounted.current) return
      setNotice({
        kind: 'error',
        message: savedJobErrorMessage(error),
        sessionExpired: error instanceof SavedJobError && error.kind === 'unauthenticated',
      })
    }).finally(() => {
      if (mounted.current) setPendingId(null)
    })
  }, [ids, pendingId])

  const retry = useCallback(() => setAttempt((value) => value + 1), [])

  return { status, isSaved, pendingId, notice, toggle, retry }
}
