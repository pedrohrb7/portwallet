import type { ReactNode } from 'react'
import { Outlet } from 'react-router'

/**
 * Narrow, vertically centered column for short public pages (forms and messages). Renders `children` when given,
 * for a route that picks its layout itself (HomeRoute), and the child route otherwise.
 */
export function PublicLayout({ children }: { children?: ReactNode }) {
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center gap-4 px-4 py-12">
      {children ?? <Outlet />}
    </main>
  )
}

/** Wider, top-aligned column for public pages whose content grows, such as lists. */
export function PublicWideLayout() {
  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-4 px-4 py-12">
      <Outlet />
    </main>
  )
}
