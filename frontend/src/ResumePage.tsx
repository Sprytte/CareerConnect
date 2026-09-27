import Resume from './Resume'
import RequireAuth from './auth/RequireAuth'
import SiteHeader from './components/SiteHeader'
import SiteFooter from './components/SiteFooter'
import './App.css'

export default function ResumePage() {
  return (
    <div className="site-shell" id="top">
      <SiteHeader />

      <main>
        {/* Keep page access separate from the resume upload implementation. */}
        <RequireAuth>
          <Resume />
        </RequireAuth>
      </main>

      <SiteFooter />
    </div>
  )
}