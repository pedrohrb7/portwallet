import { useSession } from '../features/auth/sessionStore'
import { AppLayout } from './AppLayout'
import { HomePage } from './HomePage'
import { PublicLayout } from './PublicLayout'
import { SessionLoading } from './SessionLoading'
import { WelcomePage } from './WelcomePage'

/**
 * `/` follows the session: the public home page for visitors, the authenticated area for signed-in users. It waits for
 * the restore on first load, so a signed-in user never sees the public page flash by.
 */
export function HomeRoute() {
  const session = useSession()

  if (session.status === 'loading') {
    return <SessionLoading />
  }

  if (session.status === 'authenticated') {
    return (
      <AppLayout>
        <WelcomePage />
      </AppLayout>
    )
  }

  return (
    <PublicLayout>
      <HomePage />
    </PublicLayout>
  )
}
