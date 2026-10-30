package com.portwallet.auth.domain;

public enum UserStatus {
    /** Registered, waiting for the confirmation link. Cannot log in. */
    PENDING,
    ACTIVE,
    /** Blocked by an admin: can log in, but cannot send or receive money. */
    BLOCKED
}
