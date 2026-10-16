# BUILD_GUIDE.md — Loan Application System

Source of truth for the build. The agent reads this file first, follows it in order, and does not deviate without writing the reason in the commit body.

Original requirement PDFs are in `docs/requirements/Stage_1.pdf` … `Stage_5.pdf`. This guide distills them, resolves their contradictions, and adds engineering rules. If this guide and a PDF disagree, the PDF wins except where this guide explicitly says "DECISION".

---

## 0. Stack and non-negotiables

| Item | Choice |
|---|---|
| Language | Java 17 (LTS) |
| Build | Maven, single module |
| DB | MySQL 8, accessed **only** via JDBC (`PreparedStatement`, never string-concatenated SQL) |
| Mail | Jakarta Mail (Angus implementation) over SMTP |
| Logging | SLF4J + Logback |
| Tests | JUnit 5 (pure unit tests, no DB needed) |
| UI | Console only. No Spring, no frontend, no ORM (Hibernate/JPA are out) |

Verify every dependency version on Maven Central before pinning. Pin exact versions, no ranges.

Rules that apply to every stage:

1. Layers: `ui → service → dao → db`. UI never touches SQL. DAO never prints or reads the console.
2. One shared `Scanner` wrapped in `ConsoleInput`. Never create a second `Scanner(System.in)`.
3. All money is `BigDecimal`, scale 2, `RoundingMode.HALF_UP`. Never `double` for money.
4. All JDBC uses try-with-resources. Multi-statement operations use a transaction (commit/rollback).
5. No secrets in git. Ever. Config comes from environment variables, falling back to a gitignored `application.properties`.
6. Passwords are stored **hashed** (PBKDF2WithHmacSHA256 from the JDK, per-user random salt, 120k+ iterations). The auto-generated password in Stage 4 is emailed in plaintext once, then only the hash is stored.
7. Email failure never rolls back a committed DB change. Log it, tell the admin/user the notification failed.
8. No `System.out.println` outside the `ui` package. Everything else uses the logger.
9. Custom exceptions (`ValidationException`, `AuthenticationException`, `DuplicateResourceException`, `DataAccessException`, `NotificationException`). No swallowed exceptions, no `catch (Exception e) {}`.
10. No magic strings for status/role/loan type: use enums.

---

## 1. Repository layout

```
JAVA_FINAL_PROJECT/
├── BUILD_GUIDE.md
├── README.md
├── pom.xml
├── .gitignore
├── docs/
│   └── requirements/            Stage_1.pdf … Stage_5.pdf
├── sql/
│   ├── V1__stage1_users.sql
│   ├── V2__stage2_roles_audit.sql
│   ├── V3__stage3_loans.sql
│   ├── V4__stage4_pan_cibil.sql
│   └── V5__stage5_emi.sql
├── src/main/resources/
│   ├── application.properties.example
│   └── logback.xml
├── src/main/java/com/shivamcore/loanapp/
│   ├── Main.java
│   ├── config/         ConfigLoader, DatabaseManager
│   ├── model/          User, LoanApplication, AuditLog, + enums (Status, Role, LoanType, LoanStatus)
│   ├── dao/            UserDao, LoanDao, AuditDao (+ Jdbc* implementations)
│   ├── service/        AuthService, UserService, AdminService, LoanService,
│   │                   CibilService, InterestRateService, EmiCalculator, EmailService, AuditService
│   ├── util/           Validator, PasswordHasher, CredentialGenerator, ConsoleInput
│   ├── exception/
│   └── ui/             MainMenu, UserMenu, AdminMenu, LoanMenu
└── src/test/java/com/shivamcore/loanapp/   mirrors main
```

Schema evolves through the numbered SQL files, one per stage, so the git history shows the schema growing exactly as the assignment does. Each file is idempotent where possible (`CREATE TABLE IF NOT EXISTS`, guarded `ALTER`). A `docs`-style `sql/README` note explains run order. The app does not auto-run DDL; the README tells the user to run the files in order.

---

## 2. Configuration

`application.properties.example` (committed):

