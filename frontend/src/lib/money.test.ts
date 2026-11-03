import { describe, expect, it } from 'vitest'
import { formatBRL } from './money'

// Intl uses non-breaking spaces between the symbol and the number (and as the French thousands separator).
const normalize = (value: string) => value.replace(/\s/g, ' ')

describe('formatBRL', () => {
  it('formats with the BRL symbol, thousands separator and decimal comma', () => {
    expect(normalize(formatBRL('1250.50', 'pt-BR'))).toBe('R$ 1.250,50')
  })

  it('formats zero', () => {
    expect(normalize(formatBRL('0.00', 'pt-BR'))).toBe('R$ 0,00')
  })

  it('formats negative values', () => {
    expect(normalize(formatBRL('-10.00', 'pt-BR'))).toBe('-R$ 10,00')
  })

  it('keeps BRL in every language, with that language separators', () => {
    expect(normalize(formatBRL('1250.50', 'en'))).toBe('R$1,250.50')
    expect(normalize(formatBRL('1250.50', 'es'))).toBe('1250,50 R$')
    expect(normalize(formatBRL('1250.50', 'fr'))).toBe('1 250,50 R$')
  })
})
