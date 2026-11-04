import { useTranslation } from 'react-i18next'

/** Shown while the session restore on first load has not answered yet. */
export function SessionLoading() {
  const { t } = useTranslation()
  return (
    <p role="status" className="flex-1 p-8 text-center text-sm text-slate-600">
      {t('common.loading')}
    </p>
  )
}
