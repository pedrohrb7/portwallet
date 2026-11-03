package com.portwallet.shared.privacy;

/**
 * Masks a person's name for screens that must not reveal it in full: the first name, then the initial of the last
 * surname followed by four asterisks ({@code Ana Souza Lima} becomes {@code Ana L****}). A single-word name is shown
 * as is.
 */
public final class NameMasker {

    private NameMasker() {
    }

    public static String mask(String name) {
        String[] words = name.strip().split("\\s+");
        if (words.length == 1) {
            return words[0];
        }
        String lastSurname = words[words.length - 1];
        return words[0] + " " + lastSurname.substring(0, lastSurname.offsetByCodePoints(0, 1)) + "****";
    }
}
