package com.portwallet.shared.crypto;

import java.util.Arrays;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keys for field encryption (AES-256-GCM) and the blind index (HMAC-SHA256), each 32 bytes in Base64. The application
 * refuses to start without them, and the two keys must differ so that one never helps to attack the other.
 */
@ConfigurationProperties("portwallet.crypto")
public record CryptoProperties(String encryptionKey, String blindIndexKey) {

    static final int KEY_BYTES = 32;

    public CryptoProperties {
        byte[] encryption = decode("PORTWALLET_CRYPTO_ENCRYPTION_KEY", encryptionKey);
        byte[] blindIndex = decode("PORTWALLET_CRYPTO_BLIND_INDEX_KEY", blindIndexKey);
        if (Arrays.equals(encryption, blindIndex)) {
            throw new IllegalStateException("Encryption and blind index keys must be different");
        }
    }

    byte[] encryptionKeyBytes() {
        return Base64.getDecoder().decode(encryptionKey);
    }

    byte[] blindIndexKeyBytes() {
        return Base64.getDecoder().decode(blindIndexKey);
    }

    private static byte[] decode(String variable, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variable + " must be set (32 random bytes in Base64)");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(variable + " must be valid Base64", e);
        }
        if (bytes.length != KEY_BYTES) {
            throw new IllegalStateException(variable + " must decode to exactly " + KEY_BYTES + " bytes");
        }
        return bytes;
    }

    @Override
    public String toString() {
        return "CryptoProperties[encryptionKey=***, blindIndexKey=***]";
    }
}
