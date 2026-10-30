package com.portwallet.shared.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portwallet.PortWalletApplication;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

class CryptoTest {

    private static final String KEY_A = "CofGSDsX497EScRUIT4I7b9wYzLr8TpTa1OV+rerNT0=";
    private static final String KEY_B = "21ONoO2JoA/oTp0Mws+VCh4rLMokzUTNQnUA07D+c8c=";
    private static final String KEY_C = "xAKgggRwSyj3FKLfZkTXmgehp5CGm4ivSjaqAEKkUk0=";

    private final CryptoProperties properties = new CryptoProperties(KEY_A, KEY_B);
    private final FieldEncryptor encryptor = new FieldEncryptor(properties);
    private final BlindIndex blindIndex = new BlindIndex(properties);

    @Test
    void encryptionRoundTrips() {
        assertThat(encryptor.decrypt(encryptor.encrypt("52998224725"))).isEqualTo("52998224725");
    }

    @Test
    void encryptingTheSameTextTwiceGivesDifferentCiphertexts() {
        String first = encryptor.encrypt("52998224725");
        String second = encryptor.encrypt("52998224725");
        assertThat(first).isNotEqualTo(second).doesNotContain("52998224725");
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] data = Base64.getDecoder().decode(encryptor.encrypt("52998224725"));
        data[data.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(data);
        assertThatThrownBy(() -> encryptor.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anotherKeyCannotDecrypt() {
        FieldEncryptor other = new FieldEncryptor(new CryptoProperties(KEY_C, KEY_B));
        String ciphertext = encryptor.encrypt("52998224725");
        assertThatThrownBy(() -> other.decrypt(ciphertext)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blindIndexIsDeterministicAndKeyed() {
        BlindIndex other = new BlindIndex(new CryptoProperties(KEY_A, KEY_C));
        assertThat(blindIndex.of("52998224725"))
                .isEqualTo(blindIndex.of("52998224725"))
                .hasSize(64)
                .isNotEqualTo(other.of("52998224725"))
                .isNotEqualTo(blindIndex.of("11144477735"));
    }

    @Test
    void secureTokensAreRandomAndHashedWithSha256() {
        String token = SecureTokens.generate();
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+").isNotEqualTo(SecureTokens.generate());
        assertThat(SecureTokens.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void keysMustBe32BytesOfBase64() {
        assertThatThrownBy(() -> new CryptoProperties("", KEY_B)).hasMessageContaining("must be set");
        assertThatThrownBy(() -> new CryptoProperties("not base64!", KEY_B)).hasMessageContaining("Base64");
        assertThatThrownBy(() -> new CryptoProperties("c2hvcnQ=", KEY_B)).hasMessageContaining("32 bytes");
    }

    @Test
    void keysMustDiffer() {
        assertThatThrownBy(() -> new CryptoProperties(KEY_A, KEY_A)).hasMessageContaining("must be different");
    }

    @Test
    void neverPrintsTheKeys() {
        assertThat(properties.toString()).doesNotContain(KEY_A).doesNotContain(KEY_B);
    }

    @Test
    void applicationDoesNotStartWithoutKeysOutsideDevelopmentProfiles() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(PortWalletApplication.class);
        assertThatThrownBy(() -> app.run("--spring.profiles.active=production-like", "--server.port=0",
                        "--portwallet.security.jwt.secret=a-secret-with-at-least-32-bytes-ok"))
                .rootCause()
                .hasMessageContaining("PORTWALLET_CRYPTO_ENCRYPTION_KEY must be set");
    }
}
