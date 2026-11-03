import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Trans, useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { confirmationSummary, confirmEmail } from '../../api/auth'
import { problemKey, toProblem, type Problem } from '../../api/problem'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { SERVER_ERROR, useFieldErrorText } from '../../i18n/fieldErrors'
import { activationSchema, type ActivationData } from './schemas'

/** Keeps a term such as "e-mail" from breaking at its hyphen (`<nowrap>` in the catalogs). */
const noWrap = <span className="whitespace-nowrap" />

const linkClass = 'font-medium text-slate-900 underline underline-offset-4'

function readTokenFromUrl(): string | null {
  return new URLSearchParams(window.location.hash.slice(1)).get('token')
}

function Heading() {
  const { t } = useTranslation()
  return <h1 className="text-2xl font-semibold tracking-tight">{t('confirm.title')}</h1>
}

function LinkError({ problem }: { problem: Problem }) {
  const { t } = useTranslation()
  return (
    <>
      <Heading />
      <Alert tone="error" testId="confirm-error">
        {t(problemKey(problem))}{' '}
        <Trans i18nKey="confirm.linkErrorHint" components={{ nowrap: noWrap }} />
      </Alert>
      <Link to="/register" className={`text-sm ${linkClass}`}>
        {t('confirm.backToRegister')}
      </Link>
    </>
  )
}

/** Removes the token from the address bar and the history, so it cannot be reused from there. */
function removeTokenFromUrl() {
  window.history.replaceState(window.history.state, '', window.location.pathname)
}

/**
 * Activation through the e-mail link. The page first shows the masked data of the registration, so the owner of the
 * e-mail notices if someone else replaced them, and then asks for the password of the new account.
 */
export function ConfirmPage() {
  // Read during the first render (a pure read, safe under Strict Mode); kept only in memory from then on.
  const [token, setToken] = useState(readTokenFromUrl)

  useEffect(() => {
    removeTokenFromUrl()
    // Opening another link in this same tab only changes the fragment, which does not reload the page.
    function onHashChange() {
      const next = readTokenFromUrl()
      if (next !== null) {
        setToken(next)
        removeTokenFromUrl()
      }
    }
    window.addEventListener('hashchange', onHashChange)
    return () => window.removeEventListener('hashchange', onHashChange)
  }, [])

  // A new token starts over, with no state left from the previous link.
  return <ActivationFlow key={token} token={token} />
}

function ActivationFlow({ token }: { token: string | null }) {
  const { t } = useTranslation()
  const errorText = useFieldErrorText()
  const summary = useQuery({
    queryKey: ['confirmation-summary', token],
    queryFn: () => confirmationSummary(token ?? ''),
    enabled: token !== null,
    retry: false,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })

  const form = useForm<ActivationData>({
    resolver: zodResolver(activationSchema),
    mode: 'onTouched',
    defaultValues: { password: '', passwordConfirmation: '' },
  })
  const { errors } = form.formState

  const activation = useMutation({
    mutationFn: (data: ActivationData) => confirmEmail(token ?? '', data.password),
    onError: (error) => {
      for (const fieldError of toProblem(error).errors ?? []) {
        if (fieldError.field === 'password') {
          form.setError('password', { type: SERVER_ERROR, message: fieldError.message })
        }
      }
    },
  })

  if (activation.isSuccess) {
    return (
      <>
        <Heading />
        <Alert tone="success" testId="confirm-success">
          <Trans i18nKey="confirm.success" components={{ nowrap: noWrap }} />
        </Alert>
        <Link
          to="/login"
          className="inline-flex w-fit items-center justify-center rounded-md bg-slate-900 px-4 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-slate-800"
        >
          {t('confirm.goToLogin')}
        </Link>
      </>
    )
  }

  if (token === null) {
    return <LinkError problem={{ type: 'invalid-token', status: 400 }} />
  }

  const summaryProblem = summary.isError ? toProblem(summary.error) : null
  const activationProblem = activation.isError ? toProblem(activation.error) : null
  const linkProblem = [summaryProblem, activationProblem].find(
    (problem) => problem?.type === 'invalid-token',
  )
  if (linkProblem) {
    return <LinkError problem={linkProblem} />
  }

  if (summary.isPending) {
    return (
      <>
        <Heading />
        <p role="status" className="text-sm text-slate-600">
          {t('confirm.loadingSummary')}
        </p>
      </>
    )
  }

  if (summary.isError) {
    return (
      <>
        <Heading />
        <Alert tone="error">{t(problemKey(toProblem(summary.error)))}</Alert>
        <Button
          type="button"
          className="w-fit"
          loading={summary.isFetching}
          loadingLabel={t('confirm.retrying')}
          onClick={() => void summary.refetch()}
        >
          {t('confirm.retry')}
        </Button>
      </>
    )
  }

  return (
    <>
      <div className="flex flex-col gap-1">
        <Heading />
        <p className="text-sm text-slate-600">{t('confirm.intro')}</p>
      </div>
      <section
        aria-labelledby="summary-title"
        data-testid="confirm-summary"
        className="flex flex-col gap-3 rounded-md border border-slate-200 bg-white p-4 shadow-sm"
      >
        <h2 id="summary-title" className="font-medium">
          {t('confirm.summaryTitle')}
        </h2>
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
          <dt className="text-slate-500">{t('confirm.name')}</dt>
          <dd className="font-medium text-slate-900">{summary.data.maskedName}</dd>
          <dt className="text-slate-500">{t('confirm.cpf')}</dt>
          <dd className="font-medium text-slate-900 tabular-nums">{summary.data.maskedDocument}</dd>
        </dl>
        <p className="text-sm text-slate-600">
          <Trans
            i18nKey="confirm.notYours"
            components={{ anchor: <Link to="/register" className={linkClass} /> }}
          />
        </p>
      </section>
      <form
        noValidate
        onSubmit={form.handleSubmit((data) => activation.mutate(data))}
        className="flex flex-col gap-4"
        aria-label={t('confirm.title')}
      >
        <TextField
          label={t('confirm.password')}
          type="password"
          autoComplete="new-password"
          hint={t('confirm.passwordHint')}
          error={errorText(errors.password)}
          {...form.register('password')}
        />
        <TextField
          label={t('confirm.passwordConfirmation')}
          type="password"
          autoComplete="new-password"
          error={errorText(errors.passwordConfirmation)}
          {...form.register('passwordConfirmation')}
        />
        {activationProblem && !activationProblem.errors?.length && (
          <Alert tone="error">{t(problemKey(activationProblem))}</Alert>
        )}
        <Button type="submit" loading={activation.isPending} loadingLabel={t('confirm.submitting')}>
          {t('confirm.submit')}
        </Button>
      </form>
    </>
  )
}
