package com.shivamcore.loanapp.ui;

import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.LoanApplication;
import com.shivamcore.loanapp.model.LoanType;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.service.LoanService;
import com.shivamcore.loanapp.util.ConsoleInput;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Console menu presented to authenticated users.
 */
public class UserMenu {

    private final LoanService loanService;
    private final ConsoleInput input;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public UserMenu(ConsoleInput input) {
        this(null, input);
    }

    public UserMenu(LoanService loanService, ConsoleInput input) {
        this.loanService = loanService;
        this.input = input;
    }

    /**
     * Runs the post-login user menu loop.
     *
     * @param user Currently authenticated user
     */
    public void show(User user) {
        System.out.println();
        System.out.println("==========================================");
        System.out.println("Welcome, " + user.getFname() + " " + user.getLname());
        System.out.println("==========================================");

        boolean inSession = true;
        while (inSession) {
            System.out.println();
            System.out.println("--- User Menu ---");
            System.out.println("1. Apply for Loan");
            System.out.println("2. My Loan Applications");
            System.out.println("0. Logout");
            int choice = input.readInt("Enter choice: ");

            switch (choice) {
                case 1:
                    handleApplyLoan(user);
                    break;
                case 2:
                    handleMyLoans(user);
                    break;
                case 0:
                    System.out.println("Logging out... You have been successfully logged out.");
                    inSession = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    public void handleApplyLoan(User user) {
        if (loanService == null) {
            System.out.println("Loan service is currently unavailable.");
            return;
        }

        System.out.println();
        System.out.println("--- Apply for Loan ---");

        // 1. Loan Type
        LoanType type = promptLoanType();

        // Interest rate preview
        BigDecimal rate = type.getDefaultInterestRate();
        System.out.printf("Applicable Interest Rate: %.2f%%\n", rate);

        // 2. Loan Amount
        BigDecimal amount = promptPositiveDecimal("Enter Loan Amount: ");

        // 3. Duration: Unit prompt (1=months, 2=years), then value
        int durationMonths = promptDurationMonths();

        // 4. Annual Income
        BigDecimal annualIncome = promptPositiveDecimal("Enter Annual Income: ");

        // 5. Purpose
        String purpose = promptPurpose();

        // Confirmation
        System.out.println();
        System.out.println("--- Loan Application Summary ---");
        System.out.println("Loan Type:        " + type.getDisplayName());
        System.out.printf("Loan Amount:      ₹%.2f\n", amount);
        System.out.println("Duration:         " + durationMonths + " months");
        System.out.printf("Annual Income:    ₹%.2f\n", annualIncome);
        System.out.printf("Interest Rate:    %.2f%%\n", rate);
        System.out.println("Purpose:          " + purpose);
        System.out.println("--------------------------------");

        String confirm = input.readLine("Submit loan application? (y/n): ").trim();
        if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
            System.out.println("Loan application cancelled.");
            return;
        }

        try {
            LoanApplication loan = loanService.applyForLoan(user, amount, type, durationMonths, annualIncome, purpose, rate);
            System.out.println();
            System.out.println(">> Loan application submitted successfully!");
            System.out.println("Your Loan Application ID is: " + loan.getId());
            System.out.println("Current Status: " + loan.getStatus());
        } catch (ValidationException e) {
            System.out.println("Application failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occurred while submitting your loan application: " + e.getMessage());
        }
    }

    public void handleMyLoans(User user) {
        if (loanService == null) {
            System.out.println("Loan service is currently unavailable.");
            return;
        }

        System.out.println();
        System.out.println("--- My Loan Applications ---");
        try {
            List<LoanApplication> loans = loanService.getLoansByUser(user);
            if (loans.isEmpty()) {
                System.out.println("You have no loan applications.");
                return;
            }

            System.out.printf("%-6s | %-12s | %-14s | %-10s | %-8s | %-10s | %-16s | %-10s\n",
                    "ID", "Type", "Amount", "Duration", "Rate", "Status", "Date", "Remarks");
            System.out.println("--------------------------------------------------------------------------------------------------");
            for (LoanApplication l : loans) {
                String dateStr = l.getApplicationDate() != null ? l.getApplicationDate().format(DATE_FMT) : "N/A";
                String remarks = l.getRemarks() != null ? l.getRemarks() : "";
                System.out.printf("%-6d | %-12s | ₹%-13.2f | %-7d mo | %-7.2f%% | %-10s | %-16s | %-10s\n",
                        l.getId(),
                        l.getLoanType(),
                        l.getLoanAmount(),
                        l.getLoanDurationMonths(),
                        l.getInterestRate(),
                        l.getStatus(),
                        dateStr,
                        remarks);
            }
        } catch (Exception e) {
            System.out.println("Error fetching loan applications: " + e.getMessage());
        }
    }

    private LoanType promptLoanType() {
        while (true) {
            System.out.println("Select Loan Type:");
            System.out.println("1. Home Loan (6.5% - 8.5%)");
            System.out.println("2. Personal Loan (9.0% - 12.0%)");
            System.out.println("3. Education Loan (4.0% - 6.0%)");
            System.out.println("4. Vehicle / Car Loan (7.0% - 9.0%)");
            String in = input.readLine("Enter choice (1-4 or name): ").trim();
            if (in.equalsIgnoreCase("1") || in.equalsIgnoreCase("home")) {
                return LoanType.HOME;
            } else if (in.equalsIgnoreCase("2") || in.equalsIgnoreCase("personal")) {
                return LoanType.PERSONAL;
            } else if (in.equalsIgnoreCase("3") || in.equalsIgnoreCase("education")) {
                return LoanType.EDUCATION;
            } else if (in.equalsIgnoreCase("4") || in.equalsIgnoreCase("vehicle") || in.equalsIgnoreCase("car")) {
                return LoanType.VEHICLE;
            } else {
                try {
                    return LoanType.fromString(in);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid loan type selected. Please choose from 1-4 or enter type name.");
                }
            }
        }
    }

    private BigDecimal promptPositiveDecimal(String prompt) {
        while (true) {
            String val = input.readLine(prompt).trim();
            try {
                BigDecimal d = new BigDecimal(val);
                if (d.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Value must be greater than zero. Please try again.");
                    continue;
                }
                return d;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Please enter a valid decimal number.");
            }
        }
    }

    private int promptDurationMonths() {
        while (true) {
            System.out.println("Duration unit: 1 = Months, 2 = Years");
            int unit = input.readInt("Select unit (1 or 2): ");
            if (unit != 1 && unit != 2) {
                System.out.println("Invalid choice. Please select 1 for Months or 2 for Years.");
                continue;
            }
            int val = input.readInt(unit == 1 ? "Enter duration in months: " : "Enter duration in years: ");
            if (val <= 0) {
                System.out.println("Duration must be greater than zero.");
                continue;
            }
            return unit == 2 ? val * 12 : val;
        }
    }

    private String promptPurpose() {
        while (true) {
            String p = input.readLine("Enter Purpose of Loan: ").trim();
            if (p.isEmpty()) {
                System.out.println("Loan purpose is mandatory.");
                continue;
            }
            if (p.length() > 255) {
                System.out.println("Purpose cannot exceed 255 characters. (Current length: " + p.length() + ")");
                continue;
            }
            return p;
        }
    }
}
