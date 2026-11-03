import { expect, test, type APIRequestContext, type Page } from '@playwright/test'
import { uniqueEmail, validCpf } from './support/data.ts'
import { linkFromInbox } from './support/inbox.ts'

const genericMessage =
  'Se os dados estiverem corretos, você receberá um e-mail para ativar a conta.'

async function choosePassword(page: Page, password: string) {
  await page.getByLabel('Crie uma senha').fill(password)
  await page.getByLabel('Repita a senha').fill(password)
  await page.getByRole('button', { name: 'Ativar conta' }).click()
}

test('registers and activates the account through the demo inbox', async ({ page }) => {
  const email = uniqueEmail()
  const cpf = validCpf()

  await page.goto('/')
  await page.getByRole('link', { name: 'Criar conta' }).click()
  await expect(page.getByLabel(/senha/i)).toHaveCount(0)
  await page.getByLabel('Nome completo').fill('Ana Souza Lima')
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('CPF').fill(cpf)
  await page.getByRole('button', { name: 'Criar conta' }).click()

  await expect(page.getByTestId('register-success')).toHaveText(genericMessage)

  await page.getByRole('link', { name: 'caixa de entrada demo' }).click()
  await page.getByTestId('inbox-email').fill(email)
  await page.getByRole('button', { name: 'Ver e-mails' }).click()
  const message = page
    .getByTestId('inbox-message')
    .filter({ hasText: 'Ative sua conta PortWallet' })
  await expect(message).toContainText('Nome: Ana Souza Lima')
  await expect(message).toContainText(`CPF: ***.***.***-${cpf.slice(9)}`)
  await message.getByRole('link', { name: 'Abrir link' }).click()

  // The token never stays in the address bar.
  await expect(page).toHaveURL(/\/confirm$/)
  const summary = page.getByTestId('confirm-summary')
  await expect(summary).toContainText('Estes são os seus dados?')
  await expect(summary).toContainText('Ana L****')
  await expect(summary).toContainText(`***.***.***-${cpf.slice(9)}`)
  await expect(summary).not.toContainText('Souza')
  await choosePassword(page, 's3nh@forte')

  await expect(page.getByTestId('confirm-success')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Ir para o login' })).toBeVisible()
})

async function registerByApi(
  request: APIRequestContext,
  email: string,
  name = 'Ana Souza',
): Promise<string> {
  const response = await request.post('/api/v1/auth/register', {
    data: { name, email, document: validCpf() },
  })
  expect(response.status()).toBe(202)
  return linkFromInbox(request, email, 'Ative sua conta PortWallet')
}

test('a confirmation link works only once', async ({ page, request }) => {
  const link = await registerByApi(request, uniqueEmail())

  await page.goto(link)
  await choosePassword(page, 's3nh@forte')
  await expect(page.getByTestId('confirm-success')).toBeVisible()

  // Opening the e-mail link again starts a new page, as a click in the e-mail client would.
  const secondVisit = await page.context().newPage()
  await secondVisit.goto(link)
  await expect(secondVisit.getByTestId('confirm-error')).toContainText('Link inválido ou expirado.')
  await expect(secondVisit.getByLabel('Crie uma senha')).toHaveCount(0)
})

test('registering again replaces the data shown by the newest link', async ({ page, request }) => {
  const email = uniqueEmail()
  // The owner registers; someone else then registers the same e-mail with other data.
  const ownerLink = await registerByApi(request, email, 'Ana Souza')
  // The inbox lists the newest message first, so this is the link of the second registration.
  const newestLink = await registerByApi(request, email, 'Carlos Atacante')
  expect(newestLink).not.toBe(ownerLink)

  // The owner's first link no longer works.
  await page.goto(ownerLink)
  await expect(page.getByTestId('confirm-error')).toBeVisible()

  // The newest link shows someone else's data, and points the owner to register again.
  await page.goto(newestLink)
  await expect(page.getByTestId('confirm-summary')).toContainText('Carlos A****')
  await page.getByRole('link', { name: 'faça o cadastro de novo' }).click()
  await expect(page).toHaveURL(/\/register$/)
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

test('activation checks the password confirmation without calling the API', async ({
  page,
  request,
}) => {
  const link = await registerByApi(request, uniqueEmail())
  let confirmCalls = 0
  await page.route('**/api/v1/auth/confirm', (route) => {
    confirmCalls++
    return route.continue()
  })

  await page.goto(link)
  await page.getByLabel('Crie uma senha').fill('s3nh@forte')
  await page.getByLabel('Repita a senha').fill('outra-senha')
  await page.getByRole('button', { name: 'Ativar conta' }).click()

  await expect(page.getByText('As senhas não são iguais')).toBeVisible()
  await expect(page.getByLabel('Repita a senha')).toHaveAttribute('aria-invalid', 'true')
  expect(confirmCalls).toBe(0)
})
