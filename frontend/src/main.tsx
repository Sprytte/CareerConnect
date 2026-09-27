import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import ResumePage from './ResumePage'
import SessionProvider from './auth/SessionProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />} />
          <Route path="/account/resume" element={<ResumePage />} />
        </Routes>
      </BrowserRouter>
    </SessionProvider>
  </StrictMode>,
)
