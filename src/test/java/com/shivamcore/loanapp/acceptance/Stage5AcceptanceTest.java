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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage5AcceptanceTest {

    private static DatabaseManager dbManager;
    private static UserDao userDao;
    private static LoanDao loanDao;
    private static AuditLogDao auditLogDao;
    private static EmailService emailService;
    private static AdminService adminService;
    private static LoanService loanService;
    private static EmiCalculator emiCalculator;

    private static final String TEST_USER_EMAIL = "stage5_borrower@example.com";
    private static final String TEST_USER_USERNAME = "stage5borrower";
    private static final String TEST_ADMIN_USERNAME = "stage5admin";
    private static final String TEST_ADMIN_EMAIL = "stage5_admin@example.com";

    private static User borrowerUser;
    private static User adminUser;
    private static LoanApplication testLoan;

    @BeforeAll
    static void init() {
        ConfigLoader config = new ConfigLoader();
        String url = config.getOrDefault("db.url", "jdbc:mysql://127.0.0.1:3307/loan_app?useSSL=false&serverTimezone=UTC");
        String user = config.getOrDefault("db.user", "root");
        String pass = config.getOrDefault("db.password", "");
        dbManager = new DatabaseManager(url, user, pass);

        userDao = new JdbcUserDao(dbManager);
        loanDao = new JdbcLoanDao(dbManager);
        auditLogDao = new JdbcAuditLogDao(dbManager);
        emailService = new EmailService(config);
        emiCalculator = new EmiCalculator();
        adminService = new AdminService(userDao, auditLogDao, emailService, loanDao, emiCalculator);
        loanService = new LoanService(loanDao);

        cleanup();

        // Create borrower user
        borrowerUser = new User("Rajesh", "Sharma", "9812345678", TEST_USER_EMAIL, "Delhi",
                TEST_USER_USERNAME, "hashedpass");
        borrowerUser.setRole(Role.ROLE_USER);
        borrowerUser.setStatus(Status.ACTIVE);
        borrowerUser = userDao.save(borrowerUser);

        // Create admin user
        adminUser = new User("Super", "Admin", "9812345679", TEST_ADMIN_EMAIL, "Mumbai",
                TEST_ADMIN_USERNAME, "hashedpass");
        adminUser.setRole(Role.ROLE_ADMIN);
        adminUser.setStatus(Status.ACTIVE);
        adminUser = userDao.save(adminUser);
    }

    @AfterAll
    static void tearDown() {
        cleanup();
    }

    private static void cleanup() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM loan_applications WHERE purpose LIKE '%Stage5%'");
            stmt.executeUpdate("DELETE FROM users WHERE email IN ('" + TEST_USER_EMAIL + "', '" + TEST_ADMIN_EMAIL + "')");
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. New loan application is created with PENDING status and null EMI fields")
    void testLoanCreationWithPendingEmi() {
        BigDecimal loanAmount = new BigDecimal("500000.00");
        BigDecimal rate = new BigDecimal("8.50");
        int durationMonths = 60;

        testLoan = loanService.applyForLoan(
                borrowerUser,
                loanAmount,
                LoanType.HOME,
                durationMonths,
                new BigDecimal("1500000.00"),
                "Stage5 Home Loan Purpose",
                rate
        );

        assertNotNull(testLoan.getId());
        assertEquals(LoanStatus.PENDING, testLoan.getStatus());
        assertNull(testLoan.getEmiAmount());
        assertNull(testLoan.getTotalInterest());
        assertNull(testLoan.getTotalPayable());

        // Verify from database directly
        Optional<LoanApplication> fromDb = loanDao.findById(testLoan.getId());
        assertTrue(fromDb.isPresent());
        assertNull(fromDb.get().getEmiAmount());
    }

    @Test
    @Order(2)
    @DisplayName("2. Non-admin user cannot approve loan applications (RBAC)")
    void testNonAdminCannotApprove() {
        assertThrows(AccessDeniedException.class, () ->
                adminService.approveLoan(borrowerUser, testLoan.getId()));
    }

    @Test
    @Order(3)
    @DisplayName("3. Admin approves loan: atomic transaction persists APPROVED status and calculated EMI breakdown")
    void testAdminApproveLoanCalculatesEmi() {
        LoanApplication approved = adminService.approveLoan(adminUser, testLoan.getId());

        assertEquals(LoanStatus.APPROVED, approved.getStatus());
        assertNotNull(approved.getEmiAmount());
        assertNotNull(approved.getTotalInterest());
        assertNotNull(approved.getTotalPayable());

        // Reference check check from spec: P=500000, 8.5%, 60 months -> EMI = 10258.27
        assertEquals(new BigDecimal("10258.27"), approved.getEmiAmount());

        BigDecimal expectedTotalPayable = new BigDecimal("10258.27").multiply(BigDecimal.valueOf(60));
        assertEquals(expectedTotalPayable, approved.getTotalPayable());

        BigDecimal expectedTotalInterest = expectedTotalPayable.subtract(new BigDecimal("500000.00"));
        assertEquals(expectedTotalInterest, approved.getTotalInterest());
    }

    @Test
    @Order(4)
    @DisplayName("4. Database query confirms persistence of all three EMI breakdown columns in MySQL")
    void testDatabasePersistenceOfEmiFields() {
        Optional<LoanApplication> fromDb = loanDao.findById(testLoan.getId());
        assertTrue(fromDb.isPresent());

        LoanApplication l = fromDb.get();
        assertEquals(LoanStatus.APPROVED, l.getStatus());
        assertEquals(new BigDecimal("10258.27"), l.getEmiAmount());
        assertNotNull(l.getTotalInterest());
        assertNotNull(l.getTotalPayable());
        assertEquals(0, l.getTotalPayable().compareTo(l.getEmiAmount().multiply(BigDecimal.valueOf(60))));
    }

    @Test
    @Order(5)
    @DisplayName("5. Approving an already approved loan is rejected with validation exception")
    void testCannotReapproveLoan() {
        assertThrows(ValidationException.class, () ->
                adminService.approveLoan(adminUser, testLoan.getId()));
    }

    @Test
    @Order(6)
    @DisplayName("6. Email service delivers loan approval notification with full breakdown details")
    void testLoanApprovalEmailDispatch() {
        boolean sent = emailService.sendLoanApprovedEmail(
                TEST_USER_EMAIL,
                "Rajesh",
                new BigDecimal("500000.00"),
                LoanType.HOME,
                60,
                new BigDecimal("8.50"),
                new BigDecimal("10258.27"),
                new BigDecimal("115496.20"),
                new BigDecimal("615496.20")
        );
        assertTrue(sent, "Approval email dispatch must succeed");
    }
}
