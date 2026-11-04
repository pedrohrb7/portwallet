import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Trans, useTranslation } from 'react-i18next'
import { Link, Navigate, useLocation } from 'react-router'
import { problemKey, toProblem } from '../../api/problem'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { SERVER_ERROR, useFieldErrorText } from '../../i18n/fieldErrors'
import type { LoginRedirectState } from '../../routes/RequireAuth'
import { login } from './session'
import { useSession } from './sessionStore'
import { loginSchema, type LoginData, type LoginInput } from './schemas'

type FieldName = keyof LoginInput

const linkClass = 'font-medium text-slate-900 underline underline-offset-4'

/** Keeps a term such as "e-mail" from breaking at its hyphen (`<nowrap>` in the catalogs). */
const noWrap = <span className="whitespace-nowrap" />

/**
 * Where to go after signing in: the page RequireAuth sent the user away from, or the home page. Only paths inside the
 * app are accepted, so the state can never send the user to another site.
 */
function redirectTarget(state: unknown): string {
  const from = (state as Partial<LoginRedirectState> | null)?.from
  return typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : '/'
}

export function LoginPage() {
  const { t } = useTranslation()
  const errorText = useFieldErrorText()
  const session = useSession()
  const location = useLocation()
  const form = useForm<LoginInput, unknown, LoginData>({
    resolver: zodResolver(loginSchema),
    mode: 'onTouched',
    defaultValues: { email: '', password: '' },
  })
  const { errors } = form.formState

  const mutation = useMutation({
    mutationFn: login,
    onError: (error) => {
      const problem = toProblem(error)
      for (const fieldError of problem.errors ?? []) {
        if (fieldError.field in loginSchema.shape) {
          form.setError(fieldError.field as FieldName, {
            type: SERVER_ERROR,
            message: fieldError.message,
          })
        }
      }
      if (problem.type === 'invalid-credentials') {
        // The e-mail stays, so only the password has to be typed again.
        form.resetField('password')
        form.setFocus('password')
      }
    },
  })

  // Signing in updates the session, and this render then takes the user on; an already signed-in user skips the form.
  if (session.status === 'authenticated') {
    return <Navigate to={redirectTarget(location.state)} replace />
  }

  const problem = mutation.isError ? toProblem(mutation.error) : null

  return (
    <>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t('login.title')}</h1>
        <p className="text-sm text-slate-600">
          <Trans i18nKey="login.intro" components={{ nowrap: noWrap }} />
        </p>
      </div>
      <form
        noValidate
        onSubmit={form.handleSubmit((data) => mutation.mutate(data))}
        className="flex flex-col gap-4"
        aria-label={t('login.title')}
      >
        <TextField
          label={t('login.email')}
          type="email"
          autoComplete="email"
          error={errorText(errors.email)}
          {...form.register('email')}
        />
        <TextField
          label={t('login.password')}
          type="password"
          autoComplete="current-password"
          error={errorText(errors.password)}
          {...form.register('password')}
        />
        {problem && !problem.errors?.length && (
          <Alert tone="error" testId="login-error">
            {t(problemKey(problem))}
          </Alert>
        )}
        <Button type="submit" loading={mutation.isPending} loadingLabel={t('login.submitting')}>
          {t('login.submit')}
        </Button>
      </form>
      <p className="text-sm text-slate-600">
        {t('login.noAccount')}{' '}
        <Link to="/register" className={linkClass}>
          {t('login.createAccount')}
        </Link>
      </p>
    </>
  )
}
