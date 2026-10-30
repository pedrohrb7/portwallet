import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authClient } from '../../api/client'
import { mockAdapter } from '../../test/axiosMock'
import { renderRoutes } from '../../test/renderRoutes'
import { RegisterPage } from './RegisterPage'

const message = 'Se os dados estiverem corretos, você receberá um e-mail para ativar a conta.'

describe('RegisterPage', () => {
  let requests: unknown[]
  let respond: () => { status: number; data: unknown }
  const originalAdapter = authClient.defaults.adapter

  beforeEach(() => {
    requests = []
    respond = () => ({ status: 202, data: { message } })
    mockAdapter(authClient, (config) => {
      requests.push(JSON.parse(config.data as string))
      return respond()
    })
  })

  afterEach(() => {
    authClient.defaults.adapter = originalAdapter
  })

  async function fillValidForm() {
    const user = userEvent.setup()
    renderRoutes([{ path: '/register', element: <RegisterPage /> }], '/register')
    await user.type(screen.getByLabelText('Nome completo'), 'Ana Souza')
    await user.type(screen.getByLabelText('E-mail'), 'Ana@Demo.com')
    await user.type(screen.getByLabelText('CPF'), '52998224725')
    await user.type(screen.getByLabelText('Senha'), 's3nh@forte')
    return user
  }

  it('masks the CPF while typing', async () => {
    await fillValidForm()
    expect(screen.getByLabelText('CPF')).toHaveValue('529.982.247-25')
  })

  it('sends normalized data and shows the generic message', async () => {
    const user = await fillValidForm()
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByTestId('register-success')).toHaveTextContent(message)
    expect(requests).toEqual([
      {
        name: 'Ana Souza',
        email: 'ana@demo.com',
        document: '529.982.247-25',
        password: 's3nh@forte',
      },
    ])
  })

  it('validates fields before sending', async () => {
    const user = userEvent.setup()
    renderRoutes([{ path: '/register', element: <RegisterPage /> }], '/register')
    await user.type(screen.getByLabelText('CPF'), '52998224724')
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByText('CPF inválido')).toBeInTheDocument()
    expect(screen.getByLabelText('CPF')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Informe o nome completo')).toBeInTheDocument()
    expect(requests).toHaveLength(0)
  })

  it('shows field errors returned by the API', async () => {
    respond = () => ({
      status: 400,
      data: {
        type: 'validation-error',
        status: 400,
        errors: [{ field: 'email', message: 'e-mail recusado pelo servidor' }],
      },
    })
    const user = await fillValidForm()
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByText('e-mail recusado pelo servidor')).toBeInTheDocument()
  })

  it('shows a generic error when the server is unreachable', async () => {
    mockAdapter(authClient, () => undefined)
    const user = await fillValidForm()
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Sem conexão com o servidor')
  })
})
