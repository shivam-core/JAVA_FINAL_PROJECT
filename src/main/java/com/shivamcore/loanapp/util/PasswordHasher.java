package com.shivamcore.loanapp.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Secure password hashing utility using PBKDF2WithHmacSHA256.
 * Salt is generated per user and stored alongside the derived hash.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Utility class
    }

    /**
     * Hashes a plaintext password using a freshly generated random salt.
     *
     * @param password Plaintext password
     * @return Stored format: "salt_base64:hash_base64"
     */
    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(salt) + ":" + encoder.encodeToString(hash);
    }

    /**
     * Verifies a plaintext password against a stored "salt_base64:hash_base64" string.
     * Uses constant-time comparison to protect against timing attacks.
     *
     * @param password   Plaintext candidate password
     * @param storedHash Stored salt:hash string
     * @return true if password matches, false otherwise
     */
    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }

        String[] parts = storedHash.split(":", 2);
        if (parts.length != 2) {
            return false;
        }

        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[0]);
            byte[] expectedHash = decoder.decode(parts[1]);

            byte[] computedHash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            return MessageDigest.isEqual(expectedHash, computedHash);
        } catch (IllegalArgumentException e) {
            // Malformed base64
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cryptographic algorithm unavailable: " + ALGORITHM, e);
        }
    }
}
