
import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { loginUrl, useSession } from '../auth/session'
import {
  applicationStatuses, formatAppliedDate, isApplicationsResult,
} from '../applications/applications'
import type { ApplicationStatus, ApplicationsLoader, ApplicationsResult, JobApplication } from '../applications/applications'
import { loadApplications } from '../applications/loadApplications'
import ApplicationStatusBadge from './ApplicationStatusBadge'
import SiteHeader from './SiteHeader'
import SiteFooter from './SiteFooter'
import '../styles/App.css'
import '../styles/Applications.css'

type Props = { loader?: ApplicationsLoader }
type LoadState = ApplicationsResult | { status: 'loading' } | { status: 'error' }
type Filter = 'All' | ApplicationStatus

type WeeklyApplicationCount = {
  label: string
  count: number
}

function getWeeklyApplicationCounts(applications: JobApplication[]): WeeklyApplicationCount[] {
  const counts = new Map<string, number>()

  applications.forEach((application) => {
    if (!application.appliedOn) return

    const date = new Date(`${application.appliedOn}T00:00:00Z`)

    if (Number.isNaN(date.getTime())) return

    const day = date.getUTCDay()
    const diffToMonday = day === 0 ? -6 : 1 - day

    const monday = new Date(date)
    monday.setUTCDate(date.getUTCDate() + diffToMonday)

    const weekKey = monday.toISOString().slice(0, 10)

    counts.set(weekKey, (counts.get(weekKey) ?? 0) + 1)
  })

  return Array.from(counts.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([weekStart, count]) => ({
      label: new Intl.DateTimeFormat('en-CA', {
        month: 'short',
        day: 'numeric',
        timeZone: 'UTC',
      }).format(new Date(`${weekStart}T00:00:00Z`)),
      count,
    }))
}

function ApplicationActivityChart({ applications }: { applications: JobApplication[] }) {
  const weeklyData = getWeeklyApplicationCounts(applications)

  const maxCount = Math.max(1, ...weeklyData.map((week) => week.count))
  const tickCount = Math.min(maxCount, 5)
  const ticks = Array.from(
    { length: tickCount + 1 },
    (_, index) => Math.round((maxCount * (tickCount - index)) / tickCount),
  )

  return (
    <section className="applications-chart" aria-labelledby="applications-chart-title">
      <div className="applications-chart-heading">
        <h2 id="applications-chart-title">Applications submitted over time</h2>
        <p>Track how many job applications you submitted each week.</p>
      </div>

      {weeklyData.length === 0 ? (
        <p className="applications-chart-empty">
          No application activity to display yet.
        </p>
      ) : (
        <div
          className="applications-chart-scroll"
          role="img"
          aria-label={`Weekly application submissions: ${weeklyData
            .map((week) => `${week.label}: ${week.count}`)
            .join(', ')}`}
        >
          <div className="applications-chart-body">
            <div className="applications-chart-y-axis" aria-hidden="true">
              {ticks.map((tick) => <span key={tick}>{tick}</span>)}
            </div>

            <div className="applications-chart-main">
              <div className="applications-chart-plot">
                <div className="applications-chart-grid" aria-hidden="true">
                  {ticks.map((tick) => (
                    <div key={tick} className="applications-chart-gridline" />
                  ))}
                </div>

                <div className="applications-chart-bars">
                  {weeklyData.map((week, index) => (
                    <div
                      className="applications-chart-bar-column"
                      key={`${week.label}-${index}`}
                    >
                      <div
                        className="applications-chart-bar"
                        style={{ height: `${(week.count / maxCount) * 100}%` }}
                        title={`${week.count} applications — week of ${week.label}`}
                      />
                    </div>
                  ))}
                </div>
              </div>

              <div className="applications-chart-labels" aria-hidden="true">
                {weeklyData.map((week, index) => (
                  <span key={`${week.label}-${index}`}>{week.label}</span>
                ))}
              </div>
            </div>
          </div>
          <p className="applications-chart-axis-label">Week</p>
        </div>
      )}
    </section>
  )
}

function Notice({ title, children, action, error = false }: {
  title: string
  children: ReactNode
  action?: ReactNode
  error?: boolean
}) {
  return (
    <div className="applications-notice" role={error ? 'alert' : 'status'}>
      <span className="applications-notice-mark" aria-hidden="true">
        {error ? '!' : '↗'}
      </span>
      <h2>{title}</h2>
      <p>{children}</p>
      {action && <div className="applications-notice-actions">{action}</div>}
    </div>
  )
}

function ApplicationCard({ application }: { application: JobApplication }) {
  const date = formatAppliedDate(application.appliedOn)

  return (
    <li className="application-card">
      <div className="application-card-main">
        <span className="application-company">{application.company}</span>
        <h3>{application.jobTitle}</h3>

        <p className="application-date">
          {date
            ? (
              <>
                Applied <time dateTime={application.appliedOn!}>{date}</time>
              </>
            )
            : 'Applied date unavailable'}
        </p>
      </div>

      <ApplicationStatusBadge status={application.status} />
    </li>
  )
}

