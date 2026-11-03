import { useTranslation } from 'react-i18next'

export function DemoBanner() {
  const { t } = useTranslation()
  return (
    <div
      role="note"
      data-testid="demo-banner"
      className="bg-amber-100 px-4 py-2 text-center text-sm font-medium text-amber-900"
    >
      {t('common.demoBanner')}
    </div>
  )
}
