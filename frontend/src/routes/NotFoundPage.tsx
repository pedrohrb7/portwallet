import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

export function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center gap-4 px-4 py-12">
      <h1 className="text-2xl font-semibold tracking-tight">{t('notFound.title')}</h1>
      <p className="text-slate-600">{t('notFound.description')}</p>
      <Link to="/" className="font-medium text-slate-900 underline underline-offset-4">
        {t('notFound.backHome')}
      </Link>
    </main>
  )
}
