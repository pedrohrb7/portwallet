import { useQuery } from '@tanstack/react-query'
import { useState, type SubmitEvent } from 'react'
import { useSearchParams } from 'react-router'
import { fetchInbox } from '../../api/inbox'
import { messageFor, toProblem } from '../../api/problem'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'

const dateFormat = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short',
  timeZone: 'America/Sao_Paulo',
})

export function InboxPage() {
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
        <h1 className="text-2xl font-semibold tracking-tight">Caixa de entrada demo</h1>
        <p className="text-sm text-slate-600">
          E-mails enviados pelo serviço de notificação simulado. Só existe no ambiente de
          demonstração.
        </p>
      </div>
      <form onSubmit={onSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="flex-1">
          <TextField
            label="E-mail"
            type="email"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            data-testid="inbox-email"
          />
        </div>
        <Button type="submit" loading={inbox.isFetching} loadingLabel="Buscando...">
          Ver e-mails
        </Button>
      </form>

      {inbox.isError && <Alert tone="error">{messageFor(toProblem(inbox.error))}</Alert>}
      {inbox.isSuccess && inbox.data.length === 0 && (
        <Alert tone="info">Nenhum e-mail para {recipient}.</Alert>
      )}
      {inbox.isSuccess && inbox.data.length > 0 && (
        <ul className="flex flex-col gap-3" aria-label="Mensagens">
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
                  Abrir link
                </a>
              )}
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
