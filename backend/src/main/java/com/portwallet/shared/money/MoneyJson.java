package com.portwallet.shared.money;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import org.springframework.boot.jackson.JsonComponent;

/** Writes {@link Money} as a string with two decimal places; reads it from a string or a number. */
@JsonComponent
public class MoneyJson {

    public static class Serializer extends JsonSerializer<Money> {
        @Override
        public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }
    }

    public static class Deserializer extends JsonDeserializer<Money> {
        @Override
        public Money deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            JsonToken token = parser.currentToken();
            try {
                if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                    return Money.of(parser.getDecimalValue());
                }
                if (token == JsonToken.VALUE_STRING) {
                    return Money.of(new BigDecimal(parser.getText().trim()));
                }
            } catch (IllegalArgumentException e) {
                return (Money) ctxt.handleWeirdStringValue(Money.class, parser.getText(), e.getMessage());
            }
            return (Money) ctxt.handleUnexpectedToken(Money.class, parser);
        }
    }
}
