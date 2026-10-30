import { useMutation } from '@tanstack/react-query'
import { useEffect, useState, type SubmitEvent } from 'react'
import { Link } from 'react-router'
import { confirmEmail } from '../../api/auth'
import { messageFor, toProblem } from '../../api/problem'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'

function readTokenFromUrl(): string | null {
  return new URLSearchParams(window.location.hash.slice(1)).get('token')
}

/**
 * Activation needs the e-mail link and the password chosen at registration, so nobody can register someone else's
 * e-mail with a password of their own and have the owner activate it.
 */
export function ConfirmPage() {
  // Read during the first render (a pure read, safe under Strict Mode); kept only in memory from then on.
  const [token] = useState(readTokenFromUrl)
  const [password, setPassword] = useState('')

  useEffect(() => {
    // Removes the token from the address bar and the history, so it cannot be reused from there.
    window.history.replaceState(window.history.state, '', window.location.pathname)
  }, [])

  const confirmation = useMutation({
    mutationFn: () => confirmEmail(token ?? '', password),
  })

  const problem = confirmation.isError ? toProblem(confirmation.error) : null
  const linkIsInvalid = !token || (problem !== null && problem.type !== 'wrong-password')

  function onSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    confirmation.mutate()
  }

  if (confirmation.isSuccess) {
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">Ativação da conta</h1>
        <Alert tone="success" testId="confirm-success">
          Conta ativada. Você já pode entrar.
        </Alert>
        <Link
          to="/login"
          className="inline-flex w-fit items-center justify-center rounded-md bg-slate-900 px-4 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-slate-800"
        >
          Ir para o login
        </Link>
      </>
    )
  }

  if (linkIsInvalid) {
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">Ativação da conta</h1>
        <Alert tone="error" testId="confirm-error">
          {messageFor(problem ?? { type: 'invalid-token', status: 400 })} Peça um novo link fazendo
          o cadastro de novo com o mesmo e-mail.
        </Alert>
        <Link
          to="/register"
          className="text-sm font-medium text-slate-900 underline underline-offset-4"
        >
          Voltar ao cadastro
        </Link>
      </>
    )
  }

  return (
    <>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Ativação da conta</h1>
        <p className="text-sm text-slate-600">
          Para confirmar que foi você quem se cadastrou, digite a senha escolhida no cadastro.
        </p>
      </div>
      <form onSubmit={onSubmit} className="flex flex-col gap-4" aria-label="Ativação da conta">
        <TextField
          label="Senha"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={problem ? messageFor(problem) : undefined}
          required
        />
        <Button
          type="submit"
          loading={confirmation.isPending}
          loadingLabel="Ativando..."
          disabled={password === ''}
        >
          Ativar conta
        </Button>
      </form>
    </>
  )
}
