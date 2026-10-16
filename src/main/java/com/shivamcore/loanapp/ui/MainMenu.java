package com.shivamcore.loanapp.ui;

import com.shivamcore.loanapp.exception.AuthenticationException;
import com.shivamcore.loanapp.exception.DuplicateResourceException;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.service.AuthService;
import com.shivamcore.loanapp.service.UserService;
import com.shivamcore.loanapp.util.ConsoleInput;
import com.shivamcore.loanapp.util.Validator;

/**
 * Main logged-out console menu for registration, login, and password reset.
 */
public class MainMenu {

    private final UserService userService;
    private final AuthService authService;
    private final com.shivamcore.loanapp.service.AdminService adminService;
    private final com.shivamcore.loanapp.service.LoanService loanService;
    private final com.shivamcore.loanapp.service.CibilService cibilService;
    private final com.shivamcore.loanapp.service.InterestRateService interestRateService;
    private final ConsoleInput input;
    private final UserMenu userMenu;

    public MainMenu(UserService userService, AuthService authService, ConsoleInput input) {
        this(userService, authService, null, null, null, null, input);
    }

    public MainMenu(UserService userService, AuthService authService, com.shivamcore.loanapp.service.AdminService adminService, ConsoleInput input) {
        this(userService, authService, adminService, null, null, null, input);
    }

    public MainMenu(UserService userService, AuthService authService, com.shivamcore.loanapp.service.AdminService adminService, com.shivamcore.loanapp.service.LoanService loanService, ConsoleInput input) {
        this(userService, authService, adminService, loanService, null, null, input);
    }

    public MainMenu(UserService userService, AuthService authService, com.shivamcore.loanapp.service.AdminService adminService, com.shivamcore.loanapp.service.LoanService loanService, com.shivamcore.loanapp.service.CibilService cibilService, com.shivamcore.loanapp.service.InterestRateService interestRateService, ConsoleInput input) {
        this.userService = userService;
        this.authService = authService;
        this.adminService = adminService;
        this.loanService = loanService;
        this.cibilService = cibilService != null ? cibilService : new com.shivamcore.loanapp.service.CibilService(userService != null ? userService.getUserDao() : null);
        this.interestRateService = interestRateService != null ? interestRateService : new com.shivamcore.loanapp.service.InterestRateService();
        this.input = input;
        this.userMenu = new UserMenu(loanService, input);
    }

    /**
     * Starts the main menu loop.
     */
    public void run() {
        boolean running = true;
        while (running) {
            displayMenu();
            int choice = input.readInt("Enter choice: ");

            switch (choice) {
                case 1:
                    handleRegister();
                    break;
                case 2:
                    handleLogin();
                    break;
                case 3:
                    handleForgotPassword();
                    break;
                case 4:
                    handleInterestRateCalculator();
                    break;
                case 0:
                    System.out.println("Thank you for using the Loan Application System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose an option from the menu.");
            }
        }
    }

    private void displayMenu() {
        System.out.println();
        System.out.println("==========================================");
        System.out.println("      LOAN APPLICATION SYSTEM");
        System.out.println("==========================================");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Forgot Password");
        System.out.println("4. Interest Rate Calculator");
        System.out.println("0. Exit");
        System.out.println("==========================================");
    }

    /**
     * Collects registration details, re-prompting ONLY the field that fails validation.
     */
    private void handleRegister() {
        System.out.println();
        System.out.println("--- User Registration ---");

        // 1. First Name
        String fname = promptValidatedField(
                "Enter First Name: ",
                val -> Validator.validateName("First Name", val)
        );

        // 2. Last Name
        String lname = promptValidatedField(
                "Enter Last Name: ",
                val -> Validator.validateName("Last Name", val)
        );

        // 3. Mobile Number
        String mobile = promptValidatedField(
                "Enter Mobile Number (10 digits): ",
                Validator::validateMobile
        );

        // 4. Email
        String email = promptValidatedField(
                "Enter Email Address: ",
                val -> {
                    Validator.validateEmail(val);
                    if (userService.getUserDao().existsByEmail(val.trim())) {
                        throw new DuplicateResourceException("Email '" + val.trim() + "' is already registered.");
                    }
                }
        );

        // 5. Location
        String location = promptValidatedField(
                "Enter Location: ",
                Validator::validateLocation
        );

        // 6. Username
        String username = promptValidatedField(
                "Enter Username: ",
                val -> {
                    Validator.validateUsername(val);
                    if (userService.getUserDao().existsByUsername(val.trim())) {
                        throw new DuplicateResourceException("Username '" + val.trim() + "' is already taken.");
                    }
                }
        );

        // 7. Password
        String password = promptValidatedPassword(
                "Enter Password (min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special): ",
                Validator::validatePassword
        );

        try {
            User registered = userService.register(fname, lname, mobile, email, location, username, password);
            System.out.println();
            System.out.println(">> Registration Successful!");
            System.out.println("Welcome, " + registered.getFullName().trim() + "! You can now log in.");
        } catch (DuplicateResourceException | ValidationException e) {
            System.out.println("Registration failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred during registration. Please try again.");
        }
    }

