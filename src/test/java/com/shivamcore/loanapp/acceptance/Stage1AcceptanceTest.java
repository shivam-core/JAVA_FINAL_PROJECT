package com.shivamcore.loanapp.acceptance;

import com.shivamcore.loanapp.config.DatabaseManager;
import com.shivamcore.loanapp.dao.JdbcUserDao;
import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.exception.AuthenticationException;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.service.AuthService;
import com.shivamcore.loanapp.service.UserService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1AcceptanceTest {

    private static DatabaseManager dbManager;
    private static UserDao userDao;
    private static UserService userService;
    private static AuthService authService;

    private static final String TEST_USERNAME = "stage1_test_user";
    private static final String TEST_EMAIL = "stage1_test@example.com";
    private static final String CORRECT_PASSWORD = "ValidPassword@123";

    @BeforeAll
    static void init() {
        // Connect to the MySQL instance (configurable via env/properties, fallback to local 3307)
        com.shivamcore.loanapp.config.ConfigLoader config = new com.shivamcore.loanapp.config.ConfigLoader();
        String url = config.getOrDefault("db.url", "jdbc:mysql://127.0.0.1:3307/loan_app?useSSL=false&serverTimezone=UTC");
        String user = config.getOrDefault("db.user", "root");
        String pass = config.getOrDefault("db.password", "");
        dbManager = new DatabaseManager(url, user, pass);
        userDao = new JdbcUserDao(dbManager);
        userService = new UserService(userDao);
        authService = new AuthService(userDao);

        // Clean up any leftovers from previous runs
        cleanUpUser();
    }

    @AfterAll
    static void tearDown() {
        cleanUpUser();
    }

    private static void cleanUpUser() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM users WHERE username = '" + TEST_USERNAME + "' OR email = '" + TEST_EMAIL + "'");
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    @DisplayName("Acceptance 1: User registers successfully and persists into real MySQL")
    void testUserRegistration() {
        User user = userService.register(
                "Amruta",
                "Patil",
                "9876543210",
                TEST_EMAIL,
                "Pune",
                TEST_USERNAME,
                CORRECT_PASSWORD
        );

        assertNotNull(user.getId(), "User should have an auto-generated ID from MySQL");
        assertEquals(Status.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedAttempts());

        Optional<User> fromDb = userDao.findById(user.getId());
        assertTrue(fromDb.isPresent(), "User must exist in MySQL database");
        assertEquals(TEST_USERNAME, fromDb.get().getUsername());
        assertEquals(Status.ACTIVE, fromDb.get().getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Acceptance 2: Login succeeds and resetting failed attempts")
    void testSuccessfulLogin() {
        User loggedIn = authService.login(TEST_USERNAME, CORRECT_PASSWORD);
        assertNotNull(loggedIn);
        assertEquals(TEST_USERNAME, loggedIn.getUsername());
        assertEquals(0, loggedIn.getFailedAttempts());
    }

    @Test
    @Order(3)
    @DisplayName("Acceptance 3: Success on attempt 2 resets failed attempts counter to 0 in MySQL")
    void testSuccessOnAttempt2ResetsCounter() {
        User user = userDao.findByUsername(TEST_USERNAME).orElseThrow();

        // 1 wrong attempt
        assertThrows(AuthenticationException.class, () ->
                authService.login(TEST_USERNAME, "WrongPassword@1"));

        User after1 = userDao.findById(user.getId()).orElseThrow();
        assertEquals(1, after1.getFailedAttempts(), "Database failed_attempts must be 1");
        assertEquals(Status.ACTIVE, after1.getStatus());

        // Correct password on attempt 2
        User loggedIn = authService.login(TEST_USERNAME, CORRECT_PASSWORD);
        assertNotNull(loggedIn);

        User afterSuccess = userDao.findById(user.getId()).orElseThrow();
        assertEquals(0, afterSuccess.getFailedAttempts(), "Database failed_attempts must reset to 0 after success");
    }

    @Test
    @Order(4)
    @DisplayName("Acceptance 4: 3 wrong passwords lock account to INACTIVE in MySQL")
    void testThreeWrongPasswordsLockAccount() {
        User user = userDao.findByUsername(TEST_USERNAME).orElseThrow();

        // Wrong attempt 1
        assertThrows(AuthenticationException.class, () -> authService.login(TEST_USERNAME, "Wrong1@123"));
        assertEquals(1, userDao.findById(user.getId()).orElseThrow().getFailedAttempts());

        // Wrong attempt 2
        assertThrows(AuthenticationException.class, () -> authService.login(TEST_USERNAME, "Wrong2@123"));
        assertEquals(2, userDao.findById(user.getId()).orElseThrow().getFailedAttempts());

        // Wrong attempt 3 -> should lock
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login(TEST_USERNAME, "Wrong3@123"));
        assertTrue(ex.getMessage().contains("Account locked"));

        User afterLock = userDao.findById(user.getId()).orElseThrow();
        assertEquals(3, afterLock.getFailedAttempts(), "Failed attempts should be 3 in MySQL");
        assertEquals(Status.INACTIVE, afterLock.getStatus(), "Status in MySQL must be updated to INACTIVE");
    }

    @Test
    @Order(5)
    @DisplayName("Acceptance 5: 4th attempt with the RIGHT password is refused when account is locked")
    void testFourthAttemptWithRightPasswordRefused() {
        User user = userDao.findByUsername(TEST_USERNAME).orElseThrow();
        assertEquals(Status.INACTIVE, user.getStatus(), "Pre-condition: User must be INACTIVE");

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login(TEST_USERNAME, CORRECT_PASSWORD));

        assertTrue(ex.getMessage().contains("locked or inactive"),
                "Message must indicate account is locked or inactive");
    }

    @Test
    @Order(6)
    @DisplayName("Acceptance 6: Password reset is blocked for locked/INACTIVE account")
    void testPasswordResetBlockedWhenLocked() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.resetPassword(TEST_USERNAME, "NewValidPassword@999"));

        assertTrue(ex.getMessage().contains("locked or inactive"),
                "Password reset must be blocked for locked accounts");
    }
}
