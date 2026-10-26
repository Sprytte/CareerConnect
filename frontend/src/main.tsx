import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './styles/index.css'
import App from './App.tsx'
import Resume from './components/Resume.tsx'
import Account from './components/Account.tsx'
import Applications from './components/Applications.tsx'
import SessionProvider from './auth/SessionProvider'
import { NewJobPage, MyJobsPage } from './jobs/JobPages'
import ApplyJobPage from './jobs/ApplyJobPage'
import SearchJobs from './components/SearchJobs'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />} />
          <Route path="/account" element={<Account />} />
          <Route path="/account/resume" element={<Resume />} />
          <Route path="/account/applications" element={<Applications />} />
          <Route path="/jobs/new" element={<NewJobPage />} />
          <Route path="/account/jobs" element={<MyJobsPage />} />
          <Route path="/jobs/:jobId/apply" element={<ApplyJobPage />} />
          <Route path="/jobs" element={<SearchJobs />} />
        </Routes>
      </BrowserRouter>
    </SessionProvider>
  </StrictMode>,
)