    private void handleLogin() {
        System.out.println();
        System.out.println("--- User Login ---");
        String identifier = input.readLine("Enter Username or Email: ").trim();
        String password = input.readPassword("Enter Password: ");

        try {
            User user = authService.login(identifier, password);
            System.out.println();
            System.out.println("Welcome " + user.getFname() + " " + user.getLname());
            if (user.isAdmin() && adminService != null) {
                new AdminMenu(input.getScanner(), adminService, user).display();
            } else {
                userMenu.show(user);
            }
        } catch (ValidationException | AuthenticationException e) {
            System.out.println("Login Failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occurred during login. Please try again.");
        }
    }

    private void handleForgotPassword() {
        System.out.println();
        System.out.println("--- Forgot Password ---");
        String identifier = input.readLine("Enter your registered Username or Email: ").trim();

        if (identifier.isEmpty()) {
            System.out.println("Identifier cannot be empty.");
            return;
        }

        // Re-prompt password until it matches policy
        String newPassword = promptValidatedPassword(
                "Enter New Password (min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special): ",
                Validator::validatePassword
        );

        try {
            authService.resetPassword(identifier, newPassword);
            System.out.println(">> Password reset successful! You may now log in with your new password.");
        } catch (AuthenticationException | ValidationException e) {
            System.out.println("Password Reset Failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occurred during password reset. Please try again.");
        }
    }

    private String promptValidatedField(String prompt, ValidationRule rule) {
        while (true) {
            String value = input.readLine(prompt).trim();
            try {
                rule.validate(value);
                return value;
            } catch (ValidationException | DuplicateResourceException e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println("Please try again.");
            }
        }
    }

    private String promptValidatedPassword(String prompt, ValidationRule rule) {
        while (true) {
            String value = input.readPassword(prompt);
            try {
                rule.validate(value);
                return value;
            } catch (ValidationException e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println("Please try again.");
            }
        }
    }

    private void handleInterestRateCalculator() {
        System.out.println();
        System.out.println("--- Interest Rate Calculator ---");

        // 1. PAN Card
        String pan = promptValidatedPan();

        // 2. Loan Type
        com.shivamcore.loanapp.model.LoanType type = promptLoanType();

        // 3. CIBIL Score
        int cibil = cibilService.getOrGenerateCibil(pan);

        // 4. Rate calculation
        java.math.BigDecimal rate = interestRateService.calculate(type, cibil);

        // 5. Output line format
        System.out.println();
        System.out.println(interestRateService.formatSummary(pan, cibil, type, rate));
    }

    private String promptValidatedPan() {
        while (true) {
            String pan = input.readLine("Enter PAN Card Number (e.g. ABCDE1234F): ").trim().toUpperCase();
            if (Validator.isValidPan(pan)) {
                return pan;
            }
            System.out.println("Invalid PAN format. Please enter a valid PAN.");
        }
    }

    private com.shivamcore.loanapp.model.LoanType promptLoanType() {
        while (true) {
            System.out.println("Select Loan Type:");
            System.out.println("1. Home Loan");
            System.out.println("2. Personal Loan");
            System.out.println("3. Education Loan");
            System.out.println("4. Vehicle / Car Loan");
            String choice = input.readLine("Enter choice (1-4 or name): ").trim();
            if (choice.equals("1") || choice.equalsIgnoreCase("home")) return com.shivamcore.loanapp.model.LoanType.HOME;
            if (choice.equals("2") || choice.equalsIgnoreCase("personal")) return com.shivamcore.loanapp.model.LoanType.PERSONAL;
            if (choice.equals("3") || choice.equalsIgnoreCase("education")) return com.shivamcore.loanapp.model.LoanType.EDUCATION;
            if (choice.equals("4") || choice.equalsIgnoreCase("vehicle") || choice.equalsIgnoreCase("car")) return com.shivamcore.loanapp.model.LoanType.VEHICLE;
            try {
                return com.shivamcore.loanapp.model.LoanType.fromString(choice);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid loan type selected.");
            }
        }
    }

    @FunctionalInterface
    private interface ValidationRule {
        void validate(String value);
    }
}
