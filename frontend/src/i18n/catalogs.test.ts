import { describe, expect, it } from 'vitest'
import { en } from './locales/en'
import { es } from './locales/es'
import { fr } from './locales/fr'
import { ptBR } from './locales/pt-BR'

type Tree = { [key: string]: string | Tree }

/** Every text of a catalog, by its dotted key. */
function flatten(tree: Tree, prefix = ''): Map<string, string> {
  const texts = new Map<string, string>()
  for (const [key, value] of Object.entries(tree)) {
    const path = prefix ? `${prefix}.${key}` : key
    if (typeof value === 'string') {
      texts.set(path, value)
    } else {
      flatten(value, path).forEach((text, nested) => texts.set(nested, text))
    }
  }
  return texts
}

/** Interpolations (`{{name}}`) and Trans elements (`<anchor>`, `</anchor>`) a text relies on. */
const markers = (text: string) => (text.match(/\{\{\w+\}\}|<\/?\w+>/g) ?? []).sort()

const source = flatten(ptBR)

describe.each([
  ['en', en],
  ['es', es],
  ['fr', fr],
])('catalog %s', (_, catalog) => {
  const texts = flatten(catalog)

  it('has exactly the keys of pt-BR', () => {
    expect([...texts.keys()].sort()).toEqual([...source.keys()].sort())
  })

  it('has no empty text and keeps the interpolations and elements of pt-BR', () => {
    for (const [key, text] of texts) {
      expect(text.trim(), key).not.toBe('')
      expect(markers(text), key).toEqual(markers(source.get(key) ?? ''))
    }
  })
})

describe('catalog fr', () => {
  it('puts a no-break space before high punctuation', () => {
    for (const [key, text] of flatten(fr)) {
      expect(text, key).not.toMatch(/ [:?!;]/)
    }
  })
})
