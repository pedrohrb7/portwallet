package com.portwallet.account.domain;

import com.portwallet.shared.money.Money;
import com.portwallet.shared.money.MoneyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    public static final Money DEFAULT_DAILY_LIMIT = Money.of("5000.00");

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private AccountKind kind;

    @Convert(converter = MoneyConverter.class)
    private Money balance;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "daily_limit")
    private Money dailyLimit;

    @Version
    private long version;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Account() {
    }

    private Account(UUID userId, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.kind = AccountKind.USER;
        this.balance = Money.ZERO;
        this.dailyLimit = DEFAULT_DAILY_LIMIT;
        this.createdAt = now;
    }

    public static Account openFor(UUID userId, Instant now) {
        return new Account(userId, now);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public AccountKind kind() {
        return kind;
    }

    public Money balance() {
        return balance;
    }

    public Money dailyLimit() {
        return dailyLimit;
    }

    public long version() {
        return version;
    }
}
