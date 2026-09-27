import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import Resume from './Resume.tsx'
import ResumeList from './ResumeList.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App />} />
        <Route path="/account/resume" element={<Resume />} />
        <Route path="/account/resumes" element={<ResumeList />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)