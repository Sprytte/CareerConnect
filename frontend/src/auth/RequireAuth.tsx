import type { ReactNode } from 'react'
import { loginUrl, useSession } from './session'

type RequireAuthProps = {
  children: ReactNode
}

export default function RequireAuth({ children }: RequireAuthProps) {
  const session = useSession()

  if (session.status === 'signed-in') {
    return <>{children}</>
  }

  if (session.status === 'loading') {
    return (
      <section className="account-access" aria-busy="true">
        <h1>Checking your account</h1>
        <p role="status">Please wait while we check your session.</p>
      </section>
    )
  }

  if (session.status === 'signed-out') {
    return (
      <section className="account-access">
        <h1>Sign in to access your resume</h1>
        <p>Your resume page is available after you sign in.</p>

        <div className="account-access-actions">
          <a className="header-sign-in" href={loginUrl}>
            Sign in
          </a>
          <a href="/">Back to home</a>
        </div>
      </section>
    )
  }

  // An unavailable session check does not establish that the user is signed out.
  return (
    <section className="account-access">
      <h1>We couldn’t check your account</h1>
      <p role="status">
        Your resume page is temporarily unavailable. Please try again.
      </p>

      <div className="account-access-actions">
        <button
          className="header-sign-in"
          type="button"
          onClick={() => window.location.reload()}
        >
          Try again
        </button>
        <a href="/">Back to home</a>
      </div>
    </section>
  )
}