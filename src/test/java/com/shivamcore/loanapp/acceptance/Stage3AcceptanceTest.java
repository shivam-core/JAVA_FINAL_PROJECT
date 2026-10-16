package com.shivamcore.loanapp.acceptance;

import com.shivamcore.loanapp.config.ConfigLoader;
import com.shivamcore.loanapp.config.DatabaseManager;
import com.shivamcore.loanapp.dao.*;
import com.shivamcore.loanapp.exception.AccessDeniedException;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.*;
import com.shivamcore.loanapp.service.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage3AcceptanceTest {

    private static DatabaseManager dbManager;
    private static UserDao userDao;
    private static AuditLogDao auditLogDao;
    private static LoanDao loanDao;
    private static EmailService emailService;
    private static UserService userService;
    private static AuthService authService;
    private static AdminService adminService;
    private static LoanService loanService;

    private static final String USERNAME = "stage3_applicant";
    private static final String USER_EMAIL = "stage3_applicant@example.com";
    private static final String ADMIN_USERNAME = "stage3_admin";
    private static final String ADMIN_PASSWORD = "Stage3AdminPass@123";

    private static User applicantUser;
    private static User adminUser;
    private static LoanApplication createdLoan1;
    private static LoanApplication createdLoan2;

    @BeforeAll
    static void init() {
        ConfigLoader config = new ConfigLoader();
        String url = config.getOrDefault("db.url", "jdbc:mysql://127.0.0.1:3307/loan_app?useSSL=false&serverTimezone=UTC");
        String user = config.getOrDefault("db.user", "root");
        String pass = config.getOrDefault("db.password", "");
        dbManager = new DatabaseManager(url, user, pass);

        userDao = new JdbcUserDao(dbManager);
        auditLogDao = new JdbcAuditLogDao(dbManager);
        loanDao = new JdbcLoanDao(dbManager);
        emailService = new EmailService(config);

        userService = new UserService(userDao);
        authService = new AuthService(userDao);
        adminService = new AdminService(userDao, auditLogDao, emailService, loanDao);
        loanService = new LoanService(loanDao);

        cleanUp();

        // 1. Create applicant user
        applicantUser = userService.register(
                "Aman",
                "Gupta",
                "9811111111",
                USER_EMAIL,
                "Bangalore",
                USERNAME,
                "AmanPass@123"
        );

        // 2. Create admin user
        adminUser = new User();
        adminUser.setFname("Stage3");
        adminUser.setLname("Admin");
        adminUser.setMobile("9822222222");
        adminUser.setEmail("stage3_admin@example.com");
        adminUser.setLocation("Mumbai");
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
            stmt.executeUpdate("DELETE FROM loan_applications WHERE user_id IN (SELECT id FROM users WHERE username IN ('" + USERNAME + "', '" + ADMIN_USERNAME + "'))");
            stmt.executeUpdate("DELETE FROM audit_log WHERE performed_by = '" + ADMIN_USERNAME + "'");
            stmt.executeUpdate("DELETE FROM users WHERE username IN ('" + USERNAME + "', '" + ADMIN_USERNAME + "')");
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    @DisplayName("Acceptance 1: User applies for loan; saved to MySQL with PENDING status and midpoint interest rate")
    void testApplyForLoan() {
        BigDecimal amount = new BigDecimal("400000.00");
        BigDecimal income = new BigDecimal("850000.00");
        int durationMonths = 36;
        String purpose = "Home Extension";

        createdLoan1 = loanService.applyForLoan(
                applicantUser,
                amount,
                LoanType.HOME,
                durationMonths,
                income,
                purpose,
                null // Auto-fill midpoint interest rate: 7.50%
        );

        assertNotNull(createdLoan1.getId());
        assertEquals(LoanStatus.PENDING, createdLoan1.getStatus());
        assertEquals(new BigDecimal("7.50"), createdLoan1.getInterestRate());
        assertEquals(applicantUser.getId(), createdLoan1.getUserId());
        assertNotNull(createdLoan1.getApplicationDate());

        // Verify direct from MySQL
        Optional<LoanApplication> fromDb = loanDao.findById(createdLoan1.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(LoanStatus.PENDING, fromDb.get().getStatus());
        assertEquals(purpose, fromDb.get().getPurpose());
    }

    @Test
    @Order(2)
    @DisplayName("Acceptance 2: User can list own loan applications")
    void testUserListLoans() {
        List<LoanApplication> userLoans = loanService.getLoansByUser(applicantUser);
        assertFalse(userLoans.isEmpty());
        assertTrue(userLoans.stream().anyMatch(l -> l.getId().equals(createdLoan1.getId())));
    }

    @Test
    @Order(3)
    @DisplayName("Acceptance 3: Admin lists all loans and filters by status")
    void testAdminListAndFilterLoans() {
        List<LoanApplication> allLoans = adminService.getAllLoans(adminUser);
        assertTrue(allLoans.stream().anyMatch(l -> l.getId().equals(createdLoan1.getId())));

        List<LoanApplication> pendingLoans = adminService.getLoansByStatus(adminUser, LoanStatus.PENDING);
        assertTrue(pendingLoans.stream().anyMatch(l -> l.getId().equals(createdLoan1.getId())));

        List<LoanApplication> approvedLoans = adminService.getLoansByStatus(adminUser, LoanStatus.APPROVED);
        assertFalse(approvedLoans.stream().anyMatch(l -> l.getId().equals(createdLoan1.getId())));
    }

    @Test
    @Order(4)
    @DisplayName("Acceptance 4: Admin views loan details including applicant information")
    void testAdminViewLoanDetails() {
        Optional<LoanApplication> detailsOpt = adminService.getLoanById(adminUser, createdLoan1.getId());
        assertTrue(detailsOpt.isPresent());
        LoanApplication details = detailsOpt.get();
        assertEquals("Aman Gupta", details.getApplicantName());
        assertEquals(USER_EMAIL, details.getApplicantEmail());
        assertEquals("9811111111", details.getApplicantMobile());
    }

    @Test
    @Order(5)
    @DisplayName("Acceptance 5: Admin approves loan; status becomes APPROVED in MySQL and decided loan cannot transition again")
    void testAdminApproveLoan() {
        LoanApplication approved = adminService.approveLoan(adminUser, createdLoan1.getId());
        assertEquals(LoanStatus.APPROVED, approved.getStatus());

        // Verify status in MySQL
        LoanApplication fromDb = loanDao.findById(createdLoan1.getId()).orElseThrow();
        assertEquals(LoanStatus.APPROVED, fromDb.getStatus());

        // Attempting to re-transition approved loan is refused
        assertThrows(ValidationException.class, () ->
                adminService.approveLoan(adminUser, createdLoan1.getId()));
        assertThrows(ValidationException.class, () ->
                adminService.rejectLoan(adminUser, createdLoan1.getId(), "Cannot reject approved loan"));
    }

    @Test
    @Order(6)
    @DisplayName("Acceptance 6: User applies for second loan; Admin rejects with mandatory reason; status is REJECTED in MySQL")
    void testAdminRejectLoanWithReason() {
        createdLoan2 = loanService.applyForLoan(
                applicantUser,
                new BigDecimal("150000.00"),
                LoanType.VEHICLE,
                24,
                new BigDecimal("500000.00"),
                "Used car purchase",
                null // Auto-fill midpoint: 8.00%
        );

        // Reject without reason fails
        assertThrows(ValidationException.class, () ->
                adminService.rejectLoan(adminUser, createdLoan2.getId(), ""));
        assertThrows(ValidationException.class, () ->
                adminService.rejectLoan(adminUser, createdLoan2.getId(), "   "));

        // Reject with reason succeeds
        String reason = "Low credit history";
        LoanApplication rejected = adminService.rejectLoan(adminUser, createdLoan2.getId(), reason);
        assertEquals(LoanStatus.REJECTED, rejected.getStatus());
        assertEquals(reason, rejected.getRejectionReason());

        // Verify in MySQL
        LoanApplication fromDb = loanDao.findById(createdLoan2.getId()).orElseThrow();
        assertEquals(LoanStatus.REJECTED, fromDb.getStatus());
        assertEquals(reason, fromDb.getRejectionReason());

        // Attempting to approve rejected loan is refused
        assertThrows(ValidationException.class, () ->
                adminService.approveLoan(adminUser, createdLoan2.getId()));
    }

    @Test
    @Order(7)
    @DisplayName("Acceptance 7: Non-admin user cannot access admin loan operations")
    void testRoleGuardRejectsNonAdmin() {
        assertThrows(AccessDeniedException.class, () ->
                adminService.getAllLoans(applicantUser));
        assertThrows(AccessDeniedException.class, () ->
                adminService.getLoansByStatus(applicantUser, LoanStatus.PENDING));
        assertThrows(AccessDeniedException.class, () ->
                adminService.approveLoan(applicantUser, createdLoan1.getId()));
        assertThrows(AccessDeniedException.class, () ->
                adminService.rejectLoan(applicantUser, createdLoan1.getId(), "Reason"));
    }

    @Test
    @Order(8)
    @DisplayName("Acceptance 8: Deleting user cascades to loan applications (Decision 15)")
    void testCascadeDeleteOnUserDeletion() {
        adminService.deleteUser(adminUser, applicantUser.getId());

        // Applicant user is deleted
        Optional<User> uOpt = userDao.findById(applicantUser.getId());
        assertTrue(uOpt.isEmpty());

        // Both loans are cascade deleted from MySQL
        Optional<LoanApplication> l1 = loanDao.findById(createdLoan1.getId());
        Optional<LoanApplication> l2 = loanDao.findById(createdLoan2.getId());
        assertTrue(l1.isEmpty(), "Loan 1 must be cascade deleted when user is deleted");
        assertTrue(l2.isEmpty(), "Loan 2 must be cascade deleted when user is deleted");
    }
}
