import type { ReactNode } from 'react'

type AlertProps = {
  tone: 'error' | 'success' | 'info'
  children: ReactNode
  testId?: string
}

const tones = {
  error: 'border-red-200 bg-red-50 text-red-800',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  info: 'border-slate-200 bg-white text-slate-700',
}

export function Alert({ tone, children, testId }: AlertProps) {
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      data-testid={testId}
      className={`rounded-md border px-4 py-3 text-sm ${tones[tone]}`}
    >
      {children}
    </div>
  )
}
