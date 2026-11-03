import { useTranslation } from 'react-i18next'
import { Link, NavLink, Outlet } from 'react-router'
import { useSession } from '../features/auth/sessionStore'
import { navItems } from './navigation'

export function AppLayout() {
  const { t } = useTranslation()
  const session = useSession()

  return (
    <>
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3">
          <Link to="/" className="text-lg font-semibold tracking-tight">
            PortWallet
          </Link>
          {navItems.length > 0 && (
            <nav aria-label={t('nav.main')} className="order-last w-full sm:order-none sm:w-auto">
              <ul className="flex gap-4 text-sm">
                {navItems.map((item) => (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      end
                      className={({ isActive }) =>
                        isActive
                          ? 'font-medium text-slate-900'
                          : 'text-slate-600 hover:text-slate-900'
                      }
                    >
                      {t(item.label)}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </nav>
          )}
          {session.user && (
            <span className="text-sm text-slate-600" data-testid="current-user">
              {session.user.name}
            </span>
          )}
        </div>
      </header>
      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8">
        <Outlet />
      </main>
    </>
  )
}
