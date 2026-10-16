# Loan Application System (Core Java & MySQL)

A graded, production-grade console application built in pure Core Java 17, JDBC, and MySQL. It implements role-based user authentication, administrative user management, loan applications with CIBIL-based interest rates, and loan sanctioning with EMI calculation and email notifications.

---

## 1. Project Overview

The Loan Application System manages end-to-end loan application lifecycles across five stages:
1. **User Authentication & Lifecycle**: User registration, login with email or username, 3-attempt account lockout, and password reset.
2. **Admin Operations & Auditing**: User search, profile editing, activation/deactivation, hard deletion with audit logging, and email notifications.
3. **Loan Application**: Multi-type loan requests (Home, Personal, Education, Vehicle), validation, duration calculation, and admin approval/rejection.
4. **CIBIL Score & Risk-Adjusted Rates**: PAN validation, simulated CIBIL score generation, interest rate determination, and guest registration flow.
5. **EMI Calculation & Sanction**: Pure mathematical EMI calculation (`DECIMAL128`), total interest & payable breakdown, atomic database transaction on approval, and email notifications.

---

## 2. Architecture & Design Principles

The application strictly adheres to a clean layered architecture with separation of concerns:

```
[ Console UI ] (ConsoleInput, MainMenu, UserMenu, AdminMenu, LoanMenu)
       ↓
[ Service Layer ] (AuthService, UserService, AdminService, LoanService, CibilService, InterestRateService, EmiCalculator, EmailService)
       ↓
[ Data Access Layer (DAO) ] (UserDao, LoanDao, AuditDao, Jdbc* Implementations)
       ↓
[ MySQL Database ] (Accessed ONLY via JDBC PreparedStatement)
```

### Core Engineering Rules:
- **Zero ORM / Spring**: Built with pure Core Java and JDBC.
- **Strict Parameterization**: 100% of SQL statements execute via `PreparedStatement` to prevent SQL injection.
- **BigDecimal for Currency**: All monetary values (`loan_amount`, `annual_income`, `emi_amount`, `total_interest`, `total_payable`) use `BigDecimal` with 2 decimal places and `RoundingMode.HALF_UP`.
- **Cryptographic Security**: Passwords are never stored in plaintext. They are salted with a 16-byte cryptographically secure random salt and hashed using `PBKDF2WithHmacSHA256` with 120,000 iterations.
- **Strict Console I/O Isolation**: No `System.out.println` outside the `ui` package. All logging across service and DAO layers uses SLF4J + Logback.
- **Fail-safe Notifications**: Email failures never roll back committed database transactions. Failures are logged and surfaced to administrators.

---

## 3. Tech Stack & Dependencies

| Component | Technology | Version |
|---|---|---|
| Language | Java (LTS) | 17 |
| Build Tool | Apache Maven | 3.9+ |
| Database | MySQL Server | 8.0+ / 9.x |
| Database Driver | MySQL Connector/J | 8.4.0 |
| Logging API | SLF4J | 2.0.13 |
| Logging Implementation | Logback Classic | 1.5.6 |
| Email API | Jakarta Mail API | 2.1.3 |
| Email Implementation | Angus Mail | 2.0.3 |
| Testing | JUnit 5 Jupiter | 5.10.2 |

---

## 4. Setup & Installation

### Prerequisites
- Java Development Kit (JDK) 17 or higher (`java -version`)
- Apache Maven 3.9+ (`mvn -version`)
- MySQL 8.0+ running locally or remotely

### Database Setup
1. Create the database:
   ```sql
   CREATE DATABASE loan_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Execute migration scripts in sequential order:
   - `sql/V1__stage1_users.sql` (Stage 1: users table)
   - `sql/V2__stage2_roles_audit.sql` (Stage 2: roles & audit_log)
   - `sql/V3__stage3_loans.sql` (Stage 3: loan_applications table)
   - `sql/V4__stage4_pan_cibil.sql` (Stage 4: PAN & CIBIL score columns)
   - `sql/V5__stage5_emi.sql` (Stage 5: EMI breakdown columns)

### Configuration
Copy the configuration template:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```
Edit `src/main/resources/application.properties` with your database and SMTP credentials:
```properties
db.url=jdbc:mysql://localhost:3306/loan_app?useSSL=false&serverTimezone=UTC
db.user=root
db.password=YOUR_MYSQL_PASSWORD
smtp.host=smtp.gmail.com
smtp.port=587
smtp.username=your_email@gmail.com
smtp.password=YOUR_GMAIL_APP_PASSWORD
smtp.from=your_email@gmail.com
mail.enabled=true
admin.username=admin
admin.password=Admin@1234
```
> **Note**: For local development or running tests without an SMTP server, set `mail.enabled=false`. The application will log outgoing emails instead of attempting SMTP transport.

---

## 5. Building and Running

### Build & Run Tests
```bash
mvn clean test
```

### Run Application
```bash
mvn compile exec:java -Dexec.mainClass="com.shivamcore.loanapp.Main"
```

---

## 6. Design Decisions

The following architectural and design decisions were made to resolve ambiguities between requirement specifications:
1. **Loan Type Naming**: Standardized on the enum `VEHICLE` (while accepting "Car" as an input alias).
2. **Admin Seeding**: Predefined admin user is seeded on startup via `AdminSeeder` using configured credentials if no `ROLE_ADMIN` user exists. Passwords are created using standard PBKDF2 hashing.
3. **Lockout vs Deactivation**: Both set `status = INACTIVE`. When an administrator activates an account, `failed_attempts` is automatically reset to 0.
4. **Lockout Counter Scope**: Only failed login attempts for existing non-admin accounts increment the lockout counter. Non-existent identifiers do not increment counters. Admin accounts are excluded from lockout.
5. **Inactive Login Feedback**: Accounts that are inactive/locked report their status only after password verification to prevent account enumeration.
6. **Forgot Password Flow**: Password reset operates directly via verified email/username matching the password policy. Locked/inactive accounts cannot use password reset to bypass admin lockouts.
7. **Unique Constraints**: Usernames and emails are validated at the service layer and enforced by database unique constraints.
8. **Loan Duration Storage**: Durations can be entered in months or years; they are converted and stored internally as months.
9. **Interest Rate Precedence**: Uses CIBIL-adjusted rate if available; otherwise defaults to the midpoint of the loan type rate range.
10. **Dual Stage 4 Flows**: Provides an independent Rate Calculator flow (PAN + Loan Type) and a Guest CIBIL check flow with automated account creation.
11. **CIBIL Regeneration**: Existing scores for a given PAN are retained rather than regenerated randomly on repeated checks.
12. **Guest Username Collisions**: Colliding auto-generated usernames append a 2-digit random suffix (up to 5 retries).
13. **Additive Schema Evolution**: Stage 5 adds EMI columns non-destructively through `V5__stage5_emi.sql`.
14. **Mobile Validation**: Strictly validates 10 numeric digits per specification without restricting leading prefixes.
15. **Cascade User Deletion**: Deleting a user cascades to their loan records with explicit admin confirmation and audit logging.

---

## 7. Known Limitations

- **Console Password Masking in IDEs**: When running inside graphical IDE consoles (IntelliJ, Eclipse, VS Code), `System.console()` is null; the system falls back to standard console input.
- **Forgot Password OTP**: The specification defines password reset without OTP verification; reset is protected by account status and credential validation.
- **Simulated CIBIL Score**: CIBIL scores are generated in the range 600–900 using `ThreadLocalRandom` for simulation purposes.
