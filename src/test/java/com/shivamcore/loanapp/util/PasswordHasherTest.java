package com.shivamcore.loanapp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    @DisplayName("Hash verifies successfully with correct password")
    void hashVerifiesWithCorrectPassword() {
        String password = "SecretPassword@123";
        String storedHash = PasswordHasher.hash(password);

        assertNotNull(storedHash);
        assertTrue(storedHash.contains(":"));
        assertTrue(PasswordHasher.verify(password, storedHash));
    }

    @Test
    @DisplayName("Verification fails with wrong password")
    void verificationFailsWithWrongPassword() {
        String password = "SecretPassword@123";
        String wrongPassword = "WrongPassword@123";
        String storedHash = PasswordHasher.hash(password);

        assertFalse(PasswordHasher.verify(wrongPassword, storedHash));
    }

    @Test
    @DisplayName("Same password hashed twice produces different hashes due to random salt")
    void samePasswordProducesDifferentHashes() {
        String password = "SecretPassword@123";
        String hash1 = PasswordHasher.hash(password);
        String hash2 = PasswordHasher.hash(password);

        assertNotEquals(hash1, hash2);
        assertTrue(PasswordHasher.verify(password, hash1));
        assertTrue(PasswordHasher.verify(password, hash2));
    }

    @Test
    @DisplayName("Null password in hash throws IllegalArgumentException")
    void nullPasswordThrows() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }

    @Test
    @DisplayName("Verify returns false on null or malformed hashes")
    void verifyReturnsFalseOnMalformedHashes() {
        assertFalse(PasswordHasher.verify(null, "someSalt:someHash"));
        assertFalse(PasswordHasher.verify("password", null));
        assertFalse(PasswordHasher.verify("password", "no_colon_here"));
        assertFalse(PasswordHasher.verify("password", "not_valid_base64:not_valid_base64"));
        assertFalse(PasswordHasher.verify("password", ":"));
    }
}
