import './App.css'

const features = [
  {
    number: '01',
    title: 'Your account',
    description: 'A simple starting point for signing in and accessing your space.',
  },
  {
    number: '02',
    title: 'Your resume',
    description: 'A dedicated place to upload and manage your resume.',
  },
]

function App() {
  return (
    <div className="site-shell">
      <header className="site-header">
        <a className="brand" href="#top" aria-label="CareerConnect home">
          <span className="brand-mark" aria-hidden="true">C</span>
          <span>Career<span className="brand-accent">Connect</span></span>
        </a>

        <nav className="site-nav" aria-label="Main navigation">
          <a href="#features">Features</a>
          <a href="#how-it-works">How it works</a>
        </nav>

        {/* Replace this disabled button when the team confirms the auth flow. */}
        <button
          className="header-sign-in"
          type="button"
          disabled
          title="Sign in will be connected after authentication is configured"
        >
          Sign in <span aria-hidden="true">↗</span>
        </button>
      </header>

      <main id="top">
        <section className="hero" aria-labelledby="hero-title">
          <div className="hero-copy">
            <div className="eyebrow"><span className="eyebrow-dot" /> A CLEARER WAY FORWARD</div>
            <h1 id="hero-title">Make room for your <em>next move.</em></h1>
            <p className="hero-description">
              Bring your career essentials together in one place. Start with your
              account and resume, then build from there.
            </p>
            <div className="hero-actions">
              <a className="primary-action" href="#features">
                Explore CareerConnect <span aria-hidden="true">↗</span>
              </a>
              <a className="text-action" href="#how-it-works">See how it works <span aria-hidden="true">↓</span></a>
            </div>
            <div className="hero-note"><span className="note-line" /> Built around the way you work.</div>
          </div>

          <div className="hero-visual" aria-hidden="true">
            <div className="visual-glow" />
            <div className="visual-card">
              <div className="visual-topline"><span>CC / PERSONAL SPACE</span><span className="visual-spark">✦</span></div>
              <div className="visual-avatar">A</div>
              <div className="visual-greeting">Your next chapter<br /><strong>starts here.</strong></div>
              <div className="visual-divider" />
              <div className="visual-resume">
                <span className="file-icon"><span>≡</span></span>
                <span className="file-text"><strong>Your resume</strong><small>Keep your experience close.</small></span>
                <span className="file-arrow">↗</span>
              </div>
              <div className="visual-footer">ONE PLACE. MORE POSSIBILITIES.</div>
            </div>
            <span className="visual-orbit visual-orbit-one" />
            <span className="visual-orbit visual-orbit-two" />
          </div>
        </section>

        <section className="features" id="features" aria-labelledby="features-title">
          <div className="section-heading">
            <span className="section-kicker">THE FIRST STEPS</span>
            <h2 id="features-title">The essentials, in one place.</h2>
            <p>A focused place to get started with CareerConnect.</p>
          </div>
          <div className="feature-grid">
            {features.map((feature) => (
              <article className="feature-card" key={feature.number}>
                <span className="feature-number">{feature.number} /</span>
                <h3>{feature.title}</h3>
                <p>{feature.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="closing" id="how-it-works" aria-labelledby="closing-title">
          <span className="section-kicker">KEEP MOVING</span>
          <h2 id="closing-title">A place for what comes next.</h2>
          <p>Create an account, add your resume, and keep your career details within reach.</p>
          <a href="#top">Back to top <span aria-hidden="true">↑</span></a>
        </section>
      </main>

      <footer className="site-footer"><span>CareerConnect</span><span>Made for your next move.</span></footer>
    </div>
  )
}

export default App
