import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authClient } from '../../api/client'
import { mockAdapter } from '../../test/axiosMock'
import { renderRoutes } from '../../test/renderRoutes'
import { ConfirmPage } from './ConfirmPage'

type ConfirmRequest = { token: string; password: string }

describe('ConfirmPage', () => {
  const originalAdapter = authClient.defaults.adapter
  let requests: ConfirmRequest[]

  beforeEach(() => {
    requests = []
    mockAdapter(authClient, (config) => {
      const request = JSON.parse(config.data as string) as ConfirmRequest
      requests.push(request)
      if (request.token !== 'good-token') {
        return { status: 400, data: { type: 'invalid-token', status: 400 } }
      }
      return request.password === 's3nh@forte'
        ? { status: 200, data: { status: 'ACTIVE' } }
        : { status: 422, data: { type: 'wrong-password', status: 422 } }
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

  async function submit(password: string) {
    const user = userEvent.setup()
    await user.clear(screen.getByLabelText('Senha'))
    await user.type(screen.getByLabelText('Senha'), password)
    await user.click(screen.getByRole('button', { name: 'Ativar conta' }))
  }

  it('removes the token from the URL as soon as the page opens', () => {
    renderAt('#token=good-token')

    expect(window.location.hash).toBe('')
    expect(window.location.pathname).toBe('/confirm')
    expect(requests).toHaveLength(0)
  })

  it('activates the account with the token and the password', async () => {
    renderAt('#token=good-token')
    await submit('s3nh@forte')

    expect(await screen.findByTestId('confirm-success')).toBeInTheDocument()
    expect(requests).toEqual([{ token: 'good-token', password: 's3nh@forte' }])
  })

  it('lets the user retry after a wrong password', async () => {
    renderAt('#token=good-token')
    await submit('senha-errada')

    expect(await screen.findByText(/Senha incorreta/)).toBeInTheDocument()
    expect(screen.getByLabelText('Senha')).toHaveAttribute('aria-invalid', 'true')

    await submit('s3nh@forte')
    expect(await screen.findByTestId('confirm-success')).toBeInTheDocument()
  })

  it('shows the link error for an invalid token', async () => {
    renderAt('#token=used-token')
    await submit('s3nh@forte')

    expect(await screen.findByTestId('confirm-error')).toHaveTextContent(
      'Link inválido ou expirado.',
    )
  })

  it('shows the link error without a form when there is no token', () => {
    renderAt('')

    expect(screen.getByTestId('confirm-error')).toBeInTheDocument()
    expect(screen.queryByLabelText('Senha')).not.toBeInTheDocument()
  })
})
