/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** "true" shows the demo inbox (dev, e2e and demo builds). */
  readonly VITE_DEMO_INBOX?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
