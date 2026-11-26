import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { loginUrl, useSession } from '../auth/session'
import SiteHeader from '../components/SiteHeader'
import SiteFooter from '../components/SiteFooter'
import {
  isOpen, loadSavedJobs, removeSavedJob, SavedJobError, savedJobErrorMessage,
} from '../savedJobs/savedJobsApi'
import type { SavedJob, SavedJobsResult } from '../savedJobs/savedJobsApi'
import '../styles/App.css'
import '../styles/SavedJobs.css'

// Task 9.3: a page where a job seeker views and removes their saved jobs.

type LoadState = SavedJobsResult | { status: 'loading' } | { status: 'error' }
type Feedback = { kind: 'status' | 'error'; message: string; sessionExpired?: boolean }

function formatDay(value: string | null): string | null {
  const date = value ? new Date(value) : null
  if (!date || Number.isNaN(date.getTime())) return null
  return new Intl.DateTimeFormat('en-CA', { month: 'short', day: 'numeric', year: 'numeric' }).format(date)
}

function Notice({ title, children, action, error = false }: {
  title: string
  children: ReactNode
  action?: ReactNode
  error?: boolean
}) {
  return (
    <div className="saved-notice" role={error ? 'alert' : 'status'}>
      <span className="saved-notice-mark" aria-hidden="true">{error ? '!' : '☆'}</span>
      <h2>{title}</h2>
      <p>{children}</p>
      {action && <div className="saved-notice-actions">{action}</div>}
    </div>
  )
}

function SavedJobCard({ saved, now, removing, disabled, onRemove }: {
  saved: SavedJob
  now: number
  removing: boolean
  disabled: boolean
  onRemove: () => void
}) {
  const [expanded, setExpanded] = useState(false)
  const { job } = saved
  const savedOn = formatDay(saved.savedAt)
  const title = job ? job.jobName : 'Job posting no longer available'

  return (
    <li className="saved-card">
      <div className="saved-card-main">
        {job ? (
          <>
            {job.category && <span className="saved-category">{job.category}</span>}
            <h3>{job.jobName}</h3>
            <p className="saved-details">
              {[job.company, job.location, job.salary].filter(Boolean).join(' · ')}
            </p>
            {job.description && (
              <>
                <p className={`saved-description${expanded ? ' saved-description--expanded' : ''}`}>
                  {job.description}
                </p>
                <button
                  className="saved-link-button"
                  type="button"
                  aria-expanded={expanded}
                  onClick={() => setExpanded((value) => !value)}
                >
                  {expanded ? 'Hide details' : 'View details'}
                </button>
              </>
            )}
          </>
        ) : (
          <>
            <h3>{title}</h3>
            <p className="saved-details">
              The employer removed this posting after you saved it. You can remove it from your list.
            </p>
          </>
        )}
        <p className="saved-dates">
          {savedOn && <>Saved <time dateTime={saved.savedAt!}>{savedOn}</time></>}
          {job && formatDay(job.deadline) && (
            <>
              {savedOn && <span aria-hidden="true"> · </span>}
              Deadline <time dateTime={job.deadline}>{formatDay(job.deadline)}</time>
            </>
          )}
        </p>
      </div>

      <div className="saved-card-actions">
        {job && (isOpen(job, now)
          ? <Link className="saved-button" to={`/jobs/${job.id}/apply`}>Apply now</Link>
          : <span className="saved-closed">Applications closed</span>)}
        <button
          className="saved-button saved-button--plain"
          type="button"
          aria-label={`Remove ${title} from saved jobs`}
          disabled={removing || disabled}
          onClick={onRemove}
        >
          {removing ? 'Removing…' : 'Remove'}
        </button>
      </div>
    </li>
  )
}

