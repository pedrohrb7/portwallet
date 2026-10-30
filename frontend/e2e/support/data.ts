import { randomUUID } from 'node:crypto'

/** Unique e-mail per test, so tests never depend on each other or on execution order. */
export function uniqueEmail(): string {
  return `e2e-${randomUUID().slice(0, 12)}@test.dev`
}

function checkDigit(digits: string, length: number): number {
  let sum = 0
  for (let i = 0; i < length; i++) {
    sum += Number(digits[i]) * (length + 1 - i)
  }
  const remainder = (sum * 10) % 11
  return remainder === 10 ? 0 : remainder
}

/** Random fictitious CPF with valid verification digits (11 digits, no mask). */
export function validCpf(): string {
  for (;;) {
    let digits = ''
    for (let i = 0; i < 9; i++) {
      digits += Math.floor(Math.random() * 10)
    }
    digits += checkDigit(digits, 9)
    digits += checkDigit(digits, 10)
    if (new Set(digits).size > 1) {
      return digits
    }
  }
}
