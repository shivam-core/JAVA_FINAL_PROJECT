# Loan Application System (Core Java & MySQL)

A graded, production-ready console loan application and management system built in pure **Core Java 17**, **JDBC**, and **MySQL 8.0+**. Developed adhering to strict layered enterprise architecture without Spring, Hibernate, or any third-party ORM.

---

## 1. System Overview

The system models an end-to-end retail banking loan origination and administrative lifecycle across five comprehensive stages:
1. **Stage 1 — User Authentication & Account Security**: Secure registration, dual-identifier login (email or username), PBKDF2 password hashing with salt, 3-attempt account lockout mechanism, and self-service password reset.
2. **Stage 2 — Admin Operations & Audit Logging**: Dedicated administrative console, customer directory search, profile editing, account activation / deactivation, hard deletion with cascade handling, and transactional audit logging with notification dispatch.
3. **Stage 3 — Loan Origination & Review**: Multi-category loan applications (Home, Personal, Education, Vehicle), duration normalization (months/years), eligibility evaluation, and administrative approval/rejection workflows.
4. **Stage 4 — PAN Verification & CIBIL Risk Pricing**: PAN format verification (`[A-Z]{5}[0-9]{4}[A-Z]{1}`), deterministic CIBIL score simulation (600–900), risk-adjusted tiered interest rates, guest rate calculator, and instant guest registration with auto-generated credentials emailed directly to the customer.
5. **Stage 5 — EMI Sanction & Loan Disbursal**: Pure mathematical EMI calculation (`DECIMAL128`), monthly repayment schedules, total interest and payable breakdowns, atomic database approval transactions, and detailed email notifications upon loan approval and rejection.

---

## 2. Architecture & Design Principles

```
  ┌────────────────────────────────────────────────────────┐
  │                    Console UI Layer                    │
  │    (ConsoleInput, MainMenu, UserMenu, AdminMenu)       │
  └───────────────────────────┬────────────────────────────┘
                              │
  ┌───────────────────────────▼────────────────────────────┐
  │                     Service Layer                      │
  │ (AuthService, UserService, AdminService, LoanService,  │
  │  CibilService, InterestRateService, EmiCalculator,     │
  │  EmailService)                                         │
  └───────────────────────────┬────────────────────────────┘
                              │
  ┌───────────────────────────▼────────────────────────────┐
  │               Data Access Layer (DAO)                  │
  │      (UserDao, LoanDao, AuditDao, Jdbc*Dao)            │
  └───────────────────────────┬────────────────────────────┘
                              │
  ┌───────────────────────────▼────────────────────────────┐
  │                   MySQL 8.0+ Database                  │
  │   (users, roles, user_roles, audit_log, loan_apps)     │
  └────────────────────────────────────────────────────────┘
```

### Core Engineering Invariants:
- **Zero Spring / Hibernate / ORMs**: Implemented with pure standard Java SE 17 and JDBC.
- **Strict SQL Parameterization**: 100% of queries use `PreparedStatement` with try-with-resources. Zero string concatenation in SQL queries prevents SQL injection.
- **Financial Precision (`BigDecimal`)**: All monetary computations (`loan_amount`, `annual_income`, `emi_amount`, `total_interest`, `total_payable`) use `BigDecimal` with 2 decimal places and `RoundingMode.HALF_UP`.
- **Cryptographic Security**: Passwords are never stored in plaintext. They are salted with a 16-byte cryptographically secure random salt and hashed using `PBKDF2WithHmacSHA256` with 120,000 iterations.
- **Fail-safe Asynchronous/Logged Notifications**: SMTP email errors do not roll back successful database transactions. Failures are logged and surfaced to operators.
- **Strict Console I/O Isolation**: Zero `System.out.println` or user-facing console prompts outside the `com.shivamcore.loanapp.ui` package. All internal logging uses SLF4J + Logback.

---

## 3. Tech Stack & Dependencies

| Layer / Role | Technology | Version |
|---|---|---|
| Runtime | Java (JDK LTS) | 17 |
| Build Tool | Apache Maven | 3.9+ |
| Database | MySQL Server | 8.0+ / 9.x |
| JDBC Driver | MySQL Connector/J | 8.4.0 |
| Logging API | SLF4J | 2.0.13 |
| Logging Provider | Logback Classic | 1.5.6 |
| Mail API | Jakarta Mail API | 2.1.3 |
| Mail Provider | Eclipse Angus Mail | 2.0.3 |
| Test Framework | JUnit 5 Jupiter | 5.10.2 |
| Packaging | Maven Shade Plugin | 3.5.2 |

---

## 4. Prerequisites & Setup

### Prerequisites
- **JDK 17+**: Ensure `java -version` returns 17 or higher.
- **Apache Maven 3.9+**: Ensure `mvn -version` is available.
- **MySQL Server 8.0+**: Running locally or remotely.

