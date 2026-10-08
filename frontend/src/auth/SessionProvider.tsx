import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import {
  checkSession,
  SessionContext,
  SessionUserUpdateContext,
} from './session'
import type { SessionState, SessionUser } from './session'

type SessionProviderProps = {
  children: ReactNode
}

export default function SessionProvider({ children }: SessionProviderProps) {
  const [session, setSession] = useState<SessionState>({
    status: 'loading',
  })

  const updateSessionUser = useCallback((user: SessionUser) => {
    setSession((current) =>
      current.status === 'signed-in' && current.user.userId === user.userId
        ? { status: 'signed-in', user }
        : current,
    )
  }, [])

  useEffect(() => {
    const controller = new AbortController()

    checkSession(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setSession(result)
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setSession({ status: 'unavailable' })
        }
      })

    // Prevent an obsolete request from updating state after cleanup.
    return () => controller.abort()
  }, [])

  return (
    <SessionContext.Provider value={session}>
      <SessionUserUpdateContext.Provider value={updateSessionUser}>
        {children}
      </SessionUserUpdateContext.Provider>
    </SessionContext.Provider>
  )
}
