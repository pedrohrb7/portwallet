import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Trans, useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { register as registerUser } from '../../api/auth'
import { problemKey, toProblem } from '../../api/problem'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { SERVER_ERROR, useFieldErrorText } from '../../i18n/fieldErrors'
import { formatCpf } from '../../lib/cpf'
import { demoInboxEnabled } from '../../lib/env'
import { registerSchema, type RegisterData, type RegisterInput } from './schemas'

type FieldName = keyof RegisterInput

const linkClass = 'font-medium text-slate-900 underline underline-offset-4'

export function RegisterPage() {
  const { t } = useTranslation()
  const errorText = useFieldErrorText()
  const form = useForm<RegisterInput, unknown, RegisterData>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched',
    defaultValues: { name: '', email: '', document: '' },
  })
  const { errors } = form.formState

  const mutation = useMutation({
    mutationFn: registerUser,
    onError: (error) => {
      const problem = toProblem(error)
      for (const fieldError of problem.errors ?? []) {
        if (fieldError.field in registerSchema.shape) {
          form.setError(fieldError.field as FieldName, {
            type: SERVER_ERROR,
            message: fieldError.message,
          })
        }
      }
    },
  })

  if (mutation.isSuccess) {
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">{t('register.checkEmail')}</h1>
        {/* Written by the API in the language the request was made in. */}
        <Alert tone="success" testId="register-success">
          {mutation.data}
        </Alert>
        {demoInboxEnabled && (
          <p className="text-sm text-slate-600">
            <Trans
              i18nKey="register.demoInboxNotice"
              components={{ anchor: <Link to="/inbox" className={linkClass} /> }}
            />
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
        <h1 className="text-2xl font-semibold tracking-tight">{t('register.title')}</h1>
        <p className="text-sm text-slate-600">{t('register.intro')}</p>
      </div>
      <form
        noValidate
        onSubmit={form.handleSubmit((data) => mutation.mutate(data))}
        className="flex flex-col gap-4"
        aria-label={t('register.formLabel')}
      >
        <TextField
          label={t('register.name')}
          autoComplete="name"
          error={errorText(errors.name)}
          {...form.register('name')}
        />
        <TextField
          label={t('register.email')}
          type="email"
          autoComplete="email"
          error={errorText(errors.email)}
          {...form.register('email')}
        />
        <TextField
          label={t('register.cpf')}
          inputMode="numeric"
          placeholder="000.000.000-00"
          hint={t('register.cpfHint')}
          error={errorText(errors.document)}
          {...documentField}
          onChange={(event) => {
            event.target.value = formatCpf(event.target.value)
            return documentField.onChange(event)
          }}
        />
        {problem && !problem.errors?.length && <Alert tone="error">{t(problemKey(problem))}</Alert>}
        <Button type="submit" loading={mutation.isPending} loadingLabel={t('register.submitting')}>
          {t('register.submit')}
        </Button>
      </form>
      <p className="text-sm text-slate-600">
        {t('register.haveAccount')}{' '}
        <Link to="/login" className={linkClass}>
          {t('register.signIn')}
        </Link>
      </p>
    </>
  )
}
