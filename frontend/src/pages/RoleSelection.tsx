import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  loginUrl,
  logoutUrl,
  useSession,
  useUpdateSessionUser,
} from '../auth/session'
import { RoleError, roleErrorMessage, saveRole } from '../auth/roleApi'
import {
  roleDescriptions,
  roleLabels,
  USER_ROLES,
  type UserRole,
} from '../auth/roles'
import SiteHeader from '../components/SiteHeader'
import SiteFooter from '../components/SiteFooter'
import '../styles/App.css'
import '../styles/Account.css'
import '../styles/RoleSelection.css'

type Feedback = {
  kind: 'error' | 'success'
  message: string
  sessionExpired?: boolean
} | null

function Notice({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="account-card account-notice role-notice">
      <h2>{title}</h2>
      {children}
    </div>
  )
}

export default function RoleSelection() {
  const session = useSession()
  const updateSessionUser = useUpdateSessionUser()
  const navigate = useNavigate()
  const [selected, setSelected] = useState<UserRole | null>(null)
  const [feedback, setFeedback] = useState<Feedback>(null)
  const [saving, setSaving] = useState(false)
  const request = useRef<AbortController | null>(null)
  const errorSummary = useRef<HTMLDivElement>(null)

  useEffect(() => () => request.current?.abort(), [])

  useEffect(() => {
    if (session.status === 'signed-in' && session.user.role) {
      navigate('/', { replace: true })
    }
  }, [navigate, session])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving || request.current) return
    if (session.status !== 'signed-in') return

    if (!selected) {
      setFeedback({ kind: 'error', message: 'Choose Job seeker or Recruiter to continue.' })
      return
    }

    const controller = new AbortController()
    request.current = controller
    setSaving(true)
    setFeedback(null)

    try {
      const updated = await saveRole(session.user.userId, selected, controller.signal)
      if (controller.signal.aborted) return
      updateSessionUser(updated)
      navigate('/', { replace: true })
    } catch (error) {
      if (controller.signal.aborted) return
      setFeedback({
        kind: 'error',
        message: roleErrorMessage(error),
        sessionExpired: error instanceof RoleError && error.sessionExpired,
      })
      requestAnimationFrame(() => errorSummary.current?.focus())
    } finally {
      if (!controller.signal.aborted) {
        request.current = null
        setSaving(false)
      }
    }
  }

  return (
    <div className="site-shell role-shell">
      <SiteHeader />
      <main className="role-main" id="main-content">
        <div className="role-heading">
          <span className="section-kicker">ONE LAST STEP</span>
          <h1>
            How will you use <span>CareerConnect?</span>
          </h1>
          <p>
            Your account type decides which workspace you see. Choose carefully —
            it can’t be changed after you continue.
          </p>
        </div>

        {session.status === 'loading' && (
          <Notice title="Checking your account">
            <p role="status">Please wait while we check your sign-in.</p>
          </Notice>
        )}

        {session.status === 'signed-out' && (
          <Notice title="Sign in to choose your account type">
            <p>We need to know who you are before creating your workspace.</p>
            <a className="account-primary" href={loginUrl}>
              Sign in <span aria-hidden="true">↗</span>
            </a>
          </Notice>
        )}

        {session.status === 'unavailable' && (
          <Notice title="We could not check your account">
            <p role="alert">Please check your connection and try again.</p>
            <button className="account-primary" onClick={() => window.location.reload()}>
              Try again
            </button>
          </Notice>
        )}

        {session.status === 'signed-in' && !session.user.role && (
          <>
            <form className="role-form" onSubmit={handleSubmit} aria-busy={saving}>
              <fieldset className="role-fieldset" disabled={saving || Boolean(feedback?.sessionExpired)}>
                <legend className="account-sr-only">Choose your account type</legend>
                <div className="role-grid">
                  {USER_ROLES.map((role) => (
                    <label
                      className={`role-option${selected === role ? ' role-option-selected' : ''}`}
                      key={role}
                    >
                      <input
                        type="radio"
                        name="role"
                        value={role}
                        checked={selected === role}
                        onChange={() => {
                          setSelected(role)
                          setFeedback(null)
                        }}
                      />
                      <span className="role-option-topline">
                        <span className="role-option-icon" aria-hidden="true">
                          {role === 'JOB_SEEKER' ? '↗' : '✦'}
                        </span>
                        <span className="role-option-label">{roleLabels[role]}</span>
                      </span>
                      <span className="role-option-description">
                        {roleDescriptions[role]}
                      </span>
                      <span className="role-option-detail">
                        {role === 'JOB_SEEKER'
                          ? 'Resumes, applications, and saved jobs'
                          : 'Job postings and applicant review'}
                      </span>
                    </label>
                  ))}
                </div>
              </fieldset>

              {feedback && (
                <div
                  ref={feedback.kind === 'error' ? errorSummary : undefined}
                  tabIndex={-1}
                  className={`account-feedback account-feedback-${feedback.kind}`}
                  role={feedback.kind === 'error' ? 'alert' : 'status'}
                >
                  {feedback.message}
                  {feedback.sessionExpired && (
                    <a className="account-inline-link" href={loginUrl}>
                      Sign in again
                    </a>
                  )}
                </div>
              )}

              <div className="role-actions">
                <button className="account-primary" type="submit" disabled={!selected || saving}>
                  {saving ? 'Saving…' : 'Continue to CareerConnect'}
                  <span aria-hidden="true">↗</span>
                </button>
                <span className="role-signed-in">
                  Signed in as {session.user.email || session.user.name}
                </span>
              </div>
            </form>
            <form method="post" action={logoutUrl} className="role-signout">
              <button type="submit" className="account-text-button">
                Use a different account
              </button>
            </form>
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  )
}
