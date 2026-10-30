package com.portwallet.auth.domain;

import java.util.Optional;

/**
 * Brazilian taxpayer number (CPF), always held as 11 digits. Accepts input with or without the usual mask
 * ({@code 529.982.247-25}) and checks the two verification digits.
 */
public record Cpf(String digits) {

    public Cpf {
        if (!isValidDigits(digits)) {
            throw new IllegalArgumentException("Invalid CPF");
        }
    }

    /** Parses masked or unmasked input; empty if it is not a valid CPF. */
    public static Optional<Cpf> parse(String input) {
        if (input == null || !input.matches("\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) {
            return Optional.empty();
        }
        String digits = input.replaceAll("\\D", "");
        return isValidDigits(digits) ? Optional.of(new Cpf(digits)) : Optional.empty();
    }

    public static boolean isValid(String input) {
        return parse(input).isPresent();
    }

    private static boolean isValidDigits(String digits) {
        if (digits == null || !digits.matches("\\d{11}") || digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 9) == digits.charAt(9) - '0' && checkDigit(digits, 10) == digits.charAt(10) - '0';
    }

    private static int checkDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    /** Never prints the full number. */
    @Override
    public String toString() {
        return "***.***.***-" + digits.substring(9);
    }
}
