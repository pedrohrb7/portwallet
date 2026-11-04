import { createBrowserRouter, type RouteObject } from 'react-router'
import { ConfirmPage } from '../features/auth/ConfirmPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { InboxPage } from '../features/inbox/InboxPage'
import { demoInboxEnabled } from '../lib/env'
import { AppLayout } from './AppLayout'
import { HomeRoute } from './HomeRoute'
import { NotFoundPage } from './NotFoundPage'
import { PublicLayout, PublicWideLayout } from './PublicLayout'
import { RequireAuth } from './RequireAuth'
import { RootLayout } from './RootLayout'

const publicRoutes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/confirm', element: <ConfirmPage /> },
]

const publicWideRoutes: RouteObject[] = demoInboxEnabled
  ? [{ path: '/inbox', element: <InboxPage /> }]
  : []

export const routes: RouteObject[] = [
  {
    element: <RootLayout />,
    children: [
      // Public for visitors, the authenticated home for signed-in users (HomeRoute picks the layout).
      { path: '/', element: <HomeRoute /> },
      { element: <PublicLayout />, children: publicRoutes },
      { element: <PublicWideLayout />, children: publicWideRoutes },
      {
        element: <RequireAuth />,
        children: [{ element: <AppLayout />, children: [] }],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
