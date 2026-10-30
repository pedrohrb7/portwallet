import { Outlet } from 'react-router'

/** Narrow, vertically centered column for short public pages (forms and messages). */
export function PublicLayout() {
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center gap-4 px-4 py-12">
      <Outlet />
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
