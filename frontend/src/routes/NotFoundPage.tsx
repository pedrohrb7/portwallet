import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center gap-4 px-4 py-12">
      <h1 className="text-2xl font-semibold tracking-tight">Página não encontrada</h1>
      <p className="text-slate-600">O endereço acessado não existe.</p>
      <Link to="/" className="font-medium text-slate-900 underline underline-offset-4">
        Voltar ao início
      </Link>
    </main>
  )
}
