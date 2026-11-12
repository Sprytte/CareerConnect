import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useSession } from './session'

// New accounts must choose a role before using either workspace. The backend
// also sends first-time users here after the Auth0 callback.
export default function RoleGate({ children }: { children: ReactNode }) {
  const session = useSession()
  const location = useLocation()

  if (
    session.status === 'signed-in' &&
    session.user.role === null &&
    location.pathname !== '/onboarding/role'
  ) {
    return <Navigate to="/onboarding/role" replace />
  }

  return <>{children}</>
}
