package com.portwallet.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Unique test data, so integration tests sharing a database never collide. */
public final class TestData {

    private TestData() {
    }

    public static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 12) + "@test.dev";
    }

    /** Random CPF with valid verification digits. */
    public static String validCpf() {
        while (true) {
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                digits.append(ThreadLocalRandom.current().nextInt(10));
            }
            digits.append(checkDigit(digits, 9));
            digits.append(checkDigit(digits, 10));
            String cpf = digits.toString();
            if (cpf.chars().distinct().count() > 1) {
                return cpf;
            }
        }
    }

    public static String masked(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private static int checkDigit(CharSequence digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
