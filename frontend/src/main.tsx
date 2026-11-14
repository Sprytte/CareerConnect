import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './styles/index.css'
import App from './App.tsx'
import Resume from './pages/Resume.tsx'
import RoleSelection from './pages/RoleSelection.tsx'
import ResumeFeedback from './pages/ResumeFeedback.tsx'
import Account from './components/Account.tsx'
import Applications from './components/Applications.tsx'
import SearchJobs from './components/SearchJobs.tsx'
import SessionProvider from './auth/SessionProvider'
import RoleGate from './auth/RoleGate'
import { NewJobPage, MyJobsPage } from './jobs/JobPages'
import PostingApplicationsPage from './jobs/PostingApplicationsPage'
import ApplyJobPage from './jobs/ApplyJobPage'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider>
      <BrowserRouter>
        <RoleGate>
          <Routes>
            <Route path="/" element={<App />} />
            <Route path="/onboarding/role" element={<RoleSelection />} />
            <Route path="/jobs" element={<SearchJobs />} />
            <Route path="/jobs/:jobId/apply" element={<ApplyJobPage />} />
            <Route path="/account" element={<Account />} />
            <Route path="/account/resume" element={<Resume />} />
            <Route path="/account/resume/feedback" element={<ResumeFeedback />} />
            <Route path="/account/applications" element={<Applications />} />
            <Route path="/jobs/new" element={<NewJobPage />} />
            <Route path="/account/jobs" element={<MyJobsPage />} />
            <Route path="/account/jobs/applications" element={<PostingApplicationsPage />} />
            </Routes>
        </RoleGate>
      </BrowserRouter>
    </SessionProvider>
  </StrictMode>,
)
