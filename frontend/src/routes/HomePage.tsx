import { ApiStatus } from '../components/ApiStatus'

export function HomePage() {
  return (
    <>
      <h1 className="text-3xl font-semibold tracking-tight">PortWallet</h1>
      <p className="text-slate-600">
        Carteira digital com idempotência, circuit breaker, rate limit e segurança.
      </p>
      <ApiStatus />
    </>
  )
}
