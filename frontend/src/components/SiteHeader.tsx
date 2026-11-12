import { Link, NavLink } from 'react-router-dom'
import { loginUrl, logoutUrl, useSession } from '../auth/session'
import { roleLabels } from '../auth/roles'

export default function SiteHeader() {
  const session = useSession()
  const signedIn = session.status === 'signed-in'
  const needsRole = signedIn && session.user.role === null

  return (
    <header className="site-header">
      <div className="site-header-inner">
        <Link className="brand" to="/" aria-label="CareerConnect home">
          <span className="brand-mark" aria-hidden="true">
            C
          </span>

          <span>
            Career<span className="brand-accent">Connect</span>
          </span>
        </Link>

        <nav className="site-nav" aria-label="Main navigation">
          <a href="/#features">Features</a>
          <a href="/#how-it-works">How it works</a>
          <NavLink to="/jobs">Find jobs</NavLink>

          {signedIn && session.user.role === 'RECRUITER' && (
            <>
              <NavLink to="/jobs/new">Post a job</NavLink>
              <NavLink to="/account/jobs">My postings</NavLink>
            </>
          )}

          {signedIn && session.user.role === 'JOB_SEEKER' && (
            <NavLink to="/account/applications">Applications</NavLink>
          )}
        </nav>

        <div className="header-account" aria-live="polite">
          {signedIn ? (
            <>
              <span className="header-account-name" title={session.user.name}>
                Hi, {session.user.name}
              </span>
              {needsRole ? (
                <Link className="header-sign-in" to="/onboarding/role">
                  Finish setup <span aria-hidden="true">↗</span>
                </Link>
              ) : (
                <>
                  <span className="header-account-note">
                    {session.user.role ? roleLabels[session.user.role] : ''}
                  </span>
                  <Link className="header-sign-in" to="/account">
                    My account
                  </Link>
                </>
              )}
              {/* A normal POST navigation lets the backend finish the Auth0
                  logout redirect; a fetch would not navigate the browser. */}
              <form method="post" action={logoutUrl}>
                <button className="header-sign-in" type="submit">
                  Sign out
                </button>
              </form>
            </>
          ) : (
            <>
              {session.status === 'loading' && (
                <span className="header-account-note">Checking session…</span>
              )}
              {session.status === 'unavailable' && (
                <span className="header-account-note" role="status">
                  Account status unavailable
                </span>
              )}
              <a className="header-sign-in" href={loginUrl}>
                Sign in <span aria-hidden="true">↗</span>
              </a>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
