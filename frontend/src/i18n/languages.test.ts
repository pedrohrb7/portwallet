import { describe, expect, it } from 'vitest'
import { matchLanguage } from './languages'

describe('matchLanguage', () => {
  it('matches supported tags exactly, ignoring case', () => {
    expect(matchLanguage('pt-BR')).toBe('pt-BR')
    expect(matchLanguage('pt-br')).toBe('pt-BR')
    expect(matchLanguage('en')).toBe('en')
    expect(matchLanguage('es')).toBe('es')
    expect(matchLanguage('fr')).toBe('fr')
  })

  it('matches a regional variant by its language', () => {
    expect(matchLanguage('en-US')).toBe('en')
    expect(matchLanguage('en-GB')).toBe('en')
    expect(matchLanguage('es-AR')).toBe('es')
    expect(matchLanguage('fr-CA')).toBe('fr')
    expect(matchLanguage('pt-PT')).toBe('pt-BR')
    expect(matchLanguage('pt')).toBe('pt-BR')
    expect(matchLanguage('en_US')).toBe('en')
  })

  it('returns nothing for unsupported or missing tags', () => {
    expect(matchLanguage('de')).toBeUndefined()
    expect(matchLanguage('')).toBeUndefined()
    expect(matchLanguage(null)).toBeUndefined()
    expect(matchLanguage(undefined)).toBeUndefined()
  })
})
