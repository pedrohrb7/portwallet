import { expect, type APIRequestContext } from '@playwright/test'

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
