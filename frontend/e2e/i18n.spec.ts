import { expect, test } from '@playwright/test'
import { uniqueEmail, validCpf } from './support/data.ts'
import { openInboxMessage } from './support/inbox.ts'

test.describe('in an English browser', () => {
  test.use({ locale: 'en-US' })

  test('registers and activates the account in English, e-mail included', async ({ page }) => {
    const email = uniqueEmail()
    const cpf = validCpf()

    await page.goto('/')
    await expect(page.locator('html')).toHaveAttribute('lang', 'en')
    await expect(page.getByTestId('demo-banner')).toHaveText('Demo environment, no real money')
    await expect(page.getByTestId('api-status')).toHaveText('Online')
    await page.getByRole('link', { name: 'Create account' }).click()
    await page.getByLabel('Full name').fill('Ana Souza Lima')
    await page.getByLabel('E-mail').fill(email)
    await page.getByLabel('CPF').fill(cpf)
    await page.getByRole('button', { name: 'Create account' }).click()

    // Written by the API, in the language the frontend asked for.
    await expect(page.getByTestId('register-success')).toHaveText(
      'If the details are correct, you will receive an e-mail to activate your account.',
    )

    await page.getByRole('link', { name: 'demo inbox' }).click()
    await page.getByTestId('inbox-email').fill(email)
    await page.getByRole('button', { name: 'View e-mails' }).click()
    const message = await openInboxMessage(page, 'Activate your PortWallet account')
    await expect(message).toContainText('Hello, Ana!')
    await expect(message).toContainText('Name: Ana Souza Lima')
    await message.getByRole('link', { name: 'Open link' }).click()

    const summary = page.getByTestId('confirm-summary')
    await expect(summary).toContainText('Are these your details?')
    await expect(summary).toContainText('Ana L****')
    await page.getByLabel('Create a password').fill('s3nh@forte')
    await page.getByLabel('Repeat the password').fill('s3nh@forte')
    await page.getByRole('button', { name: 'Activate account' }).click()

    await expect(page.getByTestId('confirm-success')).toContainText('Account activated.')
    await expect(page.getByRole('link', { name: 'Go to sign in' })).toBeVisible()
  })

  test('shows field errors from the API in English', async ({ page }) => {
    // Lets an invalid CPF reach the API, as a client without the frontend validation would.
    await page.route('**/api/v1/auth/register', async (route) => {
      const body = { ...route.request().postDataJSON(), document: '529.982.247-24' }
      await route.continue({ postData: JSON.stringify(body) })
    })

    await page.goto('/register')
    await page.getByLabel('Full name').fill('Ana Souza')
    await page.getByLabel('E-mail').fill(uniqueEmail())
    await page.getByLabel('CPF').fill(validCpf())
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page.getByText('invalid CPF')).toBeVisible()
  })
})

test('the chosen language wins over the browser language and survives a reload', async ({
  page,
}) => {
  await page.goto('/')
  await expect(page.getByTestId('demo-banner')).toHaveText(
    'Ambiente de demonstração, sem dinheiro real',
  )

  await page.getByRole('combobox', { name: 'Idioma' }).selectOption('fr')
  await expect(page.getByTestId('demo-banner')).toHaveText(
    'Environnement de démonstration, sans argent réel',
  )
  await page.reload()

  await expect(page.locator('html')).toHaveAttribute('lang', 'fr')
  await expect(page.getByTestId('demo-banner')).toHaveText(
    'Environnement de démonstration, sans argent réel',
  )
  await expect(page.getByRole('combobox', { name: 'Langue' })).toHaveValue('fr')
})
