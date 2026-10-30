package com.portwallet.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizesToTwoDecimalPlaces() {
        assertThat(Money.of("10").toString()).isEqualTo("10.00");
        assertThat(Money.of("10.5").toString()).isEqualTo("10.50");
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> Money.of("10.005")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsTrailingZerosBeyondTwoDecimalPlaces() {
        assertThat(Money.of("10.500")).isEqualTo(Money.of("10.50"));
    }

    @Test
    void equalityIgnoresInputScale() {
        assertThat(Money.of(new BigDecimal("1"))).isEqualTo(Money.of("1.00"));
    }

    @Test
    void arithmeticKeepsScale() {
        Money result = Money.of("100.10").minus(Money.of("0.10")).plus(Money.of("5"));
        assertThat(result.toString()).isEqualTo("105.00");
    }

    @Test
    void comparesValues() {
        assertThat(Money.of("2.00").isGreaterThan(Money.of("1.99"))).isTrue();
        assertThat(Money.of("1.99").max(Money.ZERO)).isEqualTo(Money.of("1.99"));
        assertThat(Money.of("-1").isNegative()).isTrue();
        assertThat(Money.ZERO.isPositive()).isFalse();
    }
}
