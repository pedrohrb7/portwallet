import { describe, expect, it } from 'vitest'
import { formatCpf, isValidCpf } from './cpf'

describe('isValidCpf', () => {
  it.each(['529.982.247-25', '52998224725', '111.444.777-35'])('accepts %s', (input) => {
    expect(isValidCpf(input)).toBe(true)
  })

  // Same invalid cases as the backend CpfTest.
  it.each([
    '',
    '52998224724',
    '52998224715',
    '11111111111',
    '5299822472',
    '529982247255',
    '529.982.24725',
    '529-982-247-25',
    'abc.def.ghi-jk',
  ])('rejects "%s"', (input) => {
    expect(isValidCpf(input)).toBe(false)
  })
})

describe('formatCpf', () => {
  it.each([
    ['5', '5'],
    ['5299', '529.9'],
    ['5299822', '529.982.2'],
    ['5299822472', '529.982.247-2'],
    ['52998224725', '529.982.247-25'],
    ['529982247259999', '529.982.247-25'],
    ['529.982.247-25', '529.982.247-25'],
    ['abc529', '529'],
  ])('formats %s as %s', (input, expected) => {
    expect(formatCpf(input)).toBe(expected)
  })
})
