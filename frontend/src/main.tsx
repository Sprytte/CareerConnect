import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './styles/index.css'
import App from './App.tsx'
import Resume from './components/Resume.tsx'
import CreatedJobPostings from './components/CreatedJobPostings'
import SessionProvider from './auth/SessionProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />} />
          <Route path="/account/resume" element={<Resume />} />
          <Route
            path="/account/job-postings"
            element={<CreatedJobPostings />}
          />
        </Routes>
      </BrowserRouter>
    </SessionProvider>
  </StrictMode>,
)