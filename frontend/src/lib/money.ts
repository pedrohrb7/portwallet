const formatters = new Map<string, Intl.NumberFormat>()

/**
 * Formats an amount received from the API (a string with two decimal places) for display. The currency is always
 * BRL; separators and symbol placement follow the language of the interface.
 */
export function formatBRL(value: string, language: string): string {
  let formatter = formatters.get(language)
  if (!formatter) {
    formatter = new Intl.NumberFormat(language, {
      style: 'currency',
      currency: 'BRL',
      // "R$" in every language; some (es) would otherwise print the code "BRL".
      currencyDisplay: 'narrowSymbol',
    })
    formatters.set(language, formatter)
  }
  return formatter.format(Number(value))
}
