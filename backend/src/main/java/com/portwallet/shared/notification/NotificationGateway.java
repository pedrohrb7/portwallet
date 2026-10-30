package com.portwallet.shared.notification;

/**
 * Sends e-mails to users. In phase 2 the simulated service is called in process; phase 4 replaces it with an HTTP
 * client protected by a circuit breaker and an outbox.
 */
public interface NotificationGateway {

    void send(Email email);
}
