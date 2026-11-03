/** The demo inbox exists only in the dev, e2e and demo builds (see docs/features/fr-01-registration). */
export const demoInboxEnabled = import.meta.env.VITE_DEMO_INBOX === 'true'
