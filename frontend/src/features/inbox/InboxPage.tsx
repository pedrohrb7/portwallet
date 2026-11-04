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

/** One-line preview of a body: its text with line breaks collapsed; the CSS cuts it to the card width. */
function preview(body: string): string {
  return body.replace(/\s+/g, ' ').trim()
}

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
              className="rounded-md border border-slate-200 bg-white shadow-sm"
            >
              {/* Native disclosure: collapsed by default, opened by click, Enter or Space on the summary. */}
              <details className="group">
                <summary className="flex cursor-pointer list-none items-start gap-3 rounded-md p-4 outline-none hover:bg-slate-50 focus-visible:ring-2 focus-visible:ring-slate-900/20 group-open:rounded-b-none [&::-webkit-details-marker]:hidden">
                  <div className="flex min-w-0 flex-1 flex-col gap-1">
                    {/* Stacked on narrow screens, so every card puts the date in the same place. */}
                    <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between sm:gap-4">
                      <h2 className="font-medium">{message.subject}</h2>
                      <time
                        dateTime={message.createdAt}
                        className="shrink-0 text-xs text-slate-500"
                      >
                        {dateFormat.format(new Date(message.createdAt))}
                      </time>
                    </div>
                    <p
                      className="truncate text-sm text-slate-500 group-open:hidden"
                      data-testid="inbox-preview"
                    >
                      {preview(message.body)}
                    </p>
                  </div>
                  <svg
                    aria-hidden="true"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                    className="mt-0.5 size-5 shrink-0 text-slate-400 transition-transform group-open:rotate-180"
                  >
                    <path
                      fillRule="evenodd"
                      d="M5.22 8.22a.75.75 0 0 1 1.06 0L10 11.94l3.72-3.72a.75.75 0 1 1 1.06 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L5.22 9.28a.75.75 0 0 1 0-1.06Z"
                      clipRule="evenodd"
                    />
                  </svg>
                </summary>
                <div className="flex flex-col gap-2 border-t border-slate-100 px-4 pt-3 pb-4">
                  <p className="text-sm break-words whitespace-pre-line text-slate-700">
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
                </div>
              </details>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
