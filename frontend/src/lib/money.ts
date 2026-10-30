const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

/** Formats an amount received from the API (a string with two decimal places) as BRL for display. */
export function formatBRL(value: string): string {
  return brl.format(Number(value))
}
