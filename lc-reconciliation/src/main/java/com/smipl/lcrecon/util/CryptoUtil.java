package com.smipl.lcrecon.util;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * Application-layer credential protection (VAPT WEB_VUL_07).
 * Uses only the JDK (java.security / javax.crypto) - no third-party libraries.
 *
 * An RSA-2048 keypair is generated once at startup. The browser fetches the public key,
 * encrypts the password with RSA-OAEP (SHA-256) via the built-in Web Crypto API, and the
 * server decrypts it here. The plaintext password therefore never leaves the browser.
 *
 * OAEP parameters are pinned to SHA-256 for BOTH the digest and MGF1 to match Web Crypto
 * (Java's "OAEPWithSHA-256AndMGF1Padding" otherwise defaults MGF1 to SHA-1).
 */
public final class CryptoUtil {

    private static final KeyPair KEYPAIR;

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KEYPAIR = kpg.generateKeyPair();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private CryptoUtil() {}

    /** Public key as base64-encoded X.509 SubjectPublicKeyInfo (SPKI) - importable by Web Crypto 'spki'. */
    public static String getPublicKeySpkiBase64() {
        return Base64.getEncoder().encodeToString(KEYPAIR.getPublic().getEncoded());
    }

    /** Decrypt a base64 RSA-OAEP(SHA-256) ciphertext produced by the browser. */
    public static String decryptBase64(String base64CipherText) throws Exception {
        byte[] cipherBytes = Base64.getDecoder().decode(base64CipherText);
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaep = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.DECRYPT_MODE, KEYPAIR.getPrivate(), oaep);
        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }
}
