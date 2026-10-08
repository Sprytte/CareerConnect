import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { loginUrl, useSession, useUpdateSessionUser } from '../auth/session'
import SiteHeader from './SiteHeader'
import SiteFooter from './SiteFooter'
import {
  loadProfile,
  saveProfile,
  ProfileError,
  profileErrorMessage,
} from '../profile/profileApi'
import type { AccountProfile } from '../profile/profileApi'
import '../styles/App.css'
import '../styles/Account.css'

type Feedback = {
  kind: 'success' | 'error'
  message: string
  sessionExpired?: boolean
} | null
type ProfileLoad =
  | { status: 'loading' }
  | { status: 'ready'; profile: AccountProfile }
  | { status: 'error'; message: string; sessionExpired: boolean }

function AccountNotice({
  title,
  children,
}: {
  title: string
  children: React.ReactNode
}) {
  return (
    <div className="account-card account-notice">
      <h2>{title}</h2>
      {children}
    </div>
  )
}

function ProfileEditor({ initial }: { initial: AccountProfile }) {
  const updateSessionUser = useUpdateSessionUser()
  const [profile, setProfile] = useState(initial)
  const [name, setName] = useState(initial.name)
  const [email, setEmail] = useState(initial.email)
  const [errors, setErrors] = useState<{ name?: string; email?: string }>({})
  const [feedback, setFeedback] = useState<Feedback>(null)
  const [saving, setSaving] = useState(false)
  const saveRequest = useRef<AbortController | null>(null)
  const nameInput = useRef<HTMLInputElement>(null)
  const emailInput = useRef<HTMLInputElement>(null)
  const errorSummary = useRef<HTMLDivElement>(null)
  const changed = name.trim() !== profile.name || email.trim() !== profile.email

  useEffect(() => () => saveRequest.current?.abort(), [])

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving || saveRequest.current) return
    const next = { name: name.trim(), email: email.trim() }
    const validation: { name?: string; email?: string } = {}
    if (!next.name) validation.name = 'Enter your name.'
    if (!next.email || emailInput.current?.validity.typeMismatch) {
      validation.email = 'Enter a valid email address.'
    }
    setErrors(validation)
    setFeedback(null)
    if (validation.name || validation.email) {
      if (validation.name) nameInput.current?.focus()
      else emailInput.current?.focus()
      return
    }
    if (!changed) return

    const controller = new AbortController()
    saveRequest.current = controller
    setSaving(true)
    try {
      const saved = await saveProfile(profile, next, controller.signal)
      if (controller.signal.aborted) return
      setProfile(saved)
      setName(saved.name)
      setEmail(saved.email)
      updateSessionUser({
        userId: saved.userId,
        name: saved.name || saved.email || 'Account',
        email: saved.email,
      })
      setFeedback({ kind: 'success', message: 'Your name has been updated.' })
    } catch (error) {
      if (controller.signal.aborted) return
      setFeedback({
        kind: 'error',
        message: profileErrorMessage(error),
        sessionExpired: error instanceof ProfileError && error.sessionExpired,
      })
      // The focused summary is also an alert; users can find it after a failed save.
      requestAnimationFrame(() => errorSummary.current?.focus())
    } finally {
      if (!controller.signal.aborted) {
        saveRequest.current = null
        setSaving(false)
      }
    }
  }

  function handleCancel() {
    setName(profile.name)
    setEmail(profile.email)
    setErrors({})
    setFeedback(null)
  }

  return (
    <section className="account-card" aria-labelledby="profile-title">
      <div className="account-section-heading">
        <span className="account-section-icon" aria-hidden="true">
          01
        </span>
        <div>
          <h2 id="profile-title">Personal details</h2>
          <p>Keep your account up to date.</p>
        </div>
      </div>
      <p className="account-availability">
        Name changes are currently unavailable for Google sign-in. Email changes
        are coming soon.
      </p>
      <form onSubmit={handleSave} noValidate aria-busy={saving}>
        <fieldset
          className="account-fields"
          disabled={saving || feedback?.sessionExpired}
        >
          <legend className="account-sr-only">Edit personal details</legend>
          <div className="account-field">
            <label htmlFor="account-name">Full name</label>
            <input
              ref={nameInput}
              id="account-name"
              name="name"
              autoComplete="name"
              required
              value={name}
              onChange={(event) => {
                setName(event.target.value)
                setFeedback(null)
                setErrors((current) => ({ ...current, name: undefined }))
              }}
              aria-invalid={Boolean(errors.name)}
              aria-describedby={errors.name ? 'account-name-error' : undefined}
            />
            {errors.name && (
              <p className="account-field-error" id="account-name-error">
                {errors.name}
              </p>
            )}
          </div>
          <div className="account-field">
            <label htmlFor="account-email">Email address</label>
            <input
              ref={emailInput}
              id="account-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => {
                setEmail(event.target.value)
                setFeedback(null)
                setErrors((current) => ({ ...current, email: undefined }))
              }}
              aria-invalid={Boolean(errors.email)}
              aria-describedby={`account-email-hint${errors.email ? ' account-email-error' : ''}`}
            />
            <p className="account-field-hint" id="account-email-hint">
              Your current sign-in email stays in use until email changes become
              available.
            </p>
            {errors.email && (
              <p className="account-field-error" id="account-email-error">
                {errors.email}
              </p>
            )}
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
        <div className="account-form-actions">
          <button
            className="account-primary"
            type="submit"
            disabled={saving || !changed || feedback?.sessionExpired}
          >
            {saving ? 'Saving…' : 'Save changes'}
            <span aria-hidden="true">↗</span>
          </button>
          <button
            className="account-secondary"
            type="button"
            onClick={handleCancel}
            disabled={saving || feedback?.sessionExpired}
          >
            Cancel
          </button>
          <span className="account-form-note">
            {saving
              ? 'Please wait while we save your name.'
              : changed
                ? 'You have unsaved changes.'
                : 'Your details are up to date.'}
          </span>
        </div>
      </form>
    </section>
  )
}

