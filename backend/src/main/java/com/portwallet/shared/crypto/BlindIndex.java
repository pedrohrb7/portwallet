package com.portwallet.shared.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Deterministic keyed hash (HMAC-SHA256) of an encrypted value, stored in a separate column so it can be searched and
 * kept unique without decrypting. Without the key, the index does not allow guessing the value by brute force.
 */
@Component
public class BlindIndex {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public BlindIndex(CryptoProperties properties) {
        this.key = new SecretKeySpec(properties.blindIndexKeyBytes(), ALGORITHM);
    }

    public String of(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not compute blind index", e);
        }
    }
}
