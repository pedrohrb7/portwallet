package com.portwallet.account.application;

import com.portwallet.account.domain.Account;
import com.portwallet.account.infra.AccountRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Opens the wallet account of a user. Called when the user confirms the e-mail. */
@Service
public class AccountProvisioning {

    private final AccountRepository accounts;
    private final Clock clock;

    AccountProvisioning(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    /** Must join the caller's transaction, so the user is never active without an account. */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID openUserAccount(UUID userId) {
        return accounts.save(Account.openFor(userId, clock.instant())).id();
    }
}