function PasswordEditor() {
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState<{
    password?: string
    confirmation?: string
  }>({})
  const [message, setMessage] = useState('')
  const passwordInput = useRef<HTMLInputElement>(null)
  const confirmationInput = useRef<HTMLInputElement>(null)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validation: { password?: string; confirmation?: string } = {}
    if (!password) validation.password = 'Enter a new password.'
    if (!confirmation) validation.confirmation = 'Confirm your new password.'
    else if (password !== confirmation)
      validation.confirmation = 'The passwords do not match.'
    setErrors(validation)
    setMessage('')
    if (validation.password || validation.confirmation) {
      if (validation.password) passwordInput.current?.focus()
      else confirmationInput.current?.focus()
      return
    }
    // No password-update contract exists in the supplied backend. Keep this
    // interface reviewable without transmitting or claiming to save a password.
    setMessage(
      'Password changes are not available yet. Your password has not changed.',
    )
    setPassword('')
    setConfirmation('')
    setShowPassword(false)
  }

  function clear() {
    setPassword('')
    setConfirmation('')
    setShowPassword(false)
    setErrors({})
    setMessage('')
  }

  return (
    <section className="account-card" aria-labelledby="password-title">
      <div className="account-section-heading">
        <span className="account-section-icon" aria-hidden="true">
          02
        </span>
        <div>
          <h2 id="password-title">Change password</h2>
          <p>Choose a new password and confirm it.</p>
        </div>
        <span className="account-badge">Coming soon</span>
      </div>
      <p className="account-availability" id="password-availability">
        Password updates are not available yet. You can check that the two
        fields match.
      </p>
      <form
        onSubmit={handleSubmit}
        noValidate
        aria-describedby="password-availability"
      >
        <div className="account-password-grid">
          <div className="account-field">
            <label htmlFor="account-password">New password</label>
            <input
              ref={passwordInput}
              id="account-password"
              name="newPassword"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              required
              value={password}
              onChange={(event) => {
                setPassword(event.target.value)
                setMessage('')
                setErrors((current) => ({
                  ...current,
                  password: undefined,
                  confirmation: undefined,
                }))
              }}
              aria-invalid={Boolean(errors.password)}
              aria-describedby={
                errors.password ? 'account-password-error' : undefined
              }
            />
            {errors.password && (
              <p className="account-field-error" id="account-password-error">
                {errors.password}
              </p>
            )}
          </div>
          <div className="account-field">
            <label htmlFor="account-confirm-password">
              Confirm new password
            </label>
            <input
              ref={confirmationInput}
              id="account-confirm-password"
              name="confirmPassword"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              required
              value={confirmation}
              onChange={(event) => {
                setConfirmation(event.target.value)
                setMessage('')
                setErrors((current) => ({
                  ...current,
                  confirmation: undefined,
                }))
              }}
              aria-invalid={Boolean(errors.confirmation)}
              aria-describedby={
                errors.confirmation
                  ? 'account-confirm-password-error'
                  : undefined
              }
            />
            {errors.confirmation && (
              <p
                className="account-field-error"
                id="account-confirm-password-error"
              >
                {errors.confirmation}
              </p>
            )}
          </div>
        </div>
        <label className="account-checkbox">
          <input
            type="checkbox"
            checked={showPassword}
            onChange={(event) => setShowPassword(event.target.checked)}
          />
          Show passwords
        </label>
        {message && (
          <p className="account-feedback account-feedback-error" role="alert">
            {message}
          </p>
        )}
        <div className="account-form-actions">
          <button className="account-secondary" type="submit">
            Check password fields
          </button>
          <button className="account-text-button" type="button" onClick={clear}>
            Clear
          </button>
        </div>
      </form>
    </section>
  )
}

