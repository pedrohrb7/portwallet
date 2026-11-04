import { expect, test, type Page } from '@playwright/test'
import { uniqueEmail } from './support/data.ts'
import { createActiveUser } from './support/users.ts'

const genericError = 'E-mail ou senha inválidos.'

async function signIn(page: Page, email: string, password: string) {
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill(password)
  await page.getByRole('button', { name: 'Entrar' }).click()
}

test('signs in from the home page and stays signed in after a reload', async ({
  page,
  request,
}) => {
  const user = await createActiveUser(request, 'Ana Souza Lima')

  await page.goto('/')
  await page.getByRole('link', { name: 'Entrar' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await signIn(page, user.email.toUpperCase(), user.password)

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('heading', { name: 'Olá, Ana Souza Lima' })).toBeVisible()
  await expect(page.getByTestId('current-user')).toHaveText('Ana Souza Lima')

  // The access token lives only in memory; the reload restores the session from the refresh cookie.
  await page.reload()
  await expect(page.getByTestId('current-user')).toHaveText('Ana Souza Lima')
  await expect(page.getByRole('link', { name: 'Criar conta' })).toHaveCount(0)

  // Nothing about the session is kept where JavaScript can read it (only the language choice is stored).
  expect(await page.evaluate<string>('document.cookie')).toBe('')
  const localKeys = await page.evaluate<string[]>('Object.keys(localStorage)')
  expect(localKeys.filter((key) => key !== 'portwallet.language')).toEqual([])
  expect(await page.evaluate<string[]>('Object.keys(sessionStorage)')).toEqual([])
})

test('an already signed-in user opening /login goes to the home page', async ({
  page,
  request,
}) => {
  const user = await createActiveUser(request)
  await page.goto('/login')
  await signIn(page, user.email, user.password)
  await expect(page.getByTestId('current-user')).toBeVisible()

  await page.goto('/login')

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByTestId('current-user')).toHaveText(user.name)
})

test('a wrong password and an unknown e-mail get the same generic message', async ({
  page,
  request,
}) => {
  const user = await createActiveUser(request)
  await page.goto('/login')

  await signIn(page, user.email, 'senha-errada')
  await expect(page.getByTestId('login-error')).toHaveText(genericError)
  await expect(page.getByLabel('E-mail')).toHaveValue(user.email)
  await expect(page.getByLabel('Senha')).toHaveValue('')
  await expect(page.getByLabel('Senha')).toBeFocused()

  await page.getByLabel('E-mail').fill(uniqueEmail())
  await page.getByLabel('Senha').fill(user.password)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByTestId('login-error')).toHaveText(genericError)
  await expect(page).toHaveURL(/\/login$/)
})

test('a fresh page without a session shows the public home page', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'PortWallet' })).toBeVisible()
  await expect(page.getByTestId('current-user')).toHaveCount(0)
})
