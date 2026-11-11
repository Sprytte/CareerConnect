import { useEffect, useId, useMemo, useRef, useState } from 'react'
import type { KeyboardEvent, ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { loginUrl, useSession } from '../auth/session'
import { applicationStatuses } from '../applications/applications'
import ApplicationStatusBadge from '../components/ApplicationStatusBadge'
import SiteHeader from '../components/SiteHeader'
import SiteFooter from '../components/SiteFooter'
import {
  isSessionExpired, loadPostingApplications, loadReceivedApplications,
} from './postingApplicationsApi'
import type { PostingApplicationsResult } from './postingApplicationsApi'
import {
  applicationSorts, buildView, dateRangeProblem, defaultFilters, defaultSorting,
  hasActiveFilters, postingSorts, postingStatuses,
} from './postingApplicationsView'
import type {
  ApplicationSort, DateField, Filters, PostingEntry, PostingSort, Sorting, VisiblePosting,
} from './postingApplicationsView'
import '../styles/App.css'
import '../styles/Jobs.css'
import '../styles/Applications.css'
import '../styles/PostingApplications.css'

// Task 11.3: a recruiter's view of their job postings and the applications
// received on each, with a Filter tab and a Sort tab.

type LoadState = PostingApplicationsResult | { status: 'loading' } | { status: 'error' }
type Tab = 'filter' | 'sort'

const dateFields: { value: DateField; label: string }[] = [
  { value: 'posted', label: 'Date posted' },
  { value: 'deadline', label: 'Deadline' },
  { value: 'applied', label: 'Date applied' },
]

function formatDay(value: string | null): string {
  const date = value ? new Date(value) : null
  if (!date || Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat('en-CA', { month: 'short', day: 'numeric', year: 'numeric' }).format(date)
}

function formatMoment(value: string | null): string {
  const date = value ? new Date(value) : null
  if (!date || Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat('en-CA', {
    month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit',
  }).format(date)
}

function count(value: number, noun: string): string {
  return `${value} ${noun}${value === 1 ? '' : 's'}`
}

function Notice({ title, children, action, error = false }: {
  title: string
  children: ReactNode
  action?: ReactNode
  error?: boolean
}) {
  return (
    <div className="applications-notice" role={error ? 'alert' : 'status'}>
      <span className="applications-notice-mark" aria-hidden="true">{error ? '!' : '↗'}</span>
      <h2>{title}</h2>
      <p>{children}</p>
      {action && <div className="applications-notice-actions">{action}</div>}
    </div>
  )
}

function Toolbar({ filters, sorting, onFilters, onSorting }: {
  filters: Filters
  sorting: Sorting
  onFilters: (filters: Filters) => void
  onSorting: (sorting: Sorting) => void
}) {
  const id = useId()
  const [tab, setTab] = useState<Tab>('filter')
  const tabs = useRef<Record<Tab, HTMLButtonElement | null>>({ filter: null, sort: null })
  const rangeProblem = dateRangeProblem(filters)
  const active = hasActiveFilters(filters)

  function onTabKey(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
    event.preventDefault()
    const next: Tab = tab === 'filter' ? 'sort' : 'filter'
    setTab(next)
    tabs.current[next]?.focus()
  }

  return (
    <div className="posting-toolbar">
      <div className="posting-tabs" role="tablist" aria-label="Filter and sort job postings">
        {(['filter', 'sort'] as const).map((name) => (
          <button
            key={name}
            ref={(element) => { tabs.current[name] = element }}
            id={`${id}-${name}-tab`}
            className="posting-tab"
            type="button"
            role="tab"
            aria-selected={tab === name}
            aria-controls={`${id}-${name}-panel`}
            tabIndex={tab === name ? 0 : -1}
            onClick={() => setTab(name)}
            onKeyDown={onTabKey}
          >
            {name === 'filter' ? 'Filter' : 'Sort'}
            {name === 'filter' && active && <span className="posting-tab-flag">On</span>}
          </button>
        ))}
      </div>

      <div
        id={`${id}-filter-panel`}
        className="posting-panel"
        role="tabpanel"
        aria-labelledby={`${id}-filter-tab`}
        hidden={tab !== 'filter'}
      >
        <div className="posting-fields">
          <label className="posting-field">
            <span>Posting status</span>
            <select
              value={filters.postingStatus}
              onChange={(event) => onFilters({ ...filters, postingStatus: event.target.value as Filters['postingStatus'] })}
            >
              <option value="All">All postings</option>
              {postingStatuses.map((status) => <option key={status} value={status}>{status}</option>)}
            </select>
          </label>
          <label className="posting-field">
            <span>Application status</span>
            <select
              value={filters.applicationStatus}
              onChange={(event) => onFilters({ ...filters, applicationStatus: event.target.value as Filters['applicationStatus'] })}
            >
              <option value="All">All applications</option>
              {applicationStatuses.map((status) => <option key={status} value={status}>{status}</option>)}
            </select>
          </label>
          <label className="posting-field">
            <span>Date to filter</span>
            <select
              value={filters.dateField}
              onChange={(event) => onFilters({ ...filters, dateField: event.target.value as DateField })}
            >
              {dateFields.map((field) => <option key={field.value} value={field.value}>{field.label}</option>)}
            </select>
          </label>
          <label className="posting-field">
            <span>From</span>
            <input
              type="date"
              value={filters.from}
              max={filters.to || undefined}
              aria-invalid={rangeProblem ? true : undefined}
              aria-describedby={rangeProblem ? `${id}-range-problem` : undefined}
              onChange={(event) => onFilters({ ...filters, from: event.target.value })}
            />
          </label>
          <label className="posting-field">
            <span>To</span>
            <input
              type="date"
              value={filters.to}
              min={filters.from || undefined}
              aria-invalid={rangeProblem ? true : undefined}
              aria-describedby={rangeProblem ? `${id}-range-problem` : undefined}
              onChange={(event) => onFilters({ ...filters, to: event.target.value })}
            />
          </label>
          <button
            className="posting-clear"
            type="button"
            disabled={!active}
            onClick={() => onFilters(defaultFilters)}
          >
            Clear filters
          </button>
        </div>
        {rangeProblem && (
          <p className="posting-field-error" id={`${id}-range-problem`} role="alert">
            {rangeProblem} The date filter is not applied until this is fixed.
          </p>
        )}
      </div>

      <div
        id={`${id}-sort-panel`}
        className="posting-panel"
        role="tabpanel"
        aria-labelledby={`${id}-sort-tab`}
        hidden={tab !== 'sort'}
      >
        <div className="posting-fields">
          <label className="posting-field posting-field--wide">
            <span>Sort job postings by</span>
            <select
              value={sorting.postings}
              onChange={(event) => onSorting({ ...sorting, postings: event.target.value as PostingSort })}
            >
              {postingSorts.map((sort) => <option key={sort.value} value={sort.value}>{sort.label}</option>)}
            </select>
          </label>
          <label className="posting-field posting-field--wide">
            <span>Sort applications by</span>
            <select
              value={sorting.applications}
              onChange={(event) => onSorting({ ...sorting, applications: event.target.value as ApplicationSort })}
            >
              {applicationSorts.map((sort) => <option key={sort.value} value={sort.value}>{sort.label}</option>)}
            </select>
          </label>
        </div>
      </div>
    </div>
  )
}

function PostingCard({ posting, narrowed, retrying, onRetry }: {
  posting: VisiblePosting
  narrowed: boolean
  retrying: boolean
  onRetry: () => void
}) {
  const id = useId()
  const [open, setOpen] = useState(true)
  const { job, applications, total } = posting
  const summary = total === null
    ? 'Applications unavailable'
    : narrowed && applications.length !== total
      ? `${applications.length} of ${count(total, 'application')} shown`
      : count(total, 'application')

  return (
    <li className="posting-card">
      <div className="posting-card-top">
        <div className="posting-card-main">
          <span className="posting-company">{job.company}{job.location ? ` · ${job.location}` : ''}</span>
          <h3>{job.jobName}</h3>
          <p className="posting-dates">
            Posted <time dateTime={job.dateCreated}>{formatDay(job.dateCreated)}</time>
            <span aria-hidden="true"> · </span>
            Deadline <time dateTime={job.deadline}>{formatMoment(job.deadline)}</time>
          </p>
        </div>
        <span className={`posting-status posting-status--${posting.status.toLowerCase()}`}>
          <span className="application-status-dot" aria-hidden="true" />
          {posting.status}
        </span>
      </div>

      <div className="posting-received">
        <button
          className="posting-toggle"
          type="button"
          aria-expanded={open}
          aria-controls={`${id}-applications`}
          onClick={() => setOpen((value) => !value)}
        >
          <span aria-hidden="true">{open ? '▾' : '▸'}</span> {summary}
        </button>

        <div id={`${id}-applications`} hidden={!open}>
          {total === null ? (
            <p className="posting-received-error" role="alert">
              The applications for this posting could not be loaded.{' '}
              <button className="posting-inline-button" type="button" onClick={onRetry} disabled={retrying}>
                {retrying ? 'Trying again…' : 'Try again'}
              </button>
            </p>
          ) : total === 0 ? (
            <p className="posting-received-empty">No one has applied to this posting yet.</p>
          ) : applications.length === 0 ? (
            <p className="posting-received-empty">No applications on this posting match the current filters.</p>
          ) : (
            <ul className="posting-application-list" aria-label={`Applications for ${job.jobName}`}>
              {applications.map((application) => (
                <li className="posting-application" key={application.id}>
                  <span className="posting-application-name">Application #{application.id}</span>
                  <span className="posting-application-date">
                    Applied{' '}
                    {application.appliedAt
                      ? <time dateTime={application.appliedAt}>{formatMoment(application.appliedAt)}</time>
                      : 'date unavailable'}
                  </span>
                  <ApplicationStatusBadge status={application.status} />
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </li>
  )
}

function PostingApplicationsData() {
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [now, setNow] = useState(() => Date.now())
  const [filters, setFilters] = useState<Filters>(defaultFilters)
  const [sorting, setSorting] = useState<Sorting>(defaultSorting)
  const [retrying, setRetrying] = useState<number | null>(null)
  const [searchParams, setSearchParams] = useSearchParams()
  const retryRequest = useRef<AbortController | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    loadPostingApplications(controller.signal).then((result) => {
      if (controller.signal.aborted) return
      setNow(Date.now())
      setState(result)
    }).catch(() => {
      if (!controller.signal.aborted) setState({ status: 'error' })
    })
    return () => controller.abort()
  }, [attempt])

  useEffect(() => () => retryRequest.current?.abort(), [])

  function refresh() {
    retryRequest.current?.abort()
    setRetrying(null)
    setState({ status: 'loading' })
    setAttempt((value) => value + 1)
  }

  async function retryPosting(jobId: number) {
    retryRequest.current?.abort()
    const controller = new AbortController()
    retryRequest.current = controller
    setRetrying(jobId)
    try {
      const received = await loadReceivedApplications(jobId, controller.signal)
      if (controller.signal.aborted) return
      setState((current) => current.status !== 'ready' ? current : {
        status: 'ready',
        entries: current.entries.map((entry) => entry.job.id === jobId ? { ...entry, received } : entry),
      })
    } catch (error) {
      if (controller.signal.aborted) return
      if (isSessionExpired(error)) setState({ status: 'unauthenticated' })
    } finally {
      if (!controller.signal.aborted) setRetrying(null)
    }
  }

  // /account/jobs/applications?job=12 narrows the page to one posting.
  const requestedJob = searchParams.get('job')
  const entries: PostingEntry[] | null = state.status === 'ready' ? state.entries : null
  const focused = entries && requestedJob && /^\d+$/.test(requestedJob)
    ? entries.find((entry) => entry.job.id === Number(requestedJob)) ?? null
    : null
  const scoped = useMemo(() => (entries && focused ? [focused] : entries ?? []), [entries, focused])
  const view = useMemo(() => buildView(scoped, filters, sorting, now), [scoped, filters, sorting, now])

  if (state.status === 'loading') {
    return <Notice title="Loading your job postings">Getting your postings and the applications received…</Notice>
  }
  if (state.status === 'unauthenticated') {
    return <Notice title="Please sign in again" action={<a className="applications-button" href={loginUrl}>Sign in</a>}>
      Your session has expired. Sign in again to view the applications on your postings.
    </Notice>
  }
  if (state.status === 'forbidden') {
    return <Notice title="Applications are unavailable for this account" action={<Link className="applications-button" to="/">Back to home</Link>}>
      You do not have permission to view these job postings.
    </Notice>
  }
  if (state.status === 'error') {
    return <Notice title="We couldn’t load your job postings" error action={
      <button className="applications-button" type="button" onClick={refresh}>Try again</button>
    }>
      Please check your connection and try again in a moment.
    </Notice>
  }
  if (state.entries.length === 0) {
    return <Notice title="No job postings yet" action={<Link className="applications-button" to="/jobs/new">Create a job</Link>}>
      Applications will appear here once you have posted a job and someone applies.
    </Notice>
  }

  const narrowed = filters.applicationStatus !== 'All'
    || (filters.dateField === 'applied' && (filters.from !== '' || filters.to !== '') && dateRangeProblem(filters) === null)

  return (
    <section aria-label="Job postings and applications received">
      {focused && (
        <p className="posting-scope" role="status">
          Showing applications for <strong>{focused.job.jobName}</strong>.{' '}
          <button className="posting-inline-button" type="button" onClick={() => setSearchParams({})}>
            Show all my postings
          </button>
        </p>
      )}

      <Toolbar filters={filters} sorting={sorting} onFilters={setFilters} onSorting={setSorting} />

      <div className="posting-results-top">
        <p role="status">
          Showing {view.postings.length} of {count(view.totalPostings, 'job posting')}
          {' · '}
          {view.visibleApplications === view.totalApplications
            ? count(view.totalApplications, 'application')
            : `${view.visibleApplications} of ${count(view.totalApplications, 'application')}`}
        </p>
        <button className="applications-refresh" type="button" onClick={refresh}>
          <span aria-hidden="true">↻</span> Refresh
        </button>
      </div>

      {view.postings.length === 0 ? (
        <Notice title="No job postings match these filters" action={
          <button className="applications-button" type="button" onClick={() => setFilters(defaultFilters)}>Clear filters</button>
        }>
          Try a different status or a wider date range.
        </Notice>
      ) : (
        <ul className="posting-list" aria-label="Your job postings">
          {view.postings.map((posting) => (
            <PostingCard
              key={posting.job.id}
              posting={posting}
              narrowed={narrowed}
              retrying={retrying === posting.job.id}
              onRetry={() => retryPosting(posting.job.id)}
            />
          ))}
        </ul>
      )}
    </section>
  )
}

export default function PostingApplicationsPage() {
  const session = useSession()
  return (
    <div className="site-shell applications-shell">
      <SiteHeader />
      <div className="jobs-banner">
        <div className="jobs-banner-inner">
          <span className="section-kicker">CAREERCONNECT / RECRUITING</span>
          <h1>Applications received.</h1>
          <p>See who applied to each of your job postings, then filter and sort to find what you need.</p>
        </div>
      </div>
      <main className="applications-page posting-page" id="main-content">
        <p className="job-links">
          <Link to="/account/jobs">My postings</Link>
          <Link to="/jobs/new">Create a job</Link>
        </p>
        {session.status === 'loading' && <Notice title="Checking your account">Please wait while we check your session…</Notice>}
        {session.status === 'signed-out' && <Notice title="Sign in to view applications" action={
          <a className="applications-button" href={loginUrl}>Sign in</a>
        }>
          Sign in to see the applications received on your job postings.
        </Notice>}
        {session.status === 'unavailable' && <Notice title="We couldn’t check your account" error action={
          <button className="applications-button" type="button" onClick={() => window.location.reload()}>Try again</button>
        }>
          Please check your connection and try again.
        </Notice>}
        {session.status === 'signed-in' && <PostingApplicationsData key={session.user.userId} />}
      </main>
      <SiteFooter />
    </div>
  )
}
