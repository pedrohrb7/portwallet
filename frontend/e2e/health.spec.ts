import { expect, test } from '@playwright/test'

test('home page shows the demo banner and an online API', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByTestId('demo-banner')).toHaveText(
    'Ambiente de demonstração, sem dinheiro real',
  )
  await expect(page.getByRole('heading', { name: 'PortWallet' })).toBeVisible()
  await expect(page.getByTestId('api-status')).toHaveAttribute('data-status', 'UP')
})
