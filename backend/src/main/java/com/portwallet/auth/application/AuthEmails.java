package com.portwallet.auth.application;

import com.portwallet.auth.domain.User;
import com.portwallet.shared.notification.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Texts of the e-mails sent by the auth module. */
@Component
class AuthEmails {

    private final String appBaseUrl;

    AuthEmails(@Value("${portwallet.app-base-url}") String appBaseUrl) {
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
    }

    Email confirmation(User user, String rawToken) {
        // The token goes in the URL fragment: browsers never send it to servers or in the Referer header.
        String link = appBaseUrl + "/confirm#token=" + rawToken;
        String body = """
                Olá, %s!

                Para ativar sua conta PortWallet, acesse o link abaixo. Ele vale por 24 horas e só pode ser usado uma vez.

                %s

                Se você não criou esta conta, ignore este e-mail.""".formatted(user.firstName(), link);
        return new Email(user.email(), "Ative sua conta PortWallet", body, link);
    }

    Email registrationAttempt(User owner) {
        String link = appBaseUrl + "/login";
        String body = """
                Olá, %s!

                Alguém tentou criar uma conta PortWallet usando os seus dados. Nenhuma conta nova foi criada.

                Se foi você, entre com a sua conta existente: %s

                Se não foi você, nenhuma ação é necessária.""".formatted(owner.firstName(), link);
        return new Email(owner.email(), "Tentativa de cadastro com os seus dados", body, link);
    }
}
