export default function SiteHeader() {
  return (
    <header className="site-header">
      <div className="site-header-inner">
        <a className="brand" href="#top" aria-label="CareerConnect home">
          <span className="brand-mark" aria-hidden="true">
            C
          </span>

          <span>
            Career<span className="brand-accent">Connect</span>
          </span>
        </a>

        <nav className="site-nav" aria-label="Main navigation">
          <a href="#features">Features</a>
          <a href="#how-it-works">How it works</a>
        </nav>

        {/* Sign-in stays disabled until the team chooses the authentication
            flow; connect it to that flow once agreed. */}
        <button
          className="header-sign-in"
          type="button"
          disabled
          title="Sign in is pending the team's authentication decision"
        >
          Sign in <span aria-hidden="true">↗</span>
        </button>
      </div>
    </header>
  )
}