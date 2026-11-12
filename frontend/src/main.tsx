import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './styles/index.css'
import App from './App.tsx'
import Resume from './pages/Resume.tsx'
import RoleSelection from './pages/RoleSelection.tsx'
import Account from './components/Account.tsx'
import Applications from './components/Applications.tsx'
import SearchJobs from './components/SearchJobs.tsx'
import SessionProvider from './auth/SessionProvider'
import RoleGate from './auth/RoleGate'
import { NewJobPage, MyJobsPage } from './jobs/JobPages'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider>
      <BrowserRouter>
        <RoleGate>
          <Routes>
            <Route path="/" element={<App />} />
            <Route path="/onboarding/role" element={<RoleSelection />} />
            <Route path="/jobs" element={<SearchJobs />} />
            <Route path="/account" element={<Account />} />
            <Route path="/account/resume" element={<Resume />} />
            <Route path="/account/applications" element={<Applications />} />
            <Route path="/jobs/new" element={<NewJobPage />} />
            <Route path="/account/jobs" element={<MyJobsPage />} />
          </Routes>
        </RoleGate>
      </BrowserRouter>
    </SessionProvider>
  </StrictMode>,
)