### Database Setup
1. Create the database schema in MySQL:
   ```sql
   CREATE DATABASE loan_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Apply migrations in sequential order from the `sql/` directory:
   ```bash
   mysql -u root -p loan_app < sql/V1__stage1_users.sql
   mysql -u root -p loan_app < sql/V2__stage2_admin.sql
   mysql -u root -p loan_app < sql/V3__stage3_loans.sql
   mysql -u root -p loan_app < sql/V4__stage4_cibil.sql
   mysql -u root -p loan_app < sql/V5__stage5_emi.sql
   ```

### Configuration & Credentials
The application reads configuration with the following precedence:
1. Environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `MAIL_SMTP_HOST`, `MAIL_SMTP_PORT`, `MAIL_FROM`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`)
2. Custom property file passed via system property `-Dconfig.file=/path/to/custom.properties`
3. Local `application.properties` in current working directory
4. Classpath `application.properties`

To configure via file:
```bash
cp src/main/resources/application.properties.example application.properties
```

Edit `application.properties` with your credentials:
```properties
db.url=jdbc:mysql://localhost:3306/loan_app?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_MYSQL_PASSWORD

smtp.host=smtp.gmail.com
smtp.port=587
smtp.username=your_email@gmail.com
smtp.password=YOUR_GMAIL_APP_PASSWORD
smtp.from=noreply@shivamcore.com
mail.enabled=true

admin.username=admin
admin.password=Admin@1234
```
> **Note**: For local testing without an active SMTP server, set `mail.enabled=false`. The system will gracefully log all email contents to `logs/application.log` and the audit log without network failure.

---

## 5. Building & Running

### Run Automated Tests
Execute all unit tests and acceptance suites (spanning Stage 1 through Stage 5):
```bash
mvn clean test
```

### Build Runnable Fat (Uber) JAR
Build the standalone executable jar with all bundled dependencies:
```bash
mvn clean package -DskipTests
```
The output artifact is generated at `target/loan-application-system-1.0.0.jar`.

### Run Standalone Fat JAR
```bash
java -jar target/loan-application-system-1.0.0.jar
```
Or with environment variable overrides:
```bash
DB_URL="jdbc:mysql://localhost:3306/loan_app" \
DB_USER="root" \
DB_PASSWORD="your_password" \
java -jar target/loan-application-system-1.0.0.jar
```

### Run via Maven (Development)
```bash
mvn compile exec:java -Dexec.mainClass="com.shivamcore.loanapp.Main"
```

---

## 6. Predefined Administrative Credentials

Upon startup, the system automatically inspects the database. If no administrator exists, `AdminSeeder` provisions the initial administrator account using configured credentials:
- **Username**: `admin`
- **Password**: `Admin@1234`
- **Role**: `ROLE_ADMIN`

---

## 7. Menu Navigation Map

```
==================================================
              MAIN PORTAL MENU
==================================================
1. Register
2. Login (Username or Email)
3. Forgot Password
4. Interest Rate Calculator (Guest PAN check)
5. Check Eligibility & Apply for Loan (Instant Registration)
0. Exit

  ┌──────────────────────────────────────────────┐
  │           CUSTOMER DASHBOARD MENU            │
  ├──────────────────────────────────────────────┤
  │ 1. View My Profile                           │
  │ 2. Apply for a Loan (with CIBIL rates)       │
  │ 3. View My Loan Applications (Status & EMI)  │
  │ 0. Logout                                    │
  └──────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────┐
  │            ADMIN DASHBOARD MENU              │
  ├──────────────────────────────────────────────┤
  │ 1. View All Users                            │
  │ 2. Search User (by ID, username, or email)   │
  │ 3. Edit User Details                         │
  │ 4. Activate User Account                     │
  │ 5. Deactivate User Account                   │
  │ 6. Delete User Account (with Audit Trail)    │
  │ 7. Review Pending Loan Applications          │
  │ 8. View All Loan Applications (with EMI)     │
  │ 0. Logout                                    │
  └──────────────────────────────────────────────┘
```

---

## 8. Architectural & Design Decisions