```
db.url=jdbc:mysql://localhost:3306/loan_app?useSSL=false&serverTimezone=UTC
db.user=root
db.password=CHANGE_ME
smtp.host=smtp.gmail.com
smtp.port=587
smtp.username=your_email@gmail.com
smtp.password=GMAIL_APP_PASSWORD
smtp.from=your_email@gmail.com
mail.enabled=true
admin.username=admin
admin.password=CHANGE_ME_Admin@123
```

`ConfigLoader` precedence: environment variable (`DB_URL`, `SMTP_PASSWORD`, …) → `application.properties` (gitignored) → fail fast with a clear message listing missing keys.

Gmail needs 2-step verification on and an **App Password**; the normal Gmail password will not work.

`mail.enabled=false` prints the email to the log instead of sending. This is for local dev and tests only. The submitted demo must use real SMTP because the assignment requires actual emails.

---

## 3. Database design (final state after Stage 5)

**users**

| column | type | notes |
|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | |
| fname, lname | VARCHAR(50) NOT NULL | |
| mobile | CHAR(10) NOT NULL | |
| email | VARCHAR(120) NOT NULL UNIQUE | |
| location | VARCHAR(100) NOT NULL | |
| last_modified_date | DATETIME NOT NULL | set on insert and every update |
| status | ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' | |
| username | VARCHAR(50) NOT NULL UNIQUE | |
| password_hash | VARCHAR(255) NOT NULL | salt+hash encoded |
| user_role | ENUM('ROLE_USER','ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER' | Stage 2 |
| failed_attempts | INT NOT NULL DEFAULT 0 | Stage 1 |
| pan_card | CHAR(10) NULL | Stage 4 |
| cibil_score | INT NULL | Stage 4 |

Add `CHECK (cibil_score BETWEEN 300 AND 900)` and an index on `(mobile)`, `(status)`.

**loan_applications**

| column | type | notes |
|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | |
| user_id | BIGINT NOT NULL, FK → users(id) ON DELETE CASCADE | |
| loan_amount | DECIMAL(15,2) NOT NULL | |
| loan_type | ENUM('HOME','PERSONAL','EDUCATION','VEHICLE') NOT NULL | |
| loan_duration_months | INT NOT NULL | |
| interest_rate | DECIMAL(5,2) NOT NULL | annual % |
| annual_income | DECIMAL(15,2) NOT NULL | |
| purpose | VARCHAR(255) NOT NULL | |
| status | ENUM('PENDING','APPROVED','REJECTED','NEEDS_MORE_INFO') NOT NULL DEFAULT 'PENDING' | |
| application_date | DATETIME NOT NULL | |
| rejection_reason | VARCHAR(255) NULL | |
| emi_amount | DECIMAL(15,2) NULL | Stage 5 |
| total_interest | DECIMAL(15,2) NULL | Stage 5 |
| total_payable | DECIMAL(15,2) NULL | Stage 5 |

**audit_log** (Stage 2 deletion auditing)

`id, action VARCHAR(40), performed_by VARCHAR(50), target_user_id BIGINT NULL, target_username VARCHAR(50), details VARCHAR(500), created_at DATETIME`.

DECISION: **no foreign key** from audit_log to users, because the log must survive the user's deletion. Store a username snapshot instead. Deletion is also written to `logs/audit.log` via a dedicated Logback appender.

---

## 4. Decisions on ambiguities and contradictions in the PDFs

Make these decisions, do not ask, and record them in the README under "Design decisions".

1. **Loan type naming.** Stage 3 says "Car", Stage 4 says "Vehicle Loan". DECISION: enum `VEHICLE`; accept "car" as an alias at input.
2. **Admin seeding.** Spec says predefined admin in the DB. DECISION: `AdminSeeder` runs on startup, inserts the admin from config if no `ROLE_ADMIN` row exists, with a properly hashed password. No hardcoded hash in SQL.
3. **Lockout vs deactivation.** Both set `status = INACTIVE`. Admin **Activate** must also reset `failed_attempts = 0`, otherwise an unlocked user is locked again on the next typo.
4. **Lockout scope.** Only counts wrong password for an existing non-admin user. Non-existent user: error message, no counter. Admin login never touches the counter.
5. **Inactive login message.** If the password is correct but status is INACTIVE, say the account is inactive/locked and to contact support. Do not reveal this before the password is verified (avoids leaking account state).
6. **Forgot password.** The PDF specifies no OTP. DECISION: implement as specified (identify by email or username, then set new password under the policy), but block it for INACTIVE accounts so it cannot be used to bypass a lock. State the lack of OTP as a known limitation in the README.
7. **Duplicate checks.** Username and email are both unique, checked in the service layer *and* enforced by DB constraints; catch the constraint violation as a backstop.
8. **Loan duration.** DECISION: ask for the unit (1 = months, 2 = years), convert to months internally, store months.
9. **Interest rate on loan application.** Stage 3: auto-filled by loan type (use the midpoint of the range). Stage 4: if the user has a CIBIL score, use the CIBIL-adjusted rate instead. If not, use the midpoint. Stage 3 behaviour must keep working without Stage 4 data.
10. **Stage 4 has two separate flows.** (a) Interest rate calculator: PAN + loan type → CIBIL → rate. (b) Guest "CIBIL check": personal details + PAN → auto username/password → email → CIBIL → "Apply for Loan". Both are reachable from the main menu.
11. **Stage 4 CIBIL regeneration.** CIBIL is random per spec. DECISION: if the user already has a stored score for the same PAN, reuse it instead of regenerating (a real score does not change per click). Say so in the README.
12. **Guest flow duplicates.** If the email or PAN already belongs to a registered user, reject with a clear message. If the generated username collides, append a random 2-digit suffix and retry (max 5 times).
13. **Stage 5 "don't disturb existing implementation".** Add columns through `V5__stage5_emi.sql`, extend `LoanApplication` and the DAO additively. Do not change existing method signatures; add new methods.
14. **Mobile validation.** 10 digits, starts with 6–9 (Indian mobile). If the instructor's tests might use other numbers, the stricter rule is a risk. DECISION: enforce exactly 10 digits only, as the PDF says. Do not add the 6–9 rule.
15. **Deleting a user** cascades to their loans. The confirmation prompt says so.

---

## 5. Stage-by-stage build spec

### Stage 1 — Registration, login, logout, forgot password, lockout

Menu (logged out): `1 Register · 2 Login · 3 Forgot Password · 0 Exit`.

**Register** collects fname, lname, mobile, email, location, username, password.
- Mandatory: fname, lname, mobile, email, username, password. (Location is collected; treat it as required too since Stage 4 requires it.)
- Email: regex `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`.
- Password: ≥ 8 chars, ≥ 1 upper, ≥ 1 lower, ≥ 1 digit, ≥ 1 special. Mask not possible with `Scanner`; use `System.console().readPassword()` when available, fall back to `Scanner` (IDE consoles return null).
- Mobile: exactly 10 digits.
- Username unique. Status = ACTIVE, `last_modified_date = now`, role = ROLE_USER, failed_attempts = 0.
- Re-prompt only the field that failed; do not restart the whole form.

**Login** by email **or** username (single identifier field; if it contains `@`, treat as email).
- Empty fields rejected. Unknown user → "User not found".
- Success → `Welcome <fname> <lname>`, then user menu with Logout (Stage 3 adds Apply for Loan).
- Wrong password → increment `failed_attempts` atomically in SQL (`UPDATE … SET failed_attempts = failed_attempts + 1`), then tell the user remaining attempts (`3 - n`). At 3 → `status = INACTIVE`, message "account locked, contact support".
- Success → reset `failed_attempts = 0`.

**Forgot password** per decision 6.

Acceptance: 3 wrong passwords lock the account in the DB; a 4th attempt with the right password is refused; success on attempt 2 resets the counter to 0.

### Stage 2 — Admin

- `ROLE_ADMIN` login uses the same login screen, routes to the Admin Dashboard by role. Admin is excluded from lockout.
- `AdminService` methods each call `requireAdmin(currentUser)` first. Regular users cannot reach them even by calling the service directly. This is the role-based access control.
- Dashboard: `1 Search Users · 2 View User Details · 3 Edit User · 4 Activate · 5 Deactivate · 6 Delete · 7 Loan Management (Stage 3/5) · 0 Logout`.
- **Search** by fname, lname, email, mobile, username, status (menu choice of field, then value). Use `LIKE` with bound parameters for names; exact match for mobile/status. Show results as a table.
- **View details**: full name, email, mobile, status, and the user's loan applications.
- **Edit**: validate every field with the same `Validator` as registration; update `last_modified_date`; email "Edits" template.
- **Activate / Deactivate**: set status, reset `failed_attempts` on activate; email.
- **Delete**: confirm with `yes/no`; write the audit record (DB + file) **before** deleting inside the same transaction; send the deletion email after commit (fetch the user's email/fname *before* deletion).
- Admin cannot delete or deactivate their own account or another admin (DECISION: guard against lockout of the system).
- Emails use subject `Account Update Notification` and the exact bodies from the Stage 2 PDF.

### Stage 3 — Loan application and review

**User side** (after login): `Apply for Loan`, `My Loan Applications`, `Logout`.
- Inputs: amount (> 0), type (menu), duration (> 0, decision 8), annual income (> 0), purpose (non-empty, ≤ 255).
- `interestRate` auto-filled (decision 9) and shown to the user before submit with a confirm.
- Saved with status PENDING and `application_date = now`. Show "submitted successfully" with the loan ID.

**Admin side**:
- List loans: Loan ID, user full name, amount, type, application date, status. Optional filter by status.
- View details: name, email, mobile, amount, type, duration, income, purpose, application date, status.
- Approve / Reject. Only `PENDING` (or `NEEDS_MORE_INFO`) loans can transition; approving an already decided loan is refused.
- Reject requires a non-empty reason; email carries it. Use the exact subjects/bodies from the Stage 3 PDF.
- `NEEDS_MORE_INFO` exists in the enum and is displayed; implementing an admin action for it is optional and lowest priority.

### Stage 4 — PAN, CIBIL, interest rate, auto credentials

`Validator.isValidPan`: `^[A-Z]{5}[0-9]{4}[A-Z]$`. Uppercase the input before validating. Error text exactly: `Invalid PAN format. Please enter a valid PAN.` Loan type error exactly: `Invalid loan type selected.`

`CibilService.generate()`: `ThreadLocalRandom.current().nextInt(600, 901)`. Persist to `users.cibil_score` (+ `pan_card`).

`InterestRateService.calculate(LoanType, int cibil)` — DECISION: linear interpolation inside the type's range, higher score means lower rate:

```
rate = max - ((cibil - 600) / 300.0) * (max - min)      // rounded to 2 dp
```

| Type | min | max |
|---|---|---|
| HOME | 6.5 | 8.5 |
| PERSONAL | 9.0 | 12.0 |
| EDUCATION | 4.0 | 6.0 |
| VEHICLE | 7.0 | 9.0 |

The PDF's example (720 → 7.5% for Home) is illustrative; this formula gives 7.70%. State that in the README. Unit-test the boundaries: 600 → max, 900 → min.

Output line format: `For PAN ABCDE1234F, with a CIBIL score of 720, the interest rate for Home Loan is 7.70%.`

**Guest CIBIL check flow** (menu option):
1. Collect fname, lname (letters only), mobile, email, location, PAN. Validate with the shared `Validator`.
2. `CredentialGenerator.username(fname, lname, mobile)`: first 3 letters of fname (capitalised first letter, rest lowercase) + first 3 of lname + last 4 digits of mobile. `Shivani Chaudhari 9876543210 → ShiCha3210`. Handle names shorter than 3 letters (use what exists). Collision rule: decision 12.
3. `CredentialGenerator.password()`: `SecureRandom`, 8–10 chars, guaranteed ≥ 1 upper, lower, digit, symbol, shuffled. It must satisfy the Stage 1 password policy.
4. Create the user (hashed password), role ROLE_USER, ACTIVE.
5. Send the email, subject `Your Username and Password for Loan Application is`, body with username and password.
6. **Only after the email attempt**, generate and display the CIBIL score and store it (spec order requirement).
7. Show `Apply for Loan` option; it jumps into the Stage 3 loan flow for this newly created user (treat as logged in).

### Stage 5 — EMI and approval notification

`EmiCalculator` (pure, static-free, unit-tested):

```
R = annualRate / 12 / 100
N = months
if R == 0: EMI = P / N
else:      EMI = P * R * (1+R)^N / ((1+R)^N - 1)
totalPayable  = EMI * N
totalInterest = totalPayable - P
```

Use `BigDecimal` with `MathContext.DECIMAL128` for the power and round only at the end to scale 2. Round EMI first, then compute `totalPayable = EMI × N` from the rounded EMI so the three numbers stored and emailed are mutually consistent.

Reference check (must be a unit test): P = 500000, rate 8.5 %, N = 60 → EMI ≈ 10258.27.

Approval is **one transaction**: status → APPROVED, `emi_amount`, `total_interest`, `total_payable` written together. Commit, then send the email containing sanctioned amount, loan type, tenure, applied rate, monthly EMI, total interest, total payable. If the email fails, the approval stands; the admin sees a warning.

Admin menu additions: View All Loans, Approve/Reject, each sending the decision email.

---

## 6. Testing (required, keeps the code honest)

Unit tests, no DB, no network:

- `ValidatorTest`: email, mobile, PAN, password policy, letters-only names (valid + invalid + boundary).
- `PasswordHasherTest`: hash verifies, wrong password fails, same password yields different hashes.
- `CredentialGeneratorTest`: username example from the PDF; short names; 1000 generated passwords all satisfy the policy and length 8–10.
- `InterestRateServiceTest`: 600/900 boundaries for every loan type, monotonic decrease.
- `EmiCalculatorTest`: reference case, zero-rate case, `total = emi × n`.
- `LoginLockoutTest` using in-memory fake DAOs: 3 failures lock, success resets, admin never locks.

Run `mvn -q test` before every commit that touches `src/`. A red build is never committed.

---

## 7. Git workflow (mandatory)

Repo: `https://github.com/shivam-core/JAVA_FINAL_PROJECT`

**Branches**
- `main` is always buildable and always what the evaluator sees.
- One branch per stage: `feature/stage-1-user-auth`, `feature/stage-2-admin`, `feature/stage-3-loans`, `feature/stage-4-pan-cibil`, `feature/stage-5-emi`.
- Merge with `git merge --no-ff` into `main` after the stage's acceptance checks pass, push `main`, tag `stage-N-complete`, push tags.

**Commit rules**
- Conventional Commits: `type(scope): imperative summary`, summary ≤ 72 chars. Types: `feat fix refactor test docs chore build`.
- One logical change per commit. Never `git add .` blindly; stage by path and review `git diff --staged`.
- Each commit compiles. Tests for a unit go in the same commit as the unit or the commit right after, never "all tests at the end".
- Push after **every** commit (`git push -u origin <branch>` the first time, `git push` after).
- Commit body explains *why* when non-obvious (decisions from §4 reference their number).
- No force-push to `main`. No amending pushed commits.
- Before each commit run: `git status`, check no `application.properties`, no `target/`, no `.idea`, no `logs/`.

**If a secret is ever committed:** rotate the credential immediately; deleting the commit is not enough. Do not rewrite history on a shared branch without being told to.

### Planned commit sequence

Bootstrap (on `main`):
1. `chore: initialize Maven project with Java 17 and dependencies`
2. `chore: add .gitignore and application.properties.example`
3. `docs: add requirement PDFs and BUILD_GUIDE.md`
4. `docs: add README skeleton with setup instructions`

Stage 1 (`feature/stage-1-user-auth`):
5. `feat(config): add ConfigLoader with env and properties fallback`
6. `feat(db): add DatabaseManager and JDBC connection handling`
7. `feat(sql): add V1 users table`
8. `feat(model): add User entity and Status enum`
9. `feat(util): add Validator for email, mobile, password`
10. `test(util): add ValidatorTest`
11. `feat(util): add PBKDF2 PasswordHasher`
12. `test(util): add PasswordHasherTest`
13. `feat(dao): add UserDao with JDBC implementation`
14. `feat(auth): implement registration service`
15. `feat(auth): implement login with email or username`
16. `feat(auth): add failed-attempt tracking and account lockout`
17. `test(auth): add lockout tests with fake DAO`
18. `feat(auth): implement forgot password`
19. `feat(ui): add console menus for register, login, logout, reset`
20. merge → `main`, tag `stage-1-complete`

Stage 2:
21. `feat(sql): add V2 roles and audit_log`
22. `feat(model): add Role enum and role field on User`
23. `feat(auth): seed predefined admin on startup`
24. `feat(email): add EmailService with Jakarta Mail`
25. `feat(admin): add role guard and user search`
26. `feat(admin): add view and edit user with validation`
27. `feat(admin): add activate and deactivate with notifications`
28. `feat(admin): add delete with confirmation and audit logging`
29. `feat(ui): add admin dashboard menu`
30. merge → `main`, tag `stage-2-complete`

Stage 3:
31. `feat(sql): add V3 loan_applications`
32. `feat(model): add LoanApplication, LoanType, LoanStatus`
33. `feat(loan): add loan DAO and application service with validation`
34. `feat(ui): add user loan application and my-loans screens`
35. `feat(admin): add loan listing and detail view`
36. `feat(admin): add approve and reject with email notification`
37. merge → `main`, tag `stage-3-complete`

Stage 4:
38. `feat(sql): add V4 pan_card and cibil_score`
39. `feat(util): add PAN validation`
40. `feat(cibil): add CibilService and InterestRateService`
41. `test(cibil): add interest rate boundary tests`
42. `feat(ui): add interest rate calculator flow`
43. `feat(util): add username and password generator`
44. `test(util): add CredentialGenerator tests`
45. `feat(cibil): add guest CIBIL check with credential email`
46. `feat(loan): use CIBIL-based rate in loan application`
47. merge → `main`, tag `stage-4-complete`

Stage 5:
48. `feat(sql): add V5 emi columns`
49. `feat(loan): add EmiCalculator`
50. `test(loan): add EmiCalculator tests`
51. `feat(loan): calculate and store EMI on approval in a transaction`
52. `feat(email): send approved-loan email with EMI breakdown`
53. `feat(admin): wire approve/reject and view-all-loans menu`
54. merge → `main`, tag `stage-5-complete`

Finish:
55. `build: add shade plugin for runnable jar`
56. `docs: finalize README with setup, design decisions, limitations, demo steps`
57. tag `v1.0.0`

The sequence is a plan, not a script: split further if a change is bigger than one logical unit, never combine units to hit the numbers.

---

## 8. Definition of done

- [ ] Fresh clone + README steps → running app with no undocumented steps.
- [ ] `mvn clean verify` passes.
- [ ] All five stage acceptance checks demonstrated against a real MySQL and real SMTP.
- [ ] Every email type actually delivered: activation, deactivation, deletion, edit, loan approved, loan rejected (with reason), credentials.
- [ ] `grep -ri "password" --include=*.properties .` shows only the `.example` file with placeholders.
- [ ] `git log --oneline` reads as a coherent history; `main` has tags `stage-1-complete` … `stage-5-complete`.
- [ ] README has: overview, prerequisites, DB setup, config, how to run, default admin login, menu map, design decisions (§4), known limitations (no OTP on reset, console password echo in IDEs, random CIBIL is simulated), and a demo walkthrough.
- [ ] Repo is public. Open it in a private browser window to confirm.

---

## 9. Agent operating rules

- Work in the order of this guide. Finish and verify one stage before starting the next.
- At the end of each stage, run the stage's acceptance checks and report results before merging.
- Run `mvn -q test` and `git status` before every commit.
- Do not push anything that fails to compile.
- Never print, log, or commit credentials.
- If a requirement is truly ambiguous and not covered in §4, pick the safest reasonable option, record it in the README's design decisions, and continue. Do not stall.
- If `git push` fails for auth reasons, stop and tell the user exactly what failed. Do not try to work around authentication.
