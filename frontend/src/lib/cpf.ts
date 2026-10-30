// Same rules as the backend (com.portwallet.auth.domain.Cpf), checked against the same cases in cpf.test.ts.

const FORMAT = /^(\d{11}|\d{3}\.\d{3}\.\d{3}-\d{2})$/

function checkDigit(digits: string, length: number): number {
  let sum = 0
  for (let i = 0; i < length; i++) {
    sum += Number(digits[i]) * (length + 1 - i)
  }
  const remainder = (sum * 10) % 11
  return remainder === 10 ? 0 : remainder
}

/** True for 11 digits, masked (`529.982.247-25`) or not, with valid verification digits. */
export function isValidCpf(input: string): boolean {
  if (!FORMAT.test(input)) {
    return false
  }
  const digits = input.replace(/\D/g, '')
  if (new Set(digits).size === 1) {
    return false
  }
  return (
    checkDigit(digits, 9) === Number(digits[9]) && checkDigit(digits, 10) === Number(digits[10])
  )
}

/** Applies the CPF mask while the user types, ignoring anything that is not a digit. */
export function formatCpf(input: string): string {
  const digits = input.replace(/\D/g, '').slice(0, 11)
  return digits
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2')
}
