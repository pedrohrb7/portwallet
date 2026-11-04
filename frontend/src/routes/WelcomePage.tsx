import { useTranslation } from 'react-i18next'
import { useSession } from '../features/auth/sessionStore'

/** Home page of a signed-in user until the FR-08 dashboard (balance and recent transactions) takes its place. */
export function WelcomePage() {
  const { t } = useTranslation()
  const session = useSession()

  return (
    <div className="flex flex-col gap-1">
      <h1 className="text-2xl font-semibold tracking-tight">
        {t('welcome.title', { name: session.user?.name ?? '' })}
      </h1>
      <p className="text-sm text-slate-600">{t('welcome.intro')}</p>
    </div>
  )
}
