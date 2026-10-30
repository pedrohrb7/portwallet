import { Outlet } from 'react-router'

export function PublicLayout() {
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center gap-4 px-4 py-12">
      <Outlet />
    </main>
  )
}
