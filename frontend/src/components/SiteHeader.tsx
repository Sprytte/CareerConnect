import { loginUrl, logoutUrl, useSession } from '../auth/session'
import { Link } from 'react-router-dom'

export default function SiteHeader() {
  const session = useSession()

  return (
    <header className="site-header">
      <div className="site-header-inner">
        <a className="brand" href="/" aria-label="CareerConnect home">
          <span className="brand-mark" aria-hidden="true">
            C
          </span>

          <span>
            Career<span className="brand-accent">Connect</span>
          </span>
        </a>

        <nav className="site-nav" aria-label="Main navigation">
          <a href="/#features">Features</a>
          <a href="/#how-it-works">How it works</a>
        </nav>

        <div className="header-account" aria-live="polite">
          {session.status === 'signed-in' ? (
            <>
              <span className="header-account-name" title={session.user.name}>
                Hi, {session.user.name}
              </span>
              <Link className="header-sign-in" to="/account">
                My account
              </Link>
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
