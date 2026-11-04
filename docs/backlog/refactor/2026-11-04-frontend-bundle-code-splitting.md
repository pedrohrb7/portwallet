# Split the frontend bundle by route

- **Status:** Proposed
- **Opened:** 2026-11-04
- **Scope:** `frontend/src/routes/router.tsx`, `frontend/vite.config.ts`

## Motivation

`npm run build` warns that a chunk is larger than 500 kB after minification. Every visitor downloads the code of every screen on the first load, including screens they may never open (the demo inbox, the activation page). The bundle grows with each Phase 2 feature, so the first load keeps getting slower, and the warning hides any new warning that appears in the build output.

## Current state

The production build emits a single JavaScript chunk, measured on 2026-11-04 after FR-02:

| File | Size | Gzip |
| --- | --- | --- |
| `dist/assets/index-*.js` | 634.36 kB | 200.64 kB |
| `dist/assets/index-*.css` | 16.14 kB | 4.13 kB |

`router.tsx` imports every page statically, so they all end up in the entry chunk together with React, React Router, TanStack Query, Axios, React Hook Form, Zod and i18next with its four catalogs.

## Proposed change

- Load each page with React Router's route-level `lazy`, so every screen becomes its own chunk, loaded when its route is first visited. Keep the shell (`RootLayout`, `RequireAuth`, `AppLayout`, `PublicLayout`, `HomeRoute`) in the entry chunk, so the first paint and the session restore do not wait for an extra request.
- Show the existing `SessionLoading` state (or a route-level equivalent) while a page chunk loads.
- If the vendor code still crosses the limit, split stable dependencies into a separate chunk through `build.rolldownOptions.output` in `vite.config.ts`, so it stays cached across deploys.
- Do not raise `build.chunkSizeWarningLimit` to silence the warning.

## Risks

- A deploy that removes old chunks breaks open tabs still on the previous version when they navigate to a page not loaded yet. Mitigate with a route error boundary that reloads the page once on a chunk load failure.
- The E2E and unit tests render pages through `routes`; lazy routes make the first render of a page asynchronous, so tests that query synchronously after navigation need `findBy*`.

## Out of scope

- Lazy loading the i18n catalogs per language.
- Image or CSS optimization.
