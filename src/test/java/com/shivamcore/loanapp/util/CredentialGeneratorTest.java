package com.shivamcore.loanapp.util;

import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.model.Role;
import com.shivamcore.loanapp.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CredentialGeneratorTest {

    @Test
    @DisplayName("Username generation follows exact casing and substring rules from spec")
    void testUsernameGenerationExactSpec() {
        String username = CredentialGenerator.baseUsername("Shivani", "Chaudhari", "9876543210");
        assertEquals("ShiCha3210", username);
    }

    @Test
    @DisplayName("Username handles all lowercase and all uppercase inputs correctly")
    void testUsernameCasingNormalization() {
        assertEquals("ShiCha3210", CredentialGenerator.baseUsername("shivani", "chaudhari", "9876543210"));
        assertEquals("ShiCha3210", CredentialGenerator.baseUsername("SHIVANI", "CHAUDHARI", "9876543210"));
    }

    @Test
    @DisplayName("Username handles names shorter than 3 letters")
    void testUsernameShortNames() {
        assertEquals("AlBa3210", CredentialGenerator.baseUsername("Al", "Ba", "9876543210"));
        assertEquals("AB3210", CredentialGenerator.baseUsername("a", "b", "9876543210"));
    }

    @Test
    @DisplayName("Username generation with null DAO returns base username")
    void testUsernameWithNullDao() {
        assertEquals("ShiCha3210", CredentialGenerator.generateUsername("Shivani", "Chaudhari", "9876543210", null));
    }

    @Test
    @DisplayName("Username generation resolves collisions by appending 2-digit suffix per Decision 12")
    void testUsernameCollisionResolution() {
        StubUserDao stubDao = new StubUserDao();
        stubDao.existingUsernames.add("ShiCha3210");

        String generated = CredentialGenerator.generateUsername("Shivani", "Chaudhari", "9876543210", stubDao);
        assertNotEquals("ShiCha3210", generated);
        assertTrue(generated.startsWith("ShiCha3210"));
        assertTrue(generated.length() >= "ShiCha3210".length() + 2);
    }

    @Test
    @DisplayName("Password generation creates valid password satisfying Stage 1 policy")
    void testPasswordGenerationMeetsPolicy() {
        for (int i = 0; i < 50; i++) {
            String password = CredentialGenerator.generatePassword();
            assertNotNull(password);
            assertTrue(password.length() >= 8 && password.length() <= 10, "Password length must be 8-10 chars");
            assertTrue(Validator.isValidPassword(password), "Password must satisfy Validator.isValidPassword");
        }
    }

    private static class StubUserDao implements UserDao {
        final Set<String> existingUsernames = new HashSet<>();

        @Override
        public User save(User user) { return user; }
        @Override
        public Optional<User> findById(Long id) { return Optional.empty(); }
        @Override
        public Optional<User> findByUsername(String username) { return Optional.empty(); }
        @Override
        public Optional<User> findByEmail(String email) { return Optional.empty(); }
        @Override
        public Optional<User> findByIdentifier(String identifier) { return Optional.empty(); }
        @Override
        public Optional<User> findByPan(String pan) { return Optional.empty(); }
        @Override
        public boolean existsByUsername(String username) { return existingUsernames.contains(username); }
        @Override
        public boolean existsByEmail(String email) { return false; }
        @Override
        public boolean existsByPan(String pan) { return false; }
        @Override
        public boolean existsByRole(Role role) { return false; }
        @Override
        public int incrementFailedAttempts(Long userId) { return 0; }
        @Override
        public void lockAccount(Long userId) {}
        @Override
        public void resetFailedAttempts(Long userId) {}
        @Override
        public void updatePassword(Long userId, String newPasswordHash) {}
        @Override
        public void update(User user) {}
        @Override
        public void deleteById(Long id) {}
        @Override
        public List<User> search(String field, String query) { return List.of(); }
        @Override
        public List<User> findAll() { return List.of(); }
    }
}
