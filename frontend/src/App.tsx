import { ApiStatus } from './components/ApiStatus'
import { DemoBanner } from './components/DemoBanner'

export default function App() {
  return (
    <div className="flex min-h-dvh flex-col bg-slate-50 text-slate-900">
      <DemoBanner />
      <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center gap-4 px-4 py-12">
        <h1 className="text-3xl font-semibold tracking-tight">PortWallet</h1>
        <p className="text-slate-600">
          Carteira digital com idempotência, circuit breaker, rate limit e segurança.
        </p>
        <ApiStatus />
      </main>
    </div>
  )
}
