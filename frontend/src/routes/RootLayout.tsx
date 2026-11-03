import { useEffect } from 'react'
import { Outlet } from 'react-router'
import { refreshSession } from '../api/client'
import { DemoBanner } from '../components/DemoBanner'
import { LanguageSwitcher } from '../components/LanguageSwitcher'
import { sessionStore } from '../features/auth/sessionStore'

/** Shell shared by every page: the demo banner, the language switcher and the session restore on first load. */
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
      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-4xl justify-end px-4 py-3">
          <LanguageSwitcher />
        </div>
      </footer>
    </div>
  )
}
