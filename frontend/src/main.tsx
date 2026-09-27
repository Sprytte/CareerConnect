import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import Resume from './Resume.tsx'
import SessionProvider from './auth/SessionProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />} />
          <Route path="/account/resume" element={<Resume />} />
        </Routes>
      </BrowserRouter>
    </SessionProvider>
  </StrictMode>,
)