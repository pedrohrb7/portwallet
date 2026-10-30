import axios from 'axios'

export type InboxMessage = {
  id: string
  to: string
  subject: string
  body: string
  link: string | null
  createdAt: string
}

/** Demo inbox of the simulated notification service (outside /api; only in dev, e2e and demo). */
export async function fetchInbox(to: string): Promise<InboxMessage[]> {
  const response = await axios.get<InboxMessage[]>('/mock/inbox', { params: { to } })
  return response.data
}
