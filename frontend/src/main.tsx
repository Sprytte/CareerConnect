import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './styles/index.css'
import App from './App.tsx'
import Resume from './components/Resume.tsx'
import Account from './components/Account.tsx'
import SessionProvider from './auth/SessionProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />} />
          <Route path="/account" element={<Account />} />
          <Route path="/account/resume" element={<Resume />} />
        </Routes>
      </BrowserRouter>
    </SessionProvider>
  </StrictMode>,
)
