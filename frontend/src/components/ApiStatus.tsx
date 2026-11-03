import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchHealth, type HealthStatus } from '../api/health'

type State = HealthStatus | 'LOADING'

const dotColors: Record<State, string> = {
  LOADING: 'bg-slate-400',
  UP: 'bg-emerald-500',
  DOWN: 'bg-red-500',
  UNKNOWN: 'bg-red-500',
}

export function ApiStatus() {
  const { t } = useTranslation()
  const [status, setStatus] = useState<State>('LOADING')

  useEffect(() => {
    const controller = new AbortController()
    fetchHealth(controller.signal)
      .then(setStatus)
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          console.error(error)
          setStatus('UNKNOWN')
        }
      })
    return () => controller.abort()
  }, [])

  return (
    <p className="flex items-center gap-2 text-sm text-slate-600" role="status" aria-live="polite">
      <span aria-hidden="true" className={`size-2.5 rounded-full ${dotColors[status]}`} />
      <span>
        {t('apiStatus.label')}{' '}
        <span data-testid="api-status" data-status={status}>
          {t(`apiStatus.${status}`)}
        </span>
      </span>
    </p>
  )
}