The following explicit design decisions govern the system implementation:
1. **Loan Type Standardization**: Standardized on four supported enum types: `HOME`, `PERSONAL`, `EDUCATION`, and `VEHICLE` (with "Car" supported as an alias for Vehicle).
2. **Automatic Admin Seeding**: If no user with `ROLE_ADMIN` exists in the database on startup, the application creates the configured default admin with PBKDF2 hashed credentials.
3. **Lockout vs Deactivation Alignment**: Both user lockout (triggered by 3 consecutive failed login attempts) and manual admin deactivation set `status = INACTIVE`. When an admin reactivates an account, `failed_attempts` is automatically reset to 0.
4. **Lockout Counter Scope**: Only failed login attempts against existing non-admin accounts increment the failed attempt counter. Unknown usernames/emails do not leak account presence and do not lock out accounts. Administrators cannot be locked out.
5. **Inactive Login Feedback**: Inactive or locked accounts are notified of their inactive status only after entering correct credentials, avoiding user enumeration attacks.
6. **Forgot Password Authorization**: Password reset operates via verified identity (username/email matching mobile number) and enforces standard password complexity. Locked/inactive accounts cannot use password reset to bypass admin lockouts.
7. **Database-Level Unique Constraints**: Username and email uniqueness are validated in `UserService` and strictly enforced by unique database constraints.
8. **Loan Duration Storage**: Durations can be entered in either months or years at user convenience; the system normalizes and persists all durations as integer months.
9. **Interest Rate Precedence**: When a user applies for a loan, the system evaluates their verified CIBIL score. If found, the risk-adjusted rate is applied; otherwise, the loan category midpoint rate is utilized.
10. **Dual Stage 4 Workflows**: Supported both a guest Rate Calculator (input PAN + Loan Type to get CIBIL score & quote) and an integrated instant-apply flow (auto-generates account, computes CIBIL rate, saves application, and emails credentials).
11. **Deterministic CIBIL Score Retention**: CIBIL scores are simulated between 600 and 900 on first query. If a PAN already has a recorded CIBIL score, the existing score is retained for consistency.
12. **Guest Username Collision Resolution**: Auto-generated guest usernames (`firstname.lastname`) append random 2-digit numeric suffixes if collisions occur (up to 5 retries).
13. **Additive Non-Destructive Migrations**: Stage 5 added `emi_amount`, `total_interest`, and `total_payable` as nullable columns via `V5__stage5_emi.sql`, preserving backward compatibility with Stage 3 and Stage 4 records.
14. **Mobile Number Validation**: Validates exactly 10 numeric digits per specification without restricting telecom provider prefixes.
15. **Cascade User Deletion & Auditing**: Deleting a user requires explicit admin confirmation (`yes`), cascades to delete their associated loan records within a transaction, and records an immutable audit log entry.

---

## 9. Known Limitations

- **Console Password Echo in IDEs**: When running inside non-interactive or IDE terminal wrappers (such as Eclipse, IntelliJ, or VS Code terminal), `System.console()` returns null. The application automatically falls back to standard scanner input with an informational note.
- **Forgot Password Without OTP**: In strict alignment with the provided project specification, password reset verifies matching user records without a separate out-of-band one-time password (OTP).
- **Simulated CIBIL Credit Bureau**: Real-world CIBIL API integration requires paid banking aggregator licenses; credit scores are simulated deterministically within 600–900 using `ThreadLocalRandom`.

---

## 10. End-to-End Demo Walkthrough

Follow this step-by-step walkthrough to test and demonstrate all 5 stages of the application:

### Step 1: Guest CIBIL Check & Instant Loan Application
1. Launch the application: `java -jar target/loan-application-system-1.0.0.jar`.
2. Select `5` (**Check Eligibility & Apply for Loan**).
3. Enter details:
   - First Name: `Rahul`
   - Last Name: `Sharma`
   - Email: `rahul.sharma@example.com`
   - Mobile: `9876543210`
   - PAN: `ABCDE1234F`
   - Annual Income: `750000`
   - Loan Type: `HOME`
   - Amount: `500000`
   - Duration: `60` months
4. **Result**:
   - System simulates CIBIL score (e.g., `785`).
   - Interest rate determined (e.g., `8.5%` for score $\ge 750$).
   - Creates new customer account `rahul.sharma` with random password.
   - Saves loan application with status `PENDING`.
   - Sends welcome email with login credentials and loan submission confirmation.

### Step 2: Login as Registered Customer
1. Select `2` (**Login**).
2. Enter username `rahul.sharma` and password received in the console/email.
3. Select `3` (**View My Loan Applications**).
4. **Result**: Loan ID #1 is displayed with status `PENDING` and interest rate `8.5%`.
5. Select `0` to log out.

### Step 3: Admin Review, Loan Approval & EMI Sanction
1. Select `2` (**Login**).
2. Enter username `admin` and password `Admin@1234`.
3. Select `7` (**Review Pending Loan Applications**).
4. System displays pending application for Rahul Sharma ($P = 500,000$, Rate = $8.5\%$, $N = 60$).
5. Select `A` to Approve.
6. **Result**:
   - System calculates EMI via `EmiCalculator`:
     $$\text{EMI} = 10,258.27, \quad \text{Total Interest} = 115,496.20, \quad \text{Total Payable} = 615,496.20$$
   - Database record updated atomically: `status = APPROVED`, `emi_amount = 10258.27`.
   - Approval email dispatched to customer containing full EMI breakdown.
7. Select `8` (**View All Loan Applications**): Confirms approved status and stored EMI details.
8. Select `0` to log out.

### Step 4: Customer Verification of Approved EMI
1. Select `2` (**Login**) with customer credentials.
2. Select `3` (**View My Loan Applications**).
3. **Result**: Status shows `APPROVED`, displaying Monthly EMI: `₹10,258.27`, Total Interest: `₹115,496.20`, Total Payable: `₹615,496.20`.
4. Select `0` to log out, then `0` to exit.
