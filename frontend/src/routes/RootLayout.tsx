import { useEffect } from 'react'
import { Outlet } from 'react-router'
import { refreshSession } from '../api/client'
import { DemoBanner } from '../components/DemoBanner'
import { sessionStore } from '../features/auth/sessionStore'

/** Shell shared by every page: the demo banner and the session restore on first load. */
export function RootLayout() {
  useEffect(() => {
    if (sessionStore.getState().status === 'loading') {
      void refreshSession()
    }
  }, [])

  return (
    <div className="flex min-h-dvh flex-col bg-slate-50 text-slate-900">
      <DemoBanner />
      <Outlet />
    </div>
  )
}
