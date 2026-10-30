import { describe, expect, it } from 'vitest'
import { formatBRL } from './money'

// Intl uses a non-breaking space between the symbol and the number.
const normalize = (value: string) => value.replace(/\s/g, ' ')

describe('formatBRL', () => {
  it('formats with the BRL symbol, thousands separator and decimal comma', () => {
    expect(normalize(formatBRL('1250.50'))).toBe('R$ 1.250,50')
  })

  it('formats zero', () => {
    expect(normalize(formatBRL('0.00'))).toBe('R$ 0,00')
  })

  it('formats negative values', () => {
    expect(normalize(formatBRL('-10.00'))).toBe('-R$ 10,00')
  })
})
