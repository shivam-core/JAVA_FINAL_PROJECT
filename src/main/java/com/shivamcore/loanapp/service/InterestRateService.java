package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.LoanType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service calculating loan interest rates based on loan type and CIBIL score.
 * Formula: rate = max - ((cibil - 600) / 300.0) * (max - min) rounded to 2 decimal places.
 */
public class InterestRateService {

    public static class Range {
        private final double min;
        private final double max;

        public Range(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }
    }

    public static Range getRange(LoanType type) {
        if (type == null) {
            throw new ValidationException("Invalid loan type selected.");
        }
        switch (type) {
            case HOME:
                return new Range(6.5, 8.5);
            case PERSONAL:
                return new Range(9.0, 12.0);
            case EDUCATION:
                return new Range(4.0, 6.0);
            case VEHICLE:
                return new Range(7.0, 9.0);
            default:
                throw new ValidationException("Invalid loan type selected.");
        }
    }

    public BigDecimal calculate(LoanType type, int cibilScore) {
        if (type == null) {
            throw new ValidationException("Invalid loan type selected.");
        }
        if (cibilScore < 300 || cibilScore > 900) {
            throw new ValidationException("CIBIL score must be between 300 and 900.");
        }

        Range range = getRange(type);
        // Clamp score to [600, 900] for linear interpolation range
        int effectiveScore = Math.min(900, Math.max(600, cibilScore));
        double fraction = (effectiveScore - 600) / 300.0;
        double rawRate = range.getMax() - (fraction * (range.getMax() - range.getMin()));

        return BigDecimal.valueOf(rawRate).setScale(2, RoundingMode.HALF_UP);
    }

    public String formatSummary(String pan, int cibilScore, LoanType type, BigDecimal rate) {
        return String.format("For PAN %s, with a CIBIL score of %d, the interest rate for %s is %.2f%%.",
                pan != null ? pan.trim().toUpperCase() : "",
                cibilScore,
                type != null ? type.getDisplayName() : "Loan",
                rate != null ? rate : BigDecimal.ZERO);
    }
}
