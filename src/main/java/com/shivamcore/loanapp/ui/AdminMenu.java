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
            System.out.println("7. Loan Management");
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
                case "7":
                    handleLoanManagement();
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

    private void handleLoanManagement() {
        boolean inLoanMenu = true;
        while (inLoanMenu) {
            System.out.println("\n--- Loan Management ---");
            System.out.println("1. List All Loan Applications");
            System.out.println("2. Filter Loans by Status");
            System.out.println("3. View Loan Details");
            System.out.println("4. Approve Loan");
            System.out.println("5. Reject Loan");
            System.out.println("0. Back to Admin Dashboard");
            System.out.print("Please enter your choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    displayLoans(adminService.getAllLoans(currentAdmin));
                    break;
                case "2":
                    handleFilterLoansByStatus();
                    break;
                case "3":
                    handleViewLoanDetails();
                    break;
                case "4":
                    handleApproveLoan();
                    break;
                case "5":
                    handleRejectLoan();
                    break;
                case "0":
                    inLoanMenu = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please choose an option from the menu.");
            }
        }
    }

    private void handleFilterLoansByStatus() {
        System.out.println("\nSelect Status to Filter:");
        System.out.println("1. PENDING");
        System.out.println("2. APPROVED");
        System.out.println("3. REJECTED");
        System.out.println("4. NEEDS_MORE_INFO");
        System.out.print("Enter choice (1-4): ");
        String ch = scanner.nextLine().trim();
        com.shivamcore.loanapp.model.LoanStatus status;
        switch (ch) {
            case "1": status = com.shivamcore.loanapp.model.LoanStatus.PENDING; break;
            case "2": status = com.shivamcore.loanapp.model.LoanStatus.APPROVED; break;
            case "3": status = com.shivamcore.loanapp.model.LoanStatus.REJECTED; break;
            case "4": status = com.shivamcore.loanapp.model.LoanStatus.NEEDS_MORE_INFO; break;
            default:
                System.out.println("Invalid status choice.");
                return;
        }
        displayLoans(adminService.getLoansByStatus(currentAdmin, status));
    }

    private void displayLoans(List<com.shivamcore.loanapp.model.LoanApplication> loans) {
        if (loans == null || loans.isEmpty()) {
            System.out.println("No loan applications found.");
            return;
        }
        System.out.println("\nLoan Applications (" + loans.size() + " total):");
        System.out.printf("%-6s | %-20s | %-12s | %-14s | %-16s | %-12s\n",
                "ID", "Applicant Name", "Type", "Amount", "Applied Date", "Status");
        System.out.println("------------------------------------------------------------------------------------------");
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (com.shivamcore.loanapp.model.LoanApplication l : loans) {
            String appDate = l.getApplicationDate() != null ? l.getApplicationDate().format(fmt) : "N/A";
            String name = l.getApplicantName() != null ? l.getApplicantName() : "User #" + l.getUserId();
            System.out.printf("%-6d | %-20s | %-12s | ₹%-13.2f | %-16s | %-12s\n",
                    l.getId(), name, l.getLoanType(), l.getLoanAmount(), appDate, l.getStatus());
        }
    }

    private void handleViewLoanDetails() {
        System.out.print("\nEnter Loan Application ID to view details: ");
        String idStr = scanner.nextLine().trim();
        try {
            Long loanId = Long.parseLong(idStr);
            Optional<com.shivamcore.loanapp.model.LoanApplication> opt = adminService.getLoanById(currentAdmin, loanId);
            if (opt.isEmpty()) {
                System.out.println("Loan application not found with ID: " + loanId);
                return;
            }
            com.shivamcore.loanapp.model.LoanApplication l = opt.get();
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            System.out.println("\n--- Loan Application Details ---");
            System.out.println("Loan ID:          " + l.getId());
            System.out.println("Applicant Name:   " + (l.getApplicantName() != null ? l.getApplicantName() : "User #" + l.getUserId()));
            System.out.println("Applicant Email:  " + (l.getApplicantEmail() != null ? l.getApplicantEmail() : "N/A"));
            System.out.println("Applicant Mobile: " + (l.getApplicantMobile() != null ? l.getApplicantMobile() : "N/A"));
            System.out.printf("Loan Amount:      ₹%.2f\n", l.getLoanAmount());
            System.out.println("Loan Type:        " + l.getLoanType().getDisplayName());
            System.out.println("Duration:         " + l.getLoanDurationMonths() + " months");
            System.out.printf("Annual Income:    ₹%.2f\n", l.getAnnualIncome());
            System.out.printf("Interest Rate:    %.2f%%\n", l.getInterestRate());
            System.out.println("Purpose:          " + l.getPurpose());
            System.out.println("Application Date: " + (l.getApplicationDate() != null ? l.getApplicationDate().format(fmt) : "N/A"));
            System.out.println("Status:           " + l.getStatus());
            if (l.getRejectionReason() != null && !l.getRejectionReason().isEmpty()) {
                System.out.println("Rejection Reason: " + l.getRejectionReason());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid Loan ID format.");
        }
    }

    private void handleApproveLoan() {
        System.out.print("\nEnter Loan Application ID to approve: ");
        String idStr = scanner.nextLine().trim();
        Long loanId;
        try {
            loanId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid Loan ID format.");
            return;
        }

        Optional<com.shivamcore.loanapp.model.LoanApplication> opt = adminService.getLoanById(currentAdmin, loanId);
        if (opt.isEmpty()) {
            System.out.println("Loan application not found with ID: " + loanId);
            return;
        }
        com.shivamcore.loanapp.model.LoanApplication l = opt.get();
        System.out.printf("Loan #%d for %s (Amount: ₹%.2f, Status: %s)\n",
                l.getId(), (l.getApplicantName() != null ? l.getApplicantName() : "User #" + l.getUserId()), l.getLoanAmount(), l.getStatus());

        System.out.print("Are you sure you want to approve this loan? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if ("yes".equalsIgnoreCase(confirm)) {
            try {
                adminService.approveLoan(currentAdmin, loanId);
                System.out.println(">> Loan application #" + loanId + " successfully APPROVED and notification email dispatched.");
            } catch (ValidationException e) {
                System.out.println("Approval failed: " + e.getMessage());
            }
        } else {
            System.out.println("Approval cancelled.");
        }
    }

    private void handleRejectLoan() {
        System.out.print("\nEnter Loan Application ID to reject: ");
        String idStr = scanner.nextLine().trim();
        Long loanId;
        try {
            loanId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid Loan ID format.");
            return;
        }

        Optional<com.shivamcore.loanapp.model.LoanApplication> opt = adminService.getLoanById(currentAdmin, loanId);
        if (opt.isEmpty()) {
            System.out.println("Loan application not found with ID: " + loanId);
            return;
        }
        com.shivamcore.loanapp.model.LoanApplication l = opt.get();
        System.out.printf("Loan #%d for %s (Amount: ₹%.2f, Status: %s)\n",
                l.getId(), (l.getApplicantName() != null ? l.getApplicantName() : "User #" + l.getUserId()), l.getLoanAmount(), l.getStatus());

        System.out.print("Enter reason for rejection: ");
        String reason = scanner.nextLine().trim();
        if (reason.isEmpty()) {
            System.out.println("Rejection reason cannot be empty. Rejection aborted.");
            return;
        }

        System.out.print("Are you sure you want to reject this loan? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if ("yes".equalsIgnoreCase(confirm)) {
            try {
                adminService.rejectLoan(currentAdmin, loanId, reason);
                System.out.println(">> Loan application #" + loanId + " successfully REJECTED and notification email dispatched.");
            } catch (ValidationException e) {
                System.out.println("Rejection failed: " + e.getMessage());
            }
        } else {
            System.out.println("Rejection cancelled.");
        }
    }
}
