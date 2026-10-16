package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.exception.AuthenticationException;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LoginLockoutTest {

    private FakeUserDao fakeUserDao;
    private AuthService authService;
    private User testUser;
    private static final String PLAIN_PASSWORD = "ValidPassword@123";

    @BeforeEach
    void setUp() {
        fakeUserDao = new FakeUserDao();
        authService = new AuthService(fakeUserDao);

        testUser = new User();
        testUser.setFname("Shivam");
        testUser.setLname("Kore");
        testUser.setMobile("9876543210");
        testUser.setEmail("shivam@example.com");
        testUser.setLocation("Pune");
        testUser.setUsername("shivamkore");
        testUser.setPasswordHash(PasswordHasher.hash(PLAIN_PASSWORD));
        testUser.setStatus(Status.ACTIVE);
        testUser.setFailedAttempts(0);
        testUser.setLastModifiedDate(LocalDateTime.now());

        fakeUserDao.save(testUser);
    }

    @Test
    @DisplayName("Login with valid credentials succeeds and resets attempts")
    void loginSuccess() {
        User loggedIn = authService.login("shivamkore", PLAIN_PASSWORD);
        assertNotNull(loggedIn);
        assertEquals("shivamkore", loggedIn.getUsername());
        assertEquals(0, loggedIn.getFailedAttempts());
    }

    @Test
    @DisplayName("Login by email succeeds")
    void loginByEmailSuccess() {
        User loggedIn = authService.login("shivam@example.com", PLAIN_PASSWORD);
        assertNotNull(loggedIn);
        assertEquals("shivam@example.com", loggedIn.getEmail());
    }

    @Test
    @DisplayName("Attempt 1 with wrong password fails and reports 2 remaining attempts")
    void attempt1WrongPassword() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("shivamkore", "WrongPassword@123"));

        assertTrue(ex.getMessage().contains("2 attempt(s) remaining"));
        assertEquals(1, fakeUserDao.findById(testUser.getId()).get().getFailedAttempts());
        assertEquals(Status.ACTIVE, fakeUserDao.findById(testUser.getId()).get().getStatus());
    }

    @Test
    @DisplayName("Attempt 2 with wrong password reports 1 remaining attempt")
    void attempt2WrongPassword() {
        assertThrows(AuthenticationException.class, () -> authService.login("shivamkore", "Wrong1@123"));
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("shivamkore", "Wrong2@123"));

        assertTrue(ex.getMessage().contains("1 attempt(s) remaining"));
        assertEquals(2, fakeUserDao.findById(testUser.getId()).get().getFailedAttempts());
        assertEquals(Status.ACTIVE, fakeUserDao.findById(testUser.getId()).get().getStatus());
    }

    @Test
    @DisplayName("Attempt 3 with wrong password locks the account to INACTIVE")
    void attempt3LocksAccount() {
        assertThrows(AuthenticationException.class, () -> authService.login("shivamkore", "Wrong1@123"));
        assertThrows(AuthenticationException.class, () -> authService.login("shivamkore", "Wrong2@123"));

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("shivamkore", "Wrong3@123"));

        assertTrue(ex.getMessage().contains("Account locked"));
        assertEquals(3, fakeUserDao.findById(testUser.getId()).get().getFailedAttempts());
        assertEquals(Status.INACTIVE, fakeUserDao.findById(testUser.getId()).get().getStatus());
    }

    @Test
    @DisplayName("A 4th attempt with the correct password is refused when account is locked")
    void fourthAttemptWithRightPasswordRefused() {
        // Lock account with 3 failed attempts
        assertThrows(AuthenticationException.class, () -> authService.login("shivamkore", "Wrong1@123"));
        assertThrows(AuthenticationException.class, () -> authService.login("shivamkore", "Wrong2@123"));
        assertThrows(AuthenticationException.class, () -> authService.login("shivamkore", "Wrong3@123"));

        assertEquals(Status.INACTIVE, fakeUserDao.findById(testUser.getId()).get().getStatus());

        // Attempt 4 with correct password
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("shivamkore", PLAIN_PASSWORD));

        assertTrue(ex.getMessage().contains("locked or inactive"));
    }

    @Test
    @DisplayName("Success on attempt 2 resets failed_attempts counter to 0")
    void successOnAttempt2ResetsCounter() {
        // First attempt fails
        assertThrows(AuthenticationException.class, () -> authService.login("shivamkore", "Wrong1@123"));
        assertEquals(1, fakeUserDao.findById(testUser.getId()).get().getFailedAttempts());

        // Second attempt succeeds
        User loggedIn = authService.login("shivamkore", PLAIN_PASSWORD);
        assertNotNull(loggedIn);
        assertEquals(0, fakeUserDao.findById(testUser.getId()).get().getFailedAttempts());
    }

    @Test
    @DisplayName("Non-existent user throws User not found without modifying any counter")
    void nonExistentUserThrows() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("unknown_user", "AnyPassword@123"));

        assertEquals("User not found.", ex.getMessage());
    }

    @Test
    @DisplayName("Blank credentials throw ValidationException")
    void blankCredentialsThrow() {
        assertThrows(ValidationException.class, () -> authService.login("", "pass"));
        assertThrows(ValidationException.class, () -> authService.login("user", ""));
    }

    @Test
    @DisplayName("Reset password succeeds for active account and allows login with new password")
    void resetPasswordSucceedsForActiveAccount() {
        String newPassword = "NewSecurePassword@999";
        assertDoesNotThrow(() -> authService.resetPassword("shivamkore", newPassword));

        // Old password fails
        assertThrows(AuthenticationException.class, () -> authService.login("shivamkore", PLAIN_PASSWORD));

        // New password succeeds
        User loggedIn = authService.login("shivamkore", newPassword);
        assertNotNull(loggedIn);
    }

    @Test
    @DisplayName("Reset password blocked for locked/inactive account")
    void resetPasswordBlockedForInactiveAccount() {
        // Lock the account first
        fakeUserDao.lockAccount(testUser.getId());

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.resetPassword("shivamkore", "NewPassword@123"));

        assertTrue(ex.getMessage().contains("locked or inactive"));
    }

    @Test
    @DisplayName("Reset password fails for unknown identifier")
    void resetPasswordFailsForUnknownIdentifier() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.resetPassword("nonexistent", "NewPassword@123"));

        assertEquals("User not found.", ex.getMessage());
    }

    @Test
    @DisplayName("Reset password enforces password policy")
    void resetPasswordEnforcesPolicy() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                authService.resetPassword("shivamkore", "weak"));

        assertTrue(ex.getMessage().contains("Password must be at least 8 characters"));
    }

    @Test
    @DisplayName("Admin user is excluded from failed login attempts counter and lockout (Decision 4)")
    void adminExcludedFromLockout() {
        User admin = new User();
        admin.setFname("Admin");
        admin.setLname("User");
        admin.setMobile("9999999999");
        admin.setEmail("admin@loanapp.com");
        admin.setLocation("HQ");
        admin.setUsername("admin");
        admin.setPasswordHash(PasswordHasher.hash("Admin@1234"));
        admin.setStatus(Status.ACTIVE);
        admin.setRole(com.shivamcore.loanapp.model.Role.ROLE_ADMIN);
        admin.setFailedAttempts(0);
        fakeUserDao.save(admin);

        for (int i = 0; i < 5; i++) {
            assertThrows(AuthenticationException.class, () ->
                    authService.login("admin", "WrongPass@123"));
        }

        User fromDb = fakeUserDao.findByUsername("admin").orElseThrow();
        assertEquals(0, fromDb.getFailedAttempts(), "Admin failed attempts should never increment");
        assertEquals(Status.ACTIVE, fromDb.getStatus(), "Admin should never be locked");

        // Admin can still log in with correct password
        User loggedIn = authService.login("admin", "Admin@1234");
        assertNotNull(loggedIn);
    }

    // In-memory fake DAO implementation for isolated pure unit testing
    private static class FakeUserDao implements UserDao {
        private final Map<Long, User> usersById = new HashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public User save(User user) {
            if (user.getId() == null) {
                user.setId(idGenerator.getAndIncrement());
            }
            usersById.put(user.getId(), copy(user));
            return user;
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(usersById.get(id)).map(this::copy);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return usersById.values().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .map(this::copy);
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return usersById.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst()
                    .map(this::copy);
        }

        @Override
        public Optional<User> findByIdentifier(String identifier) {
            if (identifier.contains("@")) {
                return findByEmail(identifier);
            }
            return findByUsername(identifier);
        }

        @Override
        public boolean existsByUsername(String username) {
            return usersById.values().stream()
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
        }

        @Override
        public boolean existsByEmail(String email) {
            return usersById.values().stream()
                    .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }

        @Override
        public boolean existsByRole(com.shivamcore.loanapp.model.Role role) {
            return usersById.values().stream()
                    .anyMatch(u -> u.getRole() == role);
        }

        @Override
        public int incrementFailedAttempts(Long userId) {
            User user = usersById.get(userId);
            if (user != null) {
                int newAttempts = user.getFailedAttempts() + 1;
                user.setFailedAttempts(newAttempts);
                return newAttempts;
            }
            return 0;
        }

        @Override
        public void lockAccount(Long userId) {
            User user = usersById.get(userId);
            if (user != null) {
                user.setStatus(Status.INACTIVE);
            }
        }

        @Override
        public void resetFailedAttempts(Long userId) {
            User user = usersById.get(userId);
            if (user != null) {
                user.setFailedAttempts(0);
            }
        }

        @Override
        public void updatePassword(Long userId, String newPasswordHash) {
            User user = usersById.get(userId);
            if (user != null) {
                user.setPasswordHash(newPasswordHash);
            }
        }

        @Override
        public void update(User user) {
            usersById.put(user.getId(), copy(user));
        }

        private User copy(User u) {
            return new User(
                    u.getId(), u.getFname(), u.getLname(), u.getMobile(), u.getEmail(),
                    u.getLocation(), u.getLastModifiedDate(), u.getStatus(),
                    u.getUsername(), u.getPasswordHash(), u.getRole(), u.getFailedAttempts()
            );
        }
    }
}
