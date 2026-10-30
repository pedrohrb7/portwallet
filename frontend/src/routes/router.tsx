import { createBrowserRouter, type RouteObject } from 'react-router'
import { AppLayout } from './AppLayout'
import { HomePage } from './HomePage'
import { NotFoundPage } from './NotFoundPage'
import { PublicLayout } from './PublicLayout'
import { RequireAuth } from './RequireAuth'
import { RootLayout } from './RootLayout'

export const routes: RouteObject[] = [
  {
    element: <RootLayout />,
    children: [
      {
        element: <PublicLayout />,
        children: [{ path: '/', element: <HomePage /> }],
      },
      {
        element: <RequireAuth />,
        children: [{ element: <AppLayout />, children: [] }],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
