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
    private final ConsoleInput input;
    private final UserMenu userMenu;

    public MainMenu(UserService userService, AuthService authService, ConsoleInput input) {
        this(userService, authService, null, null, input);
    }

    public MainMenu(UserService userService, AuthService authService, com.shivamcore.loanapp.service.AdminService adminService, ConsoleInput input) {
        this(userService, authService, adminService, null, input);
    }

    public MainMenu(UserService userService, AuthService authService, com.shivamcore.loanapp.service.AdminService adminService, com.shivamcore.loanapp.service.LoanService loanService, ConsoleInput input) {
        this.userService = userService;
        this.authService = authService;
        this.adminService = adminService;
        this.loanService = loanService;
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

    @FunctionalInterface
    private interface ValidationRule {
        void validate(String value);
    }
}
