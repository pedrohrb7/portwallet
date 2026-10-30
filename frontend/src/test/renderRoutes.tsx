import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { StrictMode } from 'react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router'

type Options = {
  /** Renders inside StrictMode, as main.tsx does, to catch effects that are not safe to run twice. */
  strict?: boolean
}

/** Renders routes in a memory router with a fresh query client, starting at `path`. */
export function renderRoutes(routes: RouteObject[], path = '/', { strict = false }: Options = {}) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const tree = (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
  const view = render(strict ? <StrictMode>{tree}</StrictMode> : tree)
  return { ...view, router, queryClient }
}
