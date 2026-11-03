package com.portwallet.shared.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameMaskerTest {

    @Test
    void singleWordNameIsShownAsIs() {
        assertThat(NameMasker.mask("Ana")).isEqualTo("Ana");
    }

    @Test
    void twoWordsKeepTheFirstNameAndTheInitialOfTheSurname() {
        assertThat(NameMasker.mask("Ana Souza")).isEqualTo("Ana S****");
    }

    @Test
    void threeWordsUseTheInitialOfTheLastSurname() {
        assertThat(NameMasker.mask("Ana Souza Lima")).isEqualTo("Ana L****");
    }

    @Test
    void extraSpacesAreIgnored() {
        assertThat(NameMasker.mask("  Ana   Souza  Lima ")).isEqualTo("Ana L****");
    }

    @Test
    void accentedInitialIsKept() {
        assertThat(NameMasker.mask("João Ávila")).isEqualTo("João Á****");
    }
}
