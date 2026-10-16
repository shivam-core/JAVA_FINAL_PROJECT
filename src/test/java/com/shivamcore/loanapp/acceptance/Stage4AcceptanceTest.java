package com.shivamcore.loanapp.acceptance;

import com.shivamcore.loanapp.config.ConfigLoader;
import com.shivamcore.loanapp.config.DatabaseManager;
import com.shivamcore.loanapp.dao.*;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.*;
import com.shivamcore.loanapp.service.*;
import com.shivamcore.loanapp.util.CredentialGenerator;
import com.shivamcore.loanapp.util.PasswordHasher;
import com.shivamcore.loanapp.util.Validator;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage4AcceptanceTest {

    private static DatabaseManager dbManager;
    private static UserDao userDao;
    private static LoanDao loanDao;
    private static LoanService loanService;
    private static CibilService cibilService;
    private static InterestRateService interestRateService;
    private static EmailService emailService;

    private static final String TEST_PAN = "ABCDE1234F";
    private static final String TEST_MOBILE = "9876543210";
    private static final String TEST_EMAIL = "stage4_guest@example.com";
    private static User guestUser;

    @BeforeAll
    static void init() {
        ConfigLoader config = new ConfigLoader();
        String url = config.getOrDefault("db.url", "jdbc:mysql://127.0.0.1:3307/loan_app?useSSL=false&serverTimezone=UTC");
        String user = config.getOrDefault("db.user", "root");
        String pass = config.getOrDefault("db.password", "");
        dbManager = new DatabaseManager(url, user, pass);

        userDao = new JdbcUserDao(dbManager);
        loanDao = new JdbcLoanDao(dbManager);
        loanService = new LoanService(loanDao);
        cibilService = new CibilService(userDao);
        interestRateService = new InterestRateService();
        emailService = new EmailService(config);

        cleanup();
    }

    @AfterAll
    static void tearDown() {
        cleanup();
    }

    private static void cleanup() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM loan_applications WHERE purpose LIKE '%Stage4%'");
            stmt.executeUpdate("DELETE FROM users WHERE email = '" + TEST_EMAIL + "' OR pan_card = '" + TEST_PAN + "'");
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. PAN validation accurately accepts valid formats and rejects invalid formats")
    void testPanValidation() {
        assertTrue(Validator.isValidPan("ABCDE1234F"));
        assertTrue(Validator.isValidPan("abcde1234f")); // Validator handles case normalization

        assertFalse(Validator.isValidPan("ABC123456F"));
        assertFalse(Validator.isValidPan("ABCDE12345"));
        assertFalse(Validator.isValidPan("12345ABCDE"));
        assertFalse(Validator.isValidPan(""));
        assertFalse(Validator.isValidPan(null));

        assertThrows(ValidationException.class, () -> Validator.validatePan("INVALID_PAN"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Interest rate calculation matches exact linear formula across boundary scores")
    void testInterestRateCalculations() {
        // HOME: 6.5 to 8.5
        assertEquals(new BigDecimal("8.50"), interestRateService.calculate(LoanType.HOME, 600));
        assertEquals(new BigDecimal("6.50"), interestRateService.calculate(LoanType.HOME, 900));
        assertEquals(new BigDecimal("7.70"), interestRateService.calculate(LoanType.HOME, 720));

        // PERSONAL: 9.0 to 12.0
        assertEquals(new BigDecimal("12.00"), interestRateService.calculate(LoanType.PERSONAL, 600));
        assertEquals(new BigDecimal("9.00"), interestRateService.calculate(LoanType.PERSONAL, 900));

        // Format summary check: For PAN ABCDE1234F, with a CIBIL score of 720, the interest rate for Home Loan is 7.70%.
        String summary = interestRateService.formatSummary("ABCDE1234F", 720, LoanType.HOME, new BigDecimal("7.70"));
        assertEquals("For PAN ABCDE1234F, with a CIBIL score of 720, the interest rate for Home Loan is 7.70%.", summary);
    }

    @Test
    @Order(3)
    @DisplayName("3. Guest credentials generator produces properly formatted username and strong password")
    void testGuestCredentialGeneration() {
        String username = CredentialGenerator.generateUsername("Shivani", "Chaudhari", TEST_MOBILE, userDao);
        assertEquals("ShiCha3210", username);

        String password = CredentialGenerator.generatePassword();
        assertTrue(Validator.isValidPassword(password));
    }

    @Test
    @Order(4)
    @DisplayName("4. Guest user creation persists PAN and sends credential notification email")
    void testGuestUserCreationAndEmail() {
        String username = CredentialGenerator.generateUsername("Shivani", "Chaudhari", TEST_MOBILE, userDao);
        String rawPassword = CredentialGenerator.generatePassword();
        String passwordHash = PasswordHasher.hash(rawPassword);

        User user = new User("Shivani", "Chaudhari", TEST_MOBILE, TEST_EMAIL, "Pune", username, passwordHash);
        user.setPanCard(TEST_PAN);
        user.setUserRole(Role.ROLE_USER);
        user.setStatus(Status.ACTIVE);

        guestUser = userDao.save(user);
        assertNotNull(guestUser.getId());
        assertEquals(TEST_PAN, guestUser.getPanCard());

        // Credential email dispatch
        boolean emailed = emailService.sendCredentialsEmail(TEST_EMAIL, "Shivani", username, rawPassword);
        assertTrue(emailed);
    }

    @Test
    @Order(5)
    @DisplayName("5. CIBIL score is generated (600-900), persisted to database, and reused on subsequent requests")
    void testCibilGenerationAndReuse() {
        int cibil = cibilService.getOrGenerateCibil(TEST_PAN);
        assertTrue(cibil >= 600 && cibil <= 900, "CIBIL must be within [600, 900]");

        guestUser.setCibilScore(cibil);
        userDao.update(guestUser);

        // Fetch from DB to verify persistence
        Optional<User> fetched = userDao.findById(guestUser.getId());
        assertTrue(fetched.isPresent());
        assertEquals(cibil, fetched.get().getCibilScore());

        // Decision 11: Reuse existing score for the same PAN
        int reusedScore = cibilService.getOrGenerateCibil(TEST_PAN);
        assertEquals(cibil, reusedScore, "Subsequent check on same PAN must reuse stored score");
    }

    @Test
    @Order(6)
    @DisplayName("6. Loan application submitted with CIBIL-adjusted rate is persisted accurately in MySQL")
    void testLoanApplicationWithCibilRate() {
        int cibil = guestUser.getCibilScore();
        BigDecimal cibilRate = interestRateService.calculate(LoanType.HOME, cibil);

        LoanApplication loan = loanService.applyForLoan(
                guestUser,
                new BigDecimal("500000.00"),
                LoanType.HOME,
                60,
                new BigDecimal("1200000.00"),
                "Stage4 CIBIL Home Loan Purpose",
                cibilRate
        );

        assertNotNull(loan.getId());
        assertEquals(cibilRate, loan.getInterestRate());
        assertEquals(LoanStatus.PENDING, loan.getStatus());

        Optional<LoanApplication> fromDb = loanDao.findById(loan.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(0, cibilRate.compareTo(fromDb.get().getInterestRate()));
    }
}
