package com.portwallet.shared.logging;

/** Helpers to log personal data without exposing it. */
public final class Masking {

    private Masking() {
    }

    /** {@code ana.souza@demo.com} becomes {@code a***@demo.com}. */
    public static String email(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
