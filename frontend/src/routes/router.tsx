import { createBrowserRouter, type RouteObject } from 'react-router'
import { ConfirmPage } from '../features/auth/ConfirmPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { InboxPage } from '../features/inbox/InboxPage'
import { demoInboxEnabled } from '../lib/env'
import { AppLayout } from './AppLayout'
import { HomePage } from './HomePage'
import { NotFoundPage } from './NotFoundPage'
import { PublicLayout, PublicWideLayout } from './PublicLayout'
import { RequireAuth } from './RequireAuth'
import { RootLayout } from './RootLayout'

const publicRoutes: RouteObject[] = [
  { path: '/', element: <HomePage /> },
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