function SavedJobsData() {
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [now, setNow] = useState(() => Date.now())
  const [removingId, setRemovingId] = useState<number | null>(null)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const active = useRef(true)

  useEffect(() => {
    active.current = true
    return () => { active.current = false }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    loadSavedJobs(controller.signal).then((result) => {
      if (controller.signal.aborted) return
      setNow(Date.now())
      setState(result)
    }).catch(() => {
      if (!controller.signal.aborted) setState({ status: 'error' })
    })
    return () => controller.abort()
  }, [attempt])

  function refresh() {
    setFeedback(null)
    setState({ status: 'loading' })
    setAttempt((value) => value + 1)
  }

  async function remove(saved: SavedJob) {
    if (removingId !== null) return
    const title = saved.job ? saved.job.jobName : 'The job'
    setRemovingId(saved.jobId)
    setFeedback(null)
    try {
      await removeSavedJob(saved.jobId)
      if (!active.current) return
      // Drop the card only after the server confirms the removal.
      setState((current) => current.status !== 'ready' ? current : {
        status: 'ready',
        savedJobs: current.savedJobs.filter((item) => item.jobId !== saved.jobId),
      })
      setFeedback({ kind: 'status', message: `${title} was removed from your saved jobs.` })
    } catch (error) {
      if (!active.current) return
      setFeedback({
        kind: 'error',
        message: savedJobErrorMessage(error),
        sessionExpired: error instanceof SavedJobError && error.kind === 'unauthenticated',
      })
    } finally {
      if (active.current) setRemovingId(null)
    }
  }

  if (state.status === 'loading') {
    return <Notice title="Loading your saved jobs">Getting your list…</Notice>
  }
  if (state.status === 'unauthenticated') {
    return <Notice title="Please sign in again" action={<a className="saved-button" href={loginUrl}>Sign in</a>}>
      Your session has expired. Sign in again to view your saved jobs.
    </Notice>
  }
  if (state.status === 'forbidden') {
    return <Notice title="Saved jobs are for job seekers" action={<Link className="saved-button" to="/jobs">Find jobs</Link>}>
      This account is not a job seeker account, so it cannot keep a list of saved jobs.
    </Notice>
  }
  if (state.status === 'unavailable') {
    return (
      <Notice title="Saved jobs are coming soon" action={
        <button className="saved-button" type="button" onClick={refresh}>Check again</button>
      }>
        You will be able to keep the jobs you are interested in here once saving is available.
      </Notice>
    )
  }
  if (state.status === 'error') {
    return <Notice title="We couldn’t load your saved jobs" error action={
      <button className="saved-button" type="button" onClick={refresh}>Try again</button>
    }>
      Please check your connection and try again in a moment.
    </Notice>
  }

  const count = state.savedJobs.length
  return (
    <section aria-label="Saved jobs">
      <div className="saved-results-top">
        <p role="status">{count} saved {count === 1 ? 'job' : 'jobs'}</p>
        <button className="saved-refresh" type="button" onClick={refresh}>
          <span aria-hidden="true">↻</span> Refresh
        </button>
      </div>

      {feedback && (
        <p className={`saved-feedback saved-feedback--${feedback.kind}`} role={feedback.kind === 'error' ? 'alert' : 'status'}>
          {feedback.message}
          {feedback.sessionExpired && <> <a href={loginUrl}>Sign in</a></>}
        </p>
      )}

      {count === 0 ? (
        <Notice title="No saved jobs yet" action={<Link className="saved-button" to="/jobs">Browse jobs</Link>}>
          Use “Save job” on a posting to keep it here and come back to it later.
        </Notice>
      ) : (
        <ul className="saved-list" aria-label="Your saved jobs">
          {state.savedJobs.map((saved) => (
            <SavedJobCard
              key={saved.jobId}
              saved={saved}
              now={now}
              removing={removingId === saved.jobId}
              disabled={removingId !== null}
              onRemove={() => remove(saved)}
            />
          ))}
        </ul>
      )}
    </section>
  )
}

export default function SavedJobs() {
  const session = useSession()
  return (
    <div className="site-shell saved-shell">
      <SiteHeader />
      <main className="saved-page" id="main-content">
        <nav className="saved-nav" aria-label="Account navigation">
          <span aria-current="page">Saved jobs</span>
          {!(session.status === 'signed-in' && session.user.role === 'RECRUITER') && (
            <Link to="/account/applications">Applications</Link>
          )}
          <Link to="/jobs">Find jobs</Link>
        </nav>
        <div className="saved-title">
          <p className="section-kicker">KEEP AN EYE ON THEM</p>
          <h1>Saved jobs<span aria-hidden="true">.</span></h1>
          <p>The postings you saved, ready when you are ready to apply.</p>
        </div>

        {session.status === 'loading' && <Notice title="Checking your account">Please wait while we check your session…</Notice>}
        {session.status === 'signed-out' && <Notice title="Sign in to view your saved jobs" action={
          <a className="saved-button" href={loginUrl}>Sign in</a>
        }>
          Save the jobs you are interested in and find them here later.
        </Notice>}
        {session.status === 'unavailable' && <Notice title="We couldn’t check your account" error action={
          <button className="saved-button" type="button" onClick={() => window.location.reload()}>Try again</button>
        }>
          Please check your connection and try again.
        </Notice>}
        {session.status === 'signed-in' && session.user.role === 'RECRUITER' && (
          <Notice title="Saved jobs are for job seekers" action={
            <Link className="saved-button" to="/account/jobs">Go to my postings</Link>
          }>
            Recruiter accounts manage their postings instead of saving jobs.
          </Notice>
        )}
        {session.status === 'signed-in' && session.user.role !== 'RECRUITER' && (
          <SavedJobsData key={session.user.userId} />
        )}
      </main>
      <SiteFooter />
    </div>
  )
}
