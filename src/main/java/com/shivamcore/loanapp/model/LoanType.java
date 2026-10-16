package com.shivamcore.loanapp.model;

import java.math.BigDecimal;

/**
 * Types of loans offered with their respective baseline interest rates.
 */
public enum LoanType {
    HOME(new BigDecimal("7.50")),
    PERSONAL(new BigDecimal("10.50")),
    EDUCATION(new BigDecimal("5.00")),
    VEHICLE(new BigDecimal("8.00"));

    private final BigDecimal defaultInterestRate;

    LoanType(BigDecimal defaultInterestRate) {
        this.defaultInterestRate = defaultInterestRate;
    }

    public BigDecimal getDefaultInterestRate() {
        return defaultInterestRate;
    }

    public String getDisplayName() {
        switch (this) {
            case HOME: return "Home Loan";
            case PERSONAL: return "Personal Loan";
            case EDUCATION: return "Education Loan";
            case VEHICLE: return "Vehicle Loan";
            default: return name();
        }
    }

    /**
     * Parses user input, mapping 'CAR' to VEHICLE per Decision 1.
     */
    public static LoanType fromString(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan type cannot be empty.");
        }
        String clean = input.trim().toUpperCase();
        if ("CAR".equals(clean)) {
            return VEHICLE;
        }
        return LoanType.valueOf(clean);
    }
}
