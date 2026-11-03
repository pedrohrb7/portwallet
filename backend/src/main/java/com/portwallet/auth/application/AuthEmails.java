package com.portwallet.auth.application;

import com.portwallet.auth.domain.User;
import com.portwallet.shared.notification.Email;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * E-mails sent by the auth module, from the {@code email.*} messages. Always in the language of the recipient, saved
 * at registration, never in the language of whoever triggered the e-mail.
 */
@Component
class AuthEmails {

    private final String appBaseUrl;
    private final MessageSource messages;

    AuthEmails(@Value("${portwallet.app-base-url}") String appBaseUrl, MessageSource messages) {
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
        this.messages = messages;
    }

    Email confirmation(User user, String rawToken) {
        // The token goes in the URL fragment: browsers never send it to servers or in the Referer header.
        String link = appBaseUrl + "/confirm#token=" + rawToken;
        // The data of this registration go along, so the owner notices if someone else replaced them.
        return email(user, "email.confirmation", link, user.firstName(), user.name(), user.cpf().masked(), link);
    }

    Email registrationAttempt(User owner) {
        String link = appBaseUrl + "/login";
        return email(owner, "email.registration-attempt", link, owner.firstName(), link);
    }

    private Email email(User recipient, String key, String link, Object... arguments) {
        Locale locale = recipient.language().locale();
        String subject = messages.getMessage(key + ".subject", null, locale);
        String body = messages.getMessage(key + ".body", arguments, locale);
        return new Email(recipient.email(), subject, body, link);
    }
}