function ProfileLoader({ userId }: { userId: string }) {
  const [loaded, setLoaded] = useState<ProfileLoad>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const updateSessionUser = useUpdateSessionUser()
  useEffect(() => {
    const controller = new AbortController()
    loadProfile(userId, controller.signal)
      .then((profile) => {
        if (controller.signal.aborted) return
        updateSessionUser({
          userId: profile.userId,
          name: profile.name || profile.email || 'Account',
          email: profile.email,
        })
        setLoaded({ status: 'ready', profile })
      })
      .catch((error) => {
        if (!controller.signal.aborted)
          setLoaded({
            status: 'error',
            message: profileErrorMessage(error),
            sessionExpired:
              error instanceof ProfileError && error.sessionExpired,
          })
      })
    return () => controller.abort()
  }, [userId, attempt, updateSessionUser])

  if (loaded.status === 'loading')
    return (
      <AccountNotice title="Loading your account">
        <p role="status">Getting your latest details…</p>
      </AccountNotice>
    )
  if (loaded.status === 'error')
    return (
      <AccountNotice title="We could not load your account">
        <p role="alert">{loaded.message}</p>
        {loaded.sessionExpired ? (
          <a className="account-primary" href={loginUrl}>
            Sign in again
          </a>
        ) : (
          <button
            className="account-primary"
            onClick={() => {
              setLoaded({ status: 'loading' })
              setAttempt((current) => current + 1)
            }}
          >
            Try again
          </button>
        )}
      </AccountNotice>
    )
  return (
    <>
      <ProfileEditor initial={loaded.profile} />
      <PasswordEditor />
    </>
  )
}

export default function Account() {
  const session = useSession()
  return (
    <div className="site-shell account-shell">
      <SiteHeader />
      <main className="account-main" id="main-content">
        <div className="account-page-heading">
          <div>
            <span className="section-kicker">YOUR PERSONAL SPACE</span>
            <h1>
              Your account<span>.</span>
            </h1>
            <p>A few details. A clearer next step.</p>
          </div>
          <Link className="account-resume-link" to="/account/resume">
            Manage your resumes <span aria-hidden="true">↗</span>
          </Link>
        </div>
        <div className="account-layout">
          <aside className="account-sidebar" aria-label="Account navigation">
            <span className="account-sidebar-kicker">ACCOUNT</span>
            <span className="account-current" aria-current="page">
              Profile settings <span aria-hidden="true">↗</span>
            </span>
            <Link to="/account/resume">
              Your resumes <span aria-hidden="true">↗</span>
            </Link>
            <div className="account-sidebar-note">
              <span aria-hidden="true">✦</span>
              <p>
                Make space for
                <br />
                <strong>what comes next.</strong>
              </p>
            </div>
          </aside>
          <div className="account-panels">
            {session.status === 'loading' && (
              <AccountNotice title="Checking your session">
                <p role="status">Please wait while we check your account.</p>
              </AccountNotice>
            )}
            {session.status === 'signed-out' && (
              <AccountNotice title="Sign in to edit your account">
                <p>Your personal details are available after you sign in.</p>
                <a className="account-primary" href={loginUrl}>
                  Sign in <span aria-hidden="true">↗</span>
                </a>
              </AccountNotice>
            )}
            {session.status === 'unavailable' && (
              <AccountNotice title="Account status unavailable">
                <p role="alert">
                  We could not check your session. Please try again.
                </p>
                <button
                  className="account-primary"
                  onClick={() => window.location.reload()}
                >
                  Try again
                </button>
              </AccountNotice>
            )}
            {session.status === 'signed-in' && (
              <ProfileLoader
                key={session.user.userId}
                userId={session.user.userId}
              />
            )}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