function ApplicationList({ applications, filter, onFilter }: {
  applications: JobApplication[]
  filter: Filter
  onFilter: (filter: Filter) => void
}) {
  const visible = applications.filter(
    (application) => filter === 'All' || application.status === filter
  )

  return (
    <>
      <div
        className="applications-filters"
        role="group"
        aria-label="Filter applications by status"
      >
        {(['All', ...applicationStatuses] as const).map((status) => (
          <button
            key={status}
            type="button"
            className="applications-filter"
            aria-pressed={filter === status}
            onClick={() => onFilter(status)}
          >
            {status}

            <span className="applications-filter-count">
              {status === 'All'
                ? applications.length
                : applications.filter((item) => item.status === status).length}
            </span>
          </button>
        ))}
      </div>

      <div className="applications-list-heading">
        <h2>
          {filter === 'All'
            ? 'All applications'
            : `${filter} applications`}
        </h2>

        <p role="status">
          {visible.length}{' '}
          {visible.length === 1 ? 'application' : 'applications'}
        </p>
      </div>

      {applications.length === 0 ? (
        <Notice
          title="No applications yet"
          action={
            <Link className="applications-button" to="/">
              Back to home
            </Link>
          }
        >
          Your applications will appear here after you apply for a job.
        </Notice>
      ) : visible.length === 0 ? (
        <Notice
          title={`No ${filter.toLowerCase()} applications`}
          action={
            <button
              className="applications-button"
              type="button"
              onClick={() => onFilter('All')}
            >
              Show all applications
            </button>
          }
        >
          Try another status to see your other applications.
        </Notice>
      ) : (
        <ul className="applications-list" aria-label="Your applications">
          {visible.map((application) => (
            <ApplicationCard
              key={application.id}
              application={application}
            />
          ))}
        </ul>
      )}
    </>
  )
}

function ApplicationsData({ loader }: { loader: ApplicationsLoader }) {
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [filter, setFilter] = useState<Filter>('All')

  useEffect(() => {
    const controller = new AbortController()

    Promise.resolve()
      .then(() =>
        controller.signal.aborted
          ? undefined
          : loader(controller.signal)
      )
      .then((result) => {
        if (!controller.signal.aborted) {
          setState(
            isApplicationsResult(result)
              ? result
              : { status: 'error' }
          )
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setState({ status: 'error' })
        }
      })

    return () => controller.abort()
  }, [attempt, loader])

  function refresh() {
    setState({ status: 'loading' })
    setAttempt((value) => value + 1)
  }

  if (state.status === 'loading') {
    return (
      <Notice title="Loading your applications">
        Getting the latest application statuses…
      </Notice>
    )
  }

  if (state.status === 'unavailable') {
    return (
      <Notice
        title="Application tracking is coming soon"
        action={
          <button
            className="applications-button"
            type="button"
            onClick={refresh}
          >
            Check again
          </button>
        }
      >
        Your applications and their latest statuses will appear here when
        tracking is available.
      </Notice>
    )
  }

  if (state.status === 'unauthenticated') {
    return (
      <Notice
        title="Please sign in again"
        action={
          <a className="applications-button" href={loginUrl}>
            Sign in
          </a>
        }
      >
        Your session has expired. Sign in again to view your applications.
      </Notice>
    )
  }

  if (state.status === 'forbidden') {
    return (
      <Notice
        title="Applications are unavailable for this account"
        action={
          <Link className="applications-button" to="/">
            Back to home
          </Link>
        }
      >
        You do not have permission to view these applications.
      </Notice>
    )
  }

  if (state.status === 'error') {
    return (
      <Notice
        title="We couldn’t load your applications"
        error
        action={
          <button
            className="applications-button"
            type="button"
            onClick={refresh}
          >
            Try again
          </button>
        }
      >
        Please try again in a moment.
      </Notice>
    )
  }

  return (
    <section
      className="applications-results"
      aria-label="Application statuses"
    >
      <div className="applications-results-top">
        <p>Keep track of every next step.</p>

        <button
          className="applications-refresh"
          type="button"
          onClick={refresh}
        >
          <span aria-hidden="true">↻</span> Refresh
        </button>
      </div>

      <ApplicationActivityChart applications={state.applications} />

      <ApplicationList
        applications={state.applications}
        filter={filter}
        onFilter={setFilter}
      />
    </section>
  )
}

export default function Applications({ loader = loadApplications }: Props) {
  const session = useSession()

  return (
    <div className="site-shell applications-shell">
      <SiteHeader />

      <main className="applications-page" id="main-content">
        <nav
          className="applications-account-nav"
          aria-label="Account navigation"
        >
          <span aria-current="page">Applications</span>

          <Link to="/account/resume">
            Resumes <span aria-hidden="true">↗</span>
          </Link>
        </nav>

        <div className="applications-title">
          <p className="section-kicker">YOUR NEXT CHAPTER</p>

          <h1>
            My applications<span aria-hidden="true">.</span>
          </h1>

          <p>
            See where you stand, from your first application to your next
            offer.
          </p>
        </div>

        {session.status === 'loading' && (
          <Notice title="Checking your account">
            Please wait while we check your session…
          </Notice>
        )}

        {session.status === 'signed-out' && (
          <Notice
            title="Sign in to view your applications"
            action={
              <a className="applications-button" href={loginUrl}>
                Sign in
              </a>
            }
          >
            Keep your applications and their latest statuses together in one
            place.
          </Notice>
        )}

        {session.status === 'unavailable' && (
          <Notice
            title="We couldn’t check your account"
            error
            action={
              <button
                className="applications-button"
                type="button"
                onClick={() => window.location.reload()}
              >
                Try again
              </button>
            }
          >
            Please check your connection and try again.
          </Notice>
        )}

        {session.status === 'signed-in' && (
          <ApplicationsData
            key={session.user.userId}
            loader={loader}
          />
        )}
      </main>

      <SiteFooter />
    </div>
  )
}
