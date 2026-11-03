import { useQuery } from '@tanstack/react-query'
import { useMemo, useState, type SubmitEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { fetchInbox } from '../../api/inbox'
import { problemKey, toProblem } from '../../api/problem'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { currentLanguage } from '../../i18n'

export function InboxPage() {
  const { t } = useTranslation()
  const language = currentLanguage()
  // Format of the language in use; the time zone is always the product's, whatever the language.
  const dateFormat = useMemo(
    () =>
      new Intl.DateTimeFormat(language, {
        dateStyle: 'short',
        timeStyle: 'short',
        timeZone: 'America/Sao_Paulo',
      }),
    [language],
  )
  const [searchParams, setSearchParams] = useSearchParams()
  const recipient = searchParams.get('to') ?? ''
  const [draft, setDraft] = useState(recipient)

  const inbox = useQuery({
    queryKey: ['inbox', recipient],
    queryFn: () => fetchInbox(recipient),
    enabled: recipient !== '',
  })

  function onSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const to = draft.trim().toLowerCase()
    if (to === recipient) {
      void inbox.refetch()
    } else {
      setSearchParams(to ? { to } : {})
    }
  }

  return (
    <>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t('inbox.title')}</h1>
        <p className="text-sm text-slate-600">{t('inbox.intro')}</p>
      </div>
      <form onSubmit={onSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="flex-1">
          <TextField
            label={t('inbox.email')}
            type="email"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            data-testid="inbox-email"
          />
        </div>
        <Button type="submit" loading={inbox.isFetching} loadingLabel={t('inbox.submitting')}>
          {t('inbox.submit')}
        </Button>
      </form>

      {inbox.isError && <Alert tone="error">{t(problemKey(toProblem(inbox.error)))}</Alert>}
      {inbox.isSuccess && inbox.data.length === 0 && (
        <Alert tone="info">{t('inbox.empty', { recipient })}</Alert>
      )}
      {inbox.isSuccess && inbox.data.length > 0 && (
        <ul className="flex flex-col gap-3" aria-label={t('inbox.listLabel')}>
          {inbox.data.map((message) => (
            <li
              key={message.id}
              data-testid="inbox-message"
              className="flex flex-col gap-2 rounded-md border border-slate-200 bg-white p-4 shadow-sm"
            >
              <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                <h2 className="font-medium">{message.subject}</h2>
                <time dateTime={message.createdAt} className="text-xs text-slate-500">
                  {dateFormat.format(new Date(message.createdAt))}
                </time>
              </div>
              <p className="whitespace-pre-line text-sm break-words text-slate-700">
                {message.body}
              </p>
              {message.link && (
                <a
                  href={message.link}
                  className="w-fit text-sm font-medium text-slate-900 underline underline-offset-4"
                >
                  {t('inbox.openLink')}
                </a>
              )}
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
