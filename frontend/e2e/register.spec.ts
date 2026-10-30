import { expect, test, type APIRequestContext } from '@playwright/test'
import { uniqueEmail, validCpf } from './support/data.ts'
import { linkFromInbox } from './support/inbox.ts'

const genericMessage =
  'Se os dados estiverem corretos, você receberá um e-mail para ativar a conta.'

test('registers and confirms the account through the demo inbox', async ({ page }) => {
  const email = uniqueEmail()

  await page.goto('/')
  await page.getByRole('link', { name: 'Criar conta' }).click()
  await page.getByLabel('Nome completo').fill('Ana Souza')
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('CPF').fill(validCpf())
  await page.getByLabel('Senha').fill('s3nh@forte')
  await page.getByRole('button', { name: 'Criar conta' }).click()

  await expect(page.getByTestId('register-success')).toHaveText(genericMessage)

  await page.getByRole('link', { name: 'caixa de entrada demo' }).click()
  await page.getByTestId('inbox-email').fill(email)
  await page.getByRole('button', { name: 'Ver e-mails' }).click()
  const message = page
    .getByTestId('inbox-message')
    .filter({ hasText: 'Ative sua conta PortWallet' })
  await expect(message).toBeVisible()
  await message.getByRole('link', { name: 'Abrir link' }).click()

  // The token never stays in the address bar.
  await expect(page).toHaveURL(/\/confirm$/)
  await page.getByLabel('Senha').fill('s3nh@forte')
  await page.getByRole('button', { name: 'Ativar conta' }).click()

  await expect(page.getByTestId('confirm-success')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Ir para o login' })).toBeVisible()
})

async function registerByApi(
  request: APIRequestContext,
  email: string,
  password: string,
): Promise<string> {
  const response = await request.post('/api/v1/auth/register', {
    data: { name: 'Ana Souza', email, document: validCpf(), password },
  })
  expect(response.status()).toBe(202)
  return linkFromInbox(request, email, 'Ative sua conta PortWallet')
}

test('a confirmation link works only once', async ({ page, request }) => {
  const link = await registerByApi(request, uniqueEmail(), 's3nh@forte')

  await page.goto(link)
  await page.getByLabel('Senha').fill('s3nh@forte')
  await page.getByRole('button', { name: 'Ativar conta' }).click()
  await expect(page.getByTestId('confirm-success')).toBeVisible()

  // Opening the e-mail link again starts a new page, as a click in the e-mail client would.
  const secondVisit = await page.context().newPage()
  await secondVisit.goto(link)
  await secondVisit.getByLabel('Senha').fill('s3nh@forte')
  await secondVisit.getByRole('button', { name: 'Ativar conta' }).click()
  await expect(secondVisit.getByTestId('confirm-error')).toContainText('Link inválido ou expirado.')
})

test('activation requires the password of the latest registration', async ({ page, request }) => {
  const email = uniqueEmail()
  // Someone pre-registers the e-mail with a password of their own; the owner registers later.
  await registerByApi(request, email, 'senha-do-atacante')
  // The inbox lists the newest message first, so this is the link sent to the owner.
  const ownerLink = await registerByApi(request, email, 'senha-da-dona')

  await page.goto(ownerLink)
  await page.getByLabel('Senha').fill('senha-do-atacante')
  await page.getByRole('button', { name: 'Ativar conta' }).click()
  await expect(page.getByText(/Senha incorreta/)).toBeVisible()

  await page.getByLabel('Senha').fill('senha-da-dona')
  await page.getByRole('button', { name: 'Ativar conta' }).click()
  await expect(page.getByTestId('confirm-success')).toBeVisible()
})

test('shows field errors without calling the API', async ({ page }) => {
  let registerCalls = 0
  await page.route('**/api/v1/auth/register', (route) => {
    registerCalls++
    return route.continue()
  })

  await page.goto('/register')
  await page.getByLabel('CPF').fill('529.982.247-24')
  await page.getByRole('button', { name: 'Criar conta' }).click()

  await expect(page.getByText('CPF inválido')).toBeVisible()
  await expect(page.getByLabel('CPF')).toHaveAttribute('aria-invalid', 'true')
  expect(registerCalls).toBe(0)
})
