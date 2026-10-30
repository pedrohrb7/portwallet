package com.portwallet.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.portwallet.support.TestData;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

    @Test
    void acceptsMaskedAndUnmaskedInput() {
        assertThat(Cpf.parse("529.982.247-25")).hasValue(new Cpf("52998224725"));
        assertThat(Cpf.parse("52998224725")).hasValue(new Cpf("52998224725"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "52998224724",     // wrong second check digit
            "52998224715",     // wrong first check digit
            "11111111111",     // repeated digits pass the arithmetic but are not valid
            "5299822472",      // 10 digits
            "529982247255",    // 12 digits
            "529.982.24725",   // partial mask
            "529-982-247-25",  // wrong mask
            "abc.def.ghi-jk"
    })
    void rejectsInvalidInput(String input) {
        assertThat(Cpf.parse(input)).isEmpty();
    }

    @RepeatedTest(20)
    void acceptsGeneratedTestCpfs() {
        String cpf = TestData.validCpf();
        assertThat(Cpf.isValid(cpf)).isTrue();
        assertThat(Cpf.isValid(TestData.masked(cpf))).isTrue();
    }

    @Test
    void neverPrintsTheFullNumber() {
        assertThat(new Cpf("52998224725").toString()).isEqualTo("***.***.***-25");
    }
}
