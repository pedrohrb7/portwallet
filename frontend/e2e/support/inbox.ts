import { expect, type APIRequestContext, type Locator, type Page } from '@playwright/test'

type InboxMessage = { subject: string; link: string | null }

/** Waits for an e-mail with the given subject in the demo inbox and returns its link. */
export async function linkFromInbox(
  request: APIRequestContext,
  email: string,
  subject: string,
): Promise<string> {
  let link: string | null = null
  await expect
    .poll(async () => {
      const response = await request.get('/mock/inbox', { params: { to: email } })
      const messages = (await response.json()) as InboxMessage[]
      link = messages.find((message) => message.subject === subject)?.link ?? null
      return link
    })
    .not.toBeNull()
  return link!
}

/**
 * Expands the demo inbox message with the given subject, as a reader clicking it would, and returns it. Messages
 * are collapsed in the list, so their body and link are only visible after this.
 */
export async function openInboxMessage(page: Page, subject: string): Promise<Locator> {
  const message = page.getByTestId('inbox-message').filter({ hasText: subject })
  await message.getByRole('heading', { name: subject }).click()
  await expect(message.getByTestId('inbox-preview')).toBeHidden()
  return message
}
