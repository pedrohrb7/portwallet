import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authClient } from '../../api/client'
import { mockAdapter } from '../../test/axiosMock'
import { renderRoutes } from '../../test/renderRoutes'
import { ConfirmPage } from './ConfirmPage'

type Request = { url: string; body: Record<string, string> }

const invalidToken = { status: 400, data: { type: 'invalid-token', status: 400 } }

describe('ConfirmPage', () => {
  const originalAdapter = authClient.defaults.adapter
  let requests: Request[]
  let networkDown: boolean

  beforeEach(() => {
    requests = []
    networkDown = false
    mockAdapter(authClient, (config) => {
      const request = { url: config.url ?? '', body: JSON.parse(config.data as string) }
      requests.push(request)
      if (networkDown) {
        return undefined
      }
      if (request.body.token !== 'good-token') {
        return invalidToken
      }
      if (request.url === '/auth/confirm/summary') {
        return { status: 200, data: { maskedName: 'Ana S****', maskedDocument: '***.***.***-25' } }
      }
      return { status: 200, data: { status: 'ACTIVE' } }
    })
  })

  afterEach(() => {
    authClient.defaults.adapter = originalAdapter
    window.history.replaceState(null, '', '/')
  })

  function renderAt(hash: string) {
    window.history.replaceState(null, '', `/confirm${hash}`)
    renderRoutes([{ path: '/confirm', element: <ConfirmPage /> }], '/confirm', { strict: true })
  }

  async function activate(password: string, confirmation = password) {
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Crie uma senha'), password)
    await user.type(screen.getByLabelText('Repita a senha'), confirmation)
    await user.click(screen.getByRole('button', { name: 'Ativar conta' }))
  }

  const activations = () => requests.filter((request) => request.url === '/auth/confirm')

  it('removes the token from the URL as soon as the page opens', () => {
    renderAt('#token=good-token')

    expect(window.location.hash).toBe('')
    expect(window.location.pathname).toBe('/confirm')
  })

  it('shows the masked data of the registration before activating', async () => {
    renderAt('#token=good-token')

    const summary = await screen.findByTestId('confirm-summary')
    expect(within(summary).getByRole('heading', { name: 'Estes são os seus dados?' })).toBeVisible()
    expect(summary).toHaveTextContent('Ana S****')
    expect(summary).toHaveTextContent('***.***.***-25')
    expect(within(summary).getByRole('link', { name: 'faça o cadastro de novo' })).toHaveAttribute(
      'href',
      '/register',
    )
    expect(requests).toEqual([{ url: '/auth/confirm/summary', body: { token: 'good-token' } }])
  })

  it('activates the account with the token and the new password', async () => {
    renderAt('#token=good-token')
    await activate('s3nh@forte')

    expect(await screen.findByTestId('confirm-success')).toBeInTheDocument()
    expect(activations()).toEqual([
      { url: '/auth/confirm', body: { token: 'good-token', password: 's3nh@forte' } },
    ])
  })

  it('requires both passwords to match without calling the API', async () => {
    renderAt('#token=good-token')
    await activate('s3nh@forte', 's3nh@fort')

    expect(await screen.findByText('As senhas não são iguais')).toBeInTheDocument()
    expect(screen.getByLabelText('Repita a senha')).toHaveAttribute('aria-invalid', 'true')
    expect(activations()).toHaveLength(0)
  })

  it('rejects a short password without calling the API', async () => {
    renderAt('#token=good-token')
    await activate('short')

    expect(await screen.findByText('Use pelo menos 8 caracteres')).toBeInTheDocument()
    expect(activations()).toHaveLength(0)
  })

  it('shows the link error when the link is no longer valid', async () => {
    renderAt('#token=used-token')

    expect(await screen.findByTestId('confirm-error')).toHaveTextContent(
      'Link inválido ou expirado.',
    )
    expect(screen.queryByLabelText('Crie uma senha')).not.toBeInTheDocument()
  })

  it('shows the link error when the link stops working before activation', async () => {
    renderAt('#token=good-token')
    await screen.findByTestId('confirm-summary')
    mockAdapter(authClient, () => invalidToken)
    await activate('s3nh@forte')

    expect(await screen.findByTestId('confirm-error')).toHaveTextContent(
      'Link inválido ou expirado.',
    )
  })

  it('starts over when another link is opened in the same tab', async () => {
    renderAt('#token=used-token')
    await screen.findByTestId('confirm-error')

    // A link opened in the same tab only changes the fragment, without reloading the page.
    act(() => {
      window.history.pushState(null, '', '/confirm#token=good-token')
      window.dispatchEvent(new HashChangeEvent('hashchange'))
    })

    expect(await screen.findByTestId('confirm-summary')).toHaveTextContent('Ana S****')
    expect(screen.queryByTestId('confirm-error')).not.toBeInTheDocument()
    expect(window.location.hash).toBe('')
  })

  it('offers a retry when the summary cannot be loaded', async () => {
    networkDown = true
    renderAt('#token=good-token')

    expect(await screen.findByRole('alert')).toHaveTextContent('Sem conexão com o servidor')
    expect(screen.queryByTestId('confirm-error')).not.toBeInTheDocument()

    networkDown = false
    await userEvent.setup().click(screen.getByRole('button', { name: 'Tentar de novo' }))
    expect(await screen.findByTestId('confirm-summary')).toHaveTextContent('Ana S****')
  })

  it('shows the link error without a form when there is no token', () => {
    renderAt('')

    expect(screen.getByTestId('confirm-error')).toBeInTheDocument()
    expect(screen.queryByLabelText('Crie uma senha')).not.toBeInTheDocument()
    expect(requests).toHaveLength(0)
  })
})
