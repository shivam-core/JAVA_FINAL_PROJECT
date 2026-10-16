package com.shivamcore.loanapp.acceptance;

import com.shivamcore.loanapp.config.ConfigLoader;
import com.shivamcore.loanapp.config.DatabaseManager;
import com.shivamcore.loanapp.dao.AuditLogDao;
import com.shivamcore.loanapp.dao.JdbcAuditLogDao;
import com.shivamcore.loanapp.dao.JdbcUserDao;
import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.exception.AccessDeniedException;
import com.shivamcore.loanapp.exception.AuthenticationException;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.AuditLog;
import com.shivamcore.loanapp.model.Role;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.service.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage2AcceptanceTest {

    private static DatabaseManager dbManager;
    private static UserDao userDao;
    private static AuditLogDao auditLogDao;
    private static EmailService emailService;
    private static UserService userService;
    private static AuthService authService;
    private static AdminService adminService;
    private static AdminSeeder adminSeeder;

    private static final String MANAGED_USERNAME = "stage2_managed_user";
    private static final String MANAGED_EMAIL = "stage2_managed@example.com";
    private static final String ADMIN_USERNAME = "stage2_admin";
    private static final String ADMIN_PASSWORD = "AdminPassword@123";

    private static User adminUser;
    private static User managedUser;

    @BeforeAll
    static void init() {
        ConfigLoader config = new ConfigLoader();
        String url = config.getOrDefault("db.url", "jdbc:mysql://127.0.0.1:3307/loan_app?useSSL=false&serverTimezone=UTC");
        String user = config.getOrDefault("db.user", "root");
        String pass = config.getOrDefault("db.password", "");
        dbManager = new DatabaseManager(url, user, pass);

        userDao = new JdbcUserDao(dbManager);
        auditLogDao = new JdbcAuditLogDao(dbManager);
        emailService = new EmailService(config);

        userService = new UserService(userDao);
        authService = new AuthService(userDao);
        adminService = new AdminService(userDao, auditLogDao, emailService);
        adminSeeder = new AdminSeeder(userDao, config);

        cleanUp();

        // Seed default admin if absent
        adminSeeder.seedAdminIfAbsent();

        // Create a dedicated admin for this test run
        adminUser = new User();
        adminUser.setFname("StageTwo");
        adminUser.setLname("Admin");
        adminUser.setMobile("9888888888");
        adminUser.setEmail("stage2_admin@example.com");
        adminUser.setLocation("Pune");
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setPasswordHash(com.shivamcore.loanapp.util.PasswordHasher.hash(ADMIN_PASSWORD));
        adminUser.setStatus(Status.ACTIVE);
        adminUser.setRole(Role.ROLE_ADMIN);
        adminUser.setFailedAttempts(0);
        userDao.save(adminUser);
    }

    @AfterAll
    static void tearDown() {
        cleanUp();
    }

    private static void cleanUp() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM audit_log WHERE target_username IN ('" + MANAGED_USERNAME + "') OR performed_by = '" + ADMIN_USERNAME + "'");
            stmt.executeUpdate("DELETE FROM users WHERE username IN ('" + MANAGED_USERNAME + "', '" + ADMIN_USERNAME + "')");
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    @DisplayName("Acceptance 1: Admin logs in directly with predefined credentials and role ROLE_ADMIN")
    void testAdminDirectLogin() {
        User loggedIn = authService.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertNotNull(loggedIn);
        assertEquals(Role.ROLE_ADMIN, loggedIn.getRole());
        assertEquals(Status.ACTIVE, loggedIn.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Acceptance 2: Admin is excluded from failed login attempts counter and lockout (Decision 4)")
    void testAdminExcludedFromLockout() {
        for (int i = 0; i < 4; i++) {
            assertThrows(AuthenticationException.class, () ->
                    authService.login(ADMIN_USERNAME, "WrongAdminPassword@999"));
        }

        User fromDb = userDao.findById(adminUser.getId()).orElseThrow();
        assertEquals(0, fromDb.getFailedAttempts(), "Admin failed attempts must remain 0 in MySQL");
        assertEquals(Status.ACTIVE, fromDb.getStatus(), "Admin status must remain ACTIVE");

        // Can still log in normally
        User loggedIn = authService.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertNotNull(loggedIn);
    }

    @Test
    @Order(3)
    @DisplayName("Acceptance 3: Register a regular user and search via AdminService")
    void testAdminSearchUsers() {
        managedUser = userService.register(
                "Rohit",
                "Sharma",
                "9876543299",
                MANAGED_EMAIL,
                "Mumbai",
                MANAGED_USERNAME,
                "RohitPass@123"
        );
        assertNotNull(managedUser.getId());
        assertEquals(Role.ROLE_USER, managedUser.getRole());

        // Search by first name
        List<User> byName = adminService.searchUsers(adminUser, "fname", "Rohit");
        assertTrue(byName.stream().anyMatch(u -> u.getUsername().equals(MANAGED_USERNAME)));

        // Search by mobile
        List<User> byMobile = adminService.searchUsers(adminUser, "mobile", "9876543299");
        assertEquals(1, byMobile.size());
        assertEquals(MANAGED_USERNAME, byMobile.get(0).getUsername());

        // Search by status
        List<User> activeUsers = adminService.searchUsers(adminUser, "status", "ACTIVE");
        assertTrue(activeUsers.stream().anyMatch(u -> u.getUsername().equals(MANAGED_USERNAME)));
    }

    @Test
    @Order(4)
    @DisplayName("Acceptance 4: Role-based guard strictly denies regular users from admin operations")
    void testRoleGuardDeniesRegularUser() {
        assertThrows(AccessDeniedException.class, () ->
                adminService.getAllUsers(managedUser));
        assertThrows(AccessDeniedException.class, () ->
                adminService.activateUser(managedUser, managedUser.getId()));
        assertThrows(AccessDeniedException.class, () ->
                adminService.deleteUser(managedUser, managedUser.getId()));
    }

    @Test
    @Order(5)
    @DisplayName("Acceptance 5: Admin edits user information and changes persist in MySQL")
    void testAdminEditUser() {
        User updated = adminService.editUser(
                adminUser,
                managedUser.getId(),
                "Rohit",
                "Verma",
                "9876543299",
                "rohit_updated@example.com",
                "Delhi"
        );

        assertEquals("Verma", updated.getLname());
        assertEquals("rohit_updated@example.com", updated.getEmail());

        User fromDb = userDao.findById(managedUser.getId()).orElseThrow();
        assertEquals("Verma", fromDb.getLname());
        assertEquals("rohit_updated@example.com", fromDb.getEmail());
        assertEquals("Delhi", fromDb.getLocation());
    }

    @Test
    @Order(6)
    @DisplayName("Acceptance 6: Admin deactivates user; login is refused")
    void testAdminDeactivateUser() {
        adminService.deactivateUser(adminUser, managedUser.getId());

        User fromDb = userDao.findById(managedUser.getId()).orElseThrow();
        assertEquals(Status.INACTIVE, fromDb.getStatus());

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login(MANAGED_USERNAME, "RohitPass@123"));
        assertTrue(ex.getMessage().contains("locked or inactive"));
    }

    @Test
    @Order(7)
    @DisplayName("Acceptance 7: Admin activates user; status is ACTIVE and failed_attempts is 0 (Decision 3)")
    void testAdminActivateUser() {
        adminService.activateUser(adminUser, managedUser.getId());

        User fromDb = userDao.findById(managedUser.getId()).orElseThrow();
        assertEquals(Status.ACTIVE, fromDb.getStatus());
        assertEquals(0, fromDb.getFailedAttempts(), "Failed attempts counter must be reset to 0 in MySQL");

        // Login succeeds again
        User loggedIn = authService.login(MANAGED_USERNAME, "RohitPass@123");
        assertNotNull(loggedIn);
    }

    @Test
    @Order(8)
    @DisplayName("Acceptance 8: Admin cannot delete or deactivate own account")
    void testSelfProtectionGuards() {
        assertThrows(ValidationException.class, () ->
                adminService.deactivateUser(adminUser, adminUser.getId()));
        assertThrows(ValidationException.class, () ->
                adminService.deleteUser(adminUser, adminUser.getId()));
    }

    @Test
    @Order(9)
    @DisplayName("Acceptance 9: Admin deletes user; audit_log record is inserted in MySQL and user is removed")
    void testAdminDeleteUserWithAuditLogging() {
        adminService.deleteUser(adminUser, managedUser.getId());

        // User row must be deleted
        Optional<User> fromDb = userDao.findById(managedUser.getId());
        assertTrue(fromDb.isEmpty(), "User must be deleted from MySQL database");

        // Audit log must exist in audit_log table
        List<AuditLog> recentLogs = auditLogDao.findRecent(20);
        assertTrue(recentLogs.stream().anyMatch(log ->
                "DELETE_USER".equals(log.getAction()) &&
                ADMIN_USERNAME.equals(log.getPerformedBy()) &&
                MANAGED_USERNAME.equals(log.getTargetUsername())
        ), "Audit log must be recorded in MySQL audit_log table");
    }
}
