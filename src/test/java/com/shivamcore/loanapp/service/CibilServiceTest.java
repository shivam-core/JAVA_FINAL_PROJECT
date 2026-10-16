package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CibilServiceTest {

    @Test
    @DisplayName("generate() produces score in [600, 900]")
    void testGenerateScoreRange() {
        CibilService service = new CibilService();
        for (int i = 0; i < 50; i++) {
            int score = service.generate();
            assertTrue(score >= 600 && score <= 900, "Score " + score + " must be between 600 and 900");
        }
    }

    @Test
    @DisplayName("getOrGenerateCibil reuses existing score if user already has one (Decision 11)")
    void testReuseExistingScore() {
        User user = new User();
        user.setPanCard("ABCDE1234F");
        user.setCibilScore(777);

        UserDao mockDao = new MockUserDao(user);
        CibilService service = new CibilService(mockDao);

        int score = service.getOrGenerateCibil("ABCDE1234F");
        assertEquals(777, score);
    }

    private static class MockUserDao implements UserDao {
        private final User user;

        MockUserDao(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findByPan(String pan) {
            if (user != null && pan.equalsIgnoreCase(user.getPanCard())) {
                return Optional.of(user);
            }
            return Optional.empty();
        }

        @Override public User save(User user) { return user; }
        @Override public Optional<User> findById(Long id) { return Optional.empty(); }
        @Override public Optional<User> findByUsername(String username) { return Optional.empty(); }
        @Override public Optional<User> findByEmail(String email) { return Optional.empty(); }
        @Override public Optional<User> findByIdentifier(String identifier) { return Optional.empty(); }
        @Override public boolean existsByUsername(String username) { return false; }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public boolean existsByPan(String pan) { return user != null && pan.equalsIgnoreCase(user.getPanCard()); }
        @Override public boolean existsByRole(com.shivamcore.loanapp.model.Role role) { return false; }
        @Override public int incrementFailedAttempts(Long userId) { return 0; }
        @Override public void lockAccount(Long userId) {}
        @Override public void resetFailedAttempts(Long userId) {}
        @Override public void updatePassword(Long userId, String newPasswordHash) {}
        @Override public void update(User user) {}
        @Override public void deleteById(Long id) {}
        @Override public java.util.List<User> search(String field, String query) { return java.util.Collections.emptyList(); }
        @Override public java.util.List<User> findAll() { return java.util.Collections.emptyList(); }
    }
}
