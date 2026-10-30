package com.portwallet.shared.notification;

/**
 * Message sent to a user. {@code link} repeats the call-to-action URL found in the body, so the demo inbox and the
 * tests do not need to parse the text; it may be null.
 */
public record Email(String to, String subject, String body, String link) {
}
