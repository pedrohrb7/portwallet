package com.portwallet.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class MoneyJsonTest {

    record Payload(Money amount) {
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void writesAsStringWithTwoDecimalPlaces() throws Exception {
        assertThat(objectMapper.writeValueAsString(new Payload(Money.of("150"))))
                .isEqualTo("{\"amount\":\"150.00\"}");
    }

    @Test
    void readsFromString() throws Exception {
        assertThat(objectMapper.readValue("{\"amount\":\"150.25\"}", Payload.class).amount())
                .isEqualTo(Money.of("150.25"));
    }

    @Test
    void readsFromNumberWithoutPrecisionLoss() throws Exception {
        assertThat(objectMapper.readValue("{\"amount\":0.1}", Payload.class).amount())
                .isEqualTo(Money.of("0.10"));
        assertThat(objectMapper.readValue("{\"amount\":9999999999999.99}", Payload.class).amount())
                .isEqualTo(Money.of("9999999999999.99"));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"amount\":\"1.001\"}", Payload.class))
                .isInstanceOf(InvalidFormatException.class);
        assertThatThrownBy(() -> objectMapper.readValue("{\"amount\":1.001}", Payload.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void rejectsNonNumericString() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"amount\":\"abc\"}", Payload.class))
                .isInstanceOf(InvalidFormatException.class);
    }
}
