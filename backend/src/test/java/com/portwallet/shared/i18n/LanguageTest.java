package com.portwallet.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LanguageTest {

    @Test
    void matchesExactLocales() {
        assertThat(Language.of(Locale.forLanguageTag("pt-BR"))).isEqualTo(Language.PT_BR);
        assertThat(Language.of(Locale.forLanguageTag("en"))).isEqualTo(Language.EN);
        assertThat(Language.of(Locale.forLanguageTag("es"))).isEqualTo(Language.ES);
        assertThat(Language.of(Locale.forLanguageTag("fr"))).isEqualTo(Language.FR);
    }

    @Test
    void matchesARegionalVariantByItsLanguage() {
        assertThat(Language.of(Locale.forLanguageTag("en-GB"))).isEqualTo(Language.EN);
        assertThat(Language.of(Locale.forLanguageTag("es-AR"))).isEqualTo(Language.ES);
        assertThat(Language.of(Locale.forLanguageTag("fr-CA"))).isEqualTo(Language.FR);
        assertThat(Language.of(Locale.forLanguageTag("pt-PT"))).isEqualTo(Language.PT_BR);
    }

    @Test
    void fallsBackToBrazilianPortuguese() {
        assertThat(Language.of(Locale.forLanguageTag("de"))).isEqualTo(Language.PT_BR);
        assertThat(Language.of(null)).isEqualTo(Language.PT_BR);
    }

    @Test
    void exposesTheTagsUsedByTheFrontend() {
        assertThat(Language.values()).extracting(Language::tag).containsExactly("pt-BR", "en", "es", "fr");
    }
}
