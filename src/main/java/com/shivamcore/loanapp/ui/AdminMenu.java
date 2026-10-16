package com.shivamcore.loanapp.ui;

import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.service.AdminService;
import com.shivamcore.loanapp.util.ConsoleInput;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console-based administrative dashboard menu.
 */
public class AdminMenu {

    private final Scanner scanner;
    private final AdminService adminService;
    private final User currentAdmin;

    public AdminMenu(Scanner scanner, AdminService adminService, User currentAdmin) {
        this.scanner = scanner;
        this.adminService = adminService;
        this.currentAdmin = currentAdmin;
    }

    public void display() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n========================================");
            System.out.println("            ADMIN DASHBOARD             ");
            System.out.println("  Logged in as: " + currentAdmin.getUsername());
            System.out.println("========================================");
            System.out.println("1. Search Users");
            System.out.println("2. View User Details");
            System.out.println("3. Edit User Information");
            System.out.println("4. Activate User");
            System.out.println("5. Deactivate User");
            System.out.println("6. Delete User");
            System.out.println("0. Logout");
            System.out.print("Please enter your choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleSearchUsers();
                    break;
                case "2":
                    handleViewUserDetails();
                    break;
                case "3":
                    handleEditUser();
                    break;
                case "4":
                    handleActivateUser();
                    break;
                case "5":
                    handleDeactivateUser();
                    break;
                case "6":
                    handleDeleteUser();
                    break;
                case "0":
                    System.out.println("\nLogging out from Admin Dashboard. Goodbye!");
                    inMenu = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose an option from the menu.");
            }
        }
    }

    private void handleSearchUsers() {
        System.out.println("\n--- Search Users ---");
        System.out.println("Select search criterion:");
        System.out.println("1. First Name");
        System.out.println("2. Last Name");
        System.out.println("3. Email");
        System.out.println("4. Mobile Number");
        System.out.println("5. Username");
        System.out.println("6. Status (ACTIVE / INACTIVE)");
        System.out.print("Choice: ");

        String fieldChoice = scanner.nextLine().trim();
        String field;
        switch (fieldChoice) {
            case "1": field = "fname"; break;
            case "2": field = "lname"; break;
            case "3": field = "email"; break;
            case "4": field = "mobile"; break;
            case "5": field = "username"; break;
            case "6": field = "status"; break;
            default:
                System.out.println("Invalid search criterion.");
                return;
        }

        System.out.print("Enter search term: ");
        String term = scanner.nextLine().trim();
        List<User> results = adminService.searchUsers(currentAdmin, field, term);

        if (results.isEmpty()) {
            System.out.println("No users found matching the given criteria.");
            return;
        }

        System.out.println("\nSearch Results (" + results.size() + " found):");
        printUserTable(results);
    }

    private void handleViewUserDetails() {
        System.out.print("\nEnter User ID to view details: ");
        String idStr = scanner.nextLine().trim();
        try {
            Long userId = Long.parseLong(idStr);
            Optional<User> userOpt = adminService.getUserById(currentAdmin, userId);
            if (userOpt.isEmpty()) {
                System.out.println("User not found with ID: " + userId);
                return;
            }
            User u = userOpt.get();
            System.out.println("\n--- User Details ---");
            System.out.println("ID:               " + u.getId());
            System.out.println("Full Name:        " + u.getFullName());
            System.out.println("Email:            " + u.getEmail());
            System.out.println("Mobile:           " + u.getMobile());
            System.out.println("Location:         " + u.getLocation());
            System.out.println("Username:         " + u.getUsername());
            System.out.println("Role:             " + u.getRole());
            System.out.println("Status:           " + u.getStatus());
            System.out.println("Failed Attempts:  " + u.getFailedAttempts());
            System.out.println("Last Modified:    " + u.getLastModifiedDate());
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID format.");
        }
    }

    private void handleEditUser() {
        System.out.print("\nEnter User ID to edit: ");
        String idStr = scanner.nextLine().trim();
        Long userId;
        try {
            userId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID format.");
            return;
        }

        Optional<User> userOpt = adminService.getUserById(currentAdmin, userId);
        if (userOpt.isEmpty()) {
            System.out.println("User not found with ID: " + userId);
            return;
        }
        User existing = userOpt.get();
        System.out.println("Editing user: " + existing.getUsername() + " (" + existing.getFullName() + ")");

        String fname = promptOptionalField("Enter new First Name [" + existing.getFname() + "]: ",
                val -> val.isEmpty() || com.shivamcore.loanapp.util.Validator.isValidName(val),
                "First name must contain only letters.");
        if (fname.isEmpty()) fname = existing.getFname();

        String lname = promptOptionalField("Enter new Last Name [" + existing.getLname() + "]: ",
                val -> val.isEmpty() || com.shivamcore.loanapp.util.Validator.isValidName(val),
                "Last name must contain only letters.");
        if (lname.isEmpty()) lname = existing.getLname();

        String mobile = promptOptionalField("Enter new 10-digit Mobile [" + existing.getMobile() + "]: ",
                val -> val.isEmpty() || com.shivamcore.loanapp.util.Validator.isValidMobile(val),
                "Mobile number must be exactly 10 digits.");
        if (mobile.isEmpty()) mobile = existing.getMobile();

        String email = promptOptionalField("Enter new Email [" + existing.getEmail() + "]: ",
                val -> val.isEmpty() || com.shivamcore.loanapp.util.Validator.isValidEmail(val),
                "Invalid email address format.");
        if (email.isEmpty()) email = existing.getEmail();

        System.out.print("Enter new Location [" + existing.getLocation() + "]: ");
        String location = scanner.nextLine().trim();
        if (location.isEmpty()) location = existing.getLocation();

        try {
            adminService.editUser(currentAdmin, userId, fname, lname, mobile, email, location);
            System.out.println("User details successfully updated and notification email sent.");
        } catch (ValidationException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    private void handleActivateUser() {
        System.out.print("\nEnter User ID to activate: ");
        String idStr = scanner.nextLine().trim();
        try {
            Long userId = Long.parseLong(idStr);
            adminService.activateUser(currentAdmin, userId);
            System.out.println("User ID " + userId + " successfully activated and failed attempts reset to 0.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID format.");
        } catch (ValidationException e) {
            System.out.println("Activation failed: " + e.getMessage());
        }
    }

    private void handleDeactivateUser() {
        System.out.print("\nEnter User ID to deactivate: ");
        String idStr = scanner.nextLine().trim();
        try {
            Long userId = Long.parseLong(idStr);
            adminService.deactivateUser(currentAdmin, userId);
            System.out.println("User ID " + userId + " successfully deactivated.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID format.");
        } catch (ValidationException e) {
            System.out.println("Deactivation failed: " + e.getMessage());
        }
    }

    private void handleDeleteUser() {
        System.out.print("\nEnter User ID to delete: ");
        String idStr = scanner.nextLine().trim();
        Long userId;
        try {
            userId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID format.");
            return;
        }

        Optional<User> userOpt = adminService.getUserById(currentAdmin, userId);
        if (userOpt.isEmpty()) {
            System.out.println("User not found with ID: " + userId);
            return;
        }
        User u = userOpt.get();

        System.out.print("WARNING: Deleting user " + u.getUsername() + " will cascade to all related records.\nAre you sure you want to delete this user? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if ("yes".equalsIgnoreCase(confirm)) {
            try {
                adminService.deleteUser(currentAdmin, userId);
                System.out.println("User " + u.getUsername() + " was deleted and logged to audit.");
            } catch (ValidationException e) {
                System.out.println("Deletion failed: " + e.getMessage());
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private String promptOptionalField(String prompt, java.util.function.Predicate<String> validator, String errorMsg) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty() || validator.test(line)) {
                return line;
            }
            System.out.println(errorMsg);
        }
    }

    private void printUserTable(List<User> users) {
        System.out.println(String.format("%-5s | %-20s | %-25s | %-12s | %-15s | %-10s | %-10s",
                "ID", "Name", "Email", "Mobile", "Username", "Role", "Status"));
        System.out.println("---------------------------------------------------------------------------------------------------------");
        for (User u : users) {
            System.out.println(String.format("%-5d | %-20s | %-25s | %-12s | %-15s | %-10s | %-10s",
                    u.getId(), u.getFullName(), u.getEmail(), u.getMobile(), u.getUsername(), u.getRole(), u.getStatus()));
        }
    }
}
