import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { register as registerUser } from '../../api/auth'
import { messageFor, toProblem } from '../../api/problem'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { formatCpf } from '../../lib/cpf'
import { demoInboxEnabled } from '../../lib/env'
import { registerSchema, type RegisterData, type RegisterInput } from './schemas'

type FieldName = keyof RegisterInput

export function RegisterPage() {
  const form = useForm<RegisterInput, unknown, RegisterData>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched',
    defaultValues: { name: '', email: '', document: '', password: '' },
  })
  const { errors } = form.formState

  const mutation = useMutation({
    mutationFn: registerUser,
    onError: (error) => {
      const problem = toProblem(error)
      for (const fieldError of problem.errors ?? []) {
        if (fieldError.field in registerSchema.shape) {
          form.setError(fieldError.field as FieldName, { message: fieldError.message })
        }
      }
    },
  })

  if (mutation.isSuccess) {
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">Verifique seu e-mail</h1>
        <Alert tone="success" testId="register-success">
          {mutation.data}
        </Alert>
        {demoInboxEnabled && (
          <p className="text-sm text-slate-600">
            Neste ambiente de demonstração, os e-mails chegam na{' '}
            <Link to="/inbox" className="font-medium text-slate-900 underline underline-offset-4">
              caixa de entrada demo
            </Link>
            .
          </p>
        )}
      </>
    )
  }

  const problem = mutation.isError ? toProblem(mutation.error) : null
  const documentField = form.register('document')

  return (
    <>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Criar conta</h1>
        <p className="text-sm text-slate-600">
          Use dados fictícios: este é um ambiente de demonstração.
        </p>
      </div>
      <form
        noValidate
        onSubmit={form.handleSubmit((data) => mutation.mutate(data))}
        className="flex flex-col gap-4"
        aria-label="Cadastro"
      >
        <TextField
          label="Nome completo"
          autoComplete="name"
          error={errors.name?.message}
          {...form.register('name')}
        />
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...form.register('email')}
        />
        <TextField
          label="CPF"
          inputMode="numeric"
          placeholder="000.000.000-00"
          hint="Gere um CPF fictício válido para testar."
          error={errors.document?.message}
          {...documentField}
          onChange={(event) => {
            event.target.value = formatCpf(event.target.value)
            return documentField.onChange(event)
          }}
        />
        <TextField
          label="Senha"
          type="password"
          autoComplete="new-password"
          hint="Mínimo de 8 caracteres."
          error={errors.password?.message}
          {...form.register('password')}
        />
        {problem && !problem.errors?.length && <Alert tone="error">{messageFor(problem)}</Alert>}
        <Button type="submit" loading={mutation.isPending} loadingLabel="Enviando...">
          Criar conta
        </Button>
      </form>
      <p className="text-sm text-slate-600">
        Já tem conta?{' '}
        <Link to="/login" className="font-medium text-slate-900 underline underline-offset-4">
          Entrar
        </Link>
      </p>
    </>
  )
}
