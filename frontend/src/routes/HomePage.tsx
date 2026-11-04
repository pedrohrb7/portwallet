import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { ApiStatus } from '../components/ApiStatus'
import { demoInboxEnabled } from '../lib/env'

export function HomePage() {
  const { t } = useTranslation()
  return (
    <>
      <h1 className="text-3xl font-semibold tracking-tight">PortWallet</h1>
      <p className="text-slate-600">{t('home.tagline')}</p>
      <ApiStatus />
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <Link
          to="/register"
          className="inline-flex items-center justify-center rounded-md border border-transparent bg-slate-900 px-4 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-slate-800"
        >
          {t('home.createAccount')}
        </Link>
        <Link
          to="/login"
          className="inline-flex items-center justify-center rounded-md border border-slate-300 bg-white px-4 py-2.5 text-sm font-medium text-slate-900 shadow-sm hover:bg-slate-100"
        >
          {t('home.signIn')}
        </Link>
        {demoInboxEnabled && (
          <Link
            to="/inbox"
            className="text-sm font-medium text-slate-900 underline underline-offset-4"
          >
            {t('home.demoInbox')}
          </Link>
        )}
      </div>
    </>
  )
}
