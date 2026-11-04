import { expect, test } from '@playwright/test'
import { createActiveUser } from './support/users.ts'

test('messages are listed collapsed and each opens on its own, by mouse or keyboard', async ({
  page,
  request,
}) => {
  const user = await createActiveUser(request)
  await page.goto(`/inbox?to=${encodeURIComponent(user.email)}`)

  const messages = page.getByTestId('inbox-message')
  await expect(messages).toHaveCount(2)
  const [activated, activation] = [messages.nth(0), messages.nth(1)]
  for (const message of [activated, activation]) {
    await expect(message.getByTestId('inbox-preview')).toBeVisible()
    await expect(message.getByRole('link', { name: 'Abrir link' })).toBeHidden()
  }
  await expect(activated.getByTestId('inbox-preview')).toContainText('Olá, Ana!')

  await activated.getByRole('heading', { name: 'Sua conta PortWallet está ativa' }).click()
  await expect(activated.getByRole('link', { name: 'Abrir link' })).toBeVisible()
  await expect(activated.getByTestId('inbox-preview')).toBeHidden()
  await expect(activation.getByRole('link', { name: 'Abrir link' })).toBeHidden()

  // The same from the keyboard: Enter opens the focused message and Space closes it.
  await activation.locator('summary').focus()
  await page.keyboard.press('Enter')
  await expect(activation.getByRole('link', { name: 'Abrir link' })).toBeVisible()
  await page.keyboard.press('Space')
  await expect(activation.getByRole('link', { name: 'Abrir link' })).toBeHidden()

  // Clicking an open message closes it.
  await activated.getByRole('heading', { name: 'Sua conta PortWallet está ativa' }).click()
  await expect(activated.getByRole('link', { name: 'Abrir link' })).toBeHidden()
})
