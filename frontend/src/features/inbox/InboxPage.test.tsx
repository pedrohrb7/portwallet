import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { InboxMessage } from '../../api/inbox'
import { rootClient } from '../../api/client'
import { mockAdapter } from '../../test/axiosMock'
import { renderRoutes } from '../../test/renderRoutes'
import { InboxPage } from './InboxPage'

const messages: InboxMessage[] = [
  {
    id: 'm-2',
    to: 'ana@demo.com',
    subject: 'Sua conta PortWallet está ativa',
    body: 'Olá, Ana!\n\nSua conta PortWallet foi ativada.\n\nEntre: http://localhost:7000/login',
    link: 'http://localhost:7000/login',
    createdAt: '2026-11-04T19:35:00Z',
  },
  {
    id: 'm-1',
    to: 'ana@demo.com',
    subject: 'Ative sua conta PortWallet',
    body: 'Olá, Ana!\n\nRecebemos um cadastro.',
    link: 'http://localhost:7000/confirm#token=abc',
    createdAt: '2026-11-04T19:34:00Z',
  },
]

describe('InboxPage', () => {
  const originalAdapter = rootClient.defaults.adapter

  beforeEach(() => {
    mockAdapter(rootClient, () => ({ status: 200, data: messages }))
  })

  afterEach(() => {
    rootClient.defaults.adapter = originalAdapter
  })

  async function renderInbox() {
    renderRoutes([{ path: '/inbox', element: <InboxPage /> }], '/inbox?to=ana@demo.com')
    return screen.findAllByTestId('inbox-message')
  }

  it('lists the messages collapsed, with subject, date and a one-line preview', async () => {
    const [newest, oldest] = await renderInbox()

    expect(within(newest).getByRole('heading')).toHaveTextContent('Sua conta PortWallet está ativa')
    expect(within(newest).getByText('04/11/2026, 16:35')).toBeInTheDocument()
    expect(within(newest).getByTestId('inbox-preview')).toHaveTextContent(
      'Olá, Ana! Sua conta PortWallet foi ativada. Entre: http://localhost:7000/login',
    )
    expect(within(oldest).getByRole('heading')).toHaveTextContent('Ative sua conta PortWallet')
    for (const message of [newest, oldest]) {
      expect(message.querySelector('details')).not.toHaveAttribute('open')
    }
  })

  it('opens a message to show its body and link, leaving the others collapsed', async () => {
    const user = userEvent.setup()
    const [newest, oldest] = await renderInbox()

    await user.click(within(newest).getByText('Sua conta PortWallet está ativa'))

    expect(newest.querySelector('details')).toHaveAttribute('open')
    expect(within(newest).getByRole('link', { name: 'Abrir link' })).toHaveAttribute(
      'href',
      'http://localhost:7000/login',
    )
    expect(oldest.querySelector('details')).not.toHaveAttribute('open')
  })

  it('reaches each message from the keyboard', async () => {
    const user = userEvent.setup()
    const [newest] = await renderInbox()

    await user.tab() // e-mail field
    await user.tab() // search button
    await user.tab() // first message

    // Opening with Enter or Space is browser behavior of <summary>, covered by the E2E suite.
    expect(newest.querySelector('summary')).toHaveFocus()
  })
})
