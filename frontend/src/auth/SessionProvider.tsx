import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { checkSession, SessionContext } from './session'
import type { SessionState } from './session'

type SessionProviderProps = {
  children: ReactNode
}

export default function SessionProvider({
  children,
}: SessionProviderProps) {
  const [session, setSession] = useState<SessionState>({
    status: 'loading',
  })

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
      {children}
    </SessionContext.Provider>
  )
}