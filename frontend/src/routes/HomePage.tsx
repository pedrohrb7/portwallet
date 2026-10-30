import { Link } from 'react-router'
import { ApiStatus } from '../components/ApiStatus'
import { demoInboxEnabled } from '../lib/env'

export function HomePage() {
  return (
    <>
      <h1 className="text-3xl font-semibold tracking-tight">PortWallet</h1>
      <p className="text-slate-600">
        Carteira digital com idempotência, circuit breaker, rate limit e segurança.
      </p>
      <ApiStatus />
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <Link
          to="/register"
          className="inline-flex items-center justify-center rounded-md bg-slate-900 px-4 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-slate-800"
        >
          Criar conta
        </Link>
        {demoInboxEnabled && (
          <Link
            to="/inbox"
            className="text-sm font-medium text-slate-900 underline underline-offset-4"
          >
            Caixa de entrada demo
          </Link>
        )}
      </div>
    </>
  )
}
