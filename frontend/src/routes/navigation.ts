import type { ParseKeys } from 'i18next'

/** `label` is a translation key, translated when the menu renders. */
export type NavItem = { to: string; label: ParseKeys }

/** Main navigation of the authenticated area. Each feature adds its page here as it ships. */
export const navItems: NavItem[] = []
