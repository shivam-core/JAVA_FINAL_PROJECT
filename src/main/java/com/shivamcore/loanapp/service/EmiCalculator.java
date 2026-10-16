package com.shivamcore.loanapp.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Pure, static-free calculation engine for EMI, total interest, and total payable.
 * Follows standard amortization formula using MathContext.DECIMAL128.
 * Total payable is derived from rounded EMI to maintain internal consistency across columns.
 */
public class EmiCalculator {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");

    public static class Result {
        private final BigDecimal emi;
        private final BigDecimal totalInterest;
        private final BigDecimal totalPayable;

        public Result(BigDecimal emi, BigDecimal totalInterest, BigDecimal totalPayable) {
            this.emi = emi;
            this.totalInterest = totalInterest;
            this.totalPayable = totalPayable;
        }

        public BigDecimal getEmi() {
            return emi;
        }

        public BigDecimal getTotalInterest() {
            return totalInterest;
        }

        public BigDecimal getTotalPayable() {
            return totalPayable;
        }

        @Override
        public String toString() {
            return "Result{emi=" + emi + ", totalInterest=" + totalInterest + ", totalPayable=" + totalPayable + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Result result = (Result) o;
            return Objects.equals(emi, result.emi) &&
                    Objects.equals(totalInterest, result.totalInterest) &&
                    Objects.equals(totalPayable, result.totalPayable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(emi, totalInterest, totalPayable);
        }
    }

    public Result calculate(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be greater than zero");
        }
        if (months <= 0) {
            throw new IllegalArgumentException("Months must be greater than zero");
        }
        if (annualRatePercent == null || annualRatePercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }

        BigDecimal emi;
        if (annualRatePercent.compareTo(BigDecimal.ZERO) == 0) {
            emi = principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        } else {
            // R = annualRate / 12 / 100
            BigDecimal r = annualRatePercent.divide(TWELVE_HUNDRED, MC);
            // (1+R)^N
            BigDecimal onePlusR = BigDecimal.ONE.add(r, MC);
            BigDecimal onePlusRPowN = onePlusR.pow(months, MC);

            // EMI = P * R * (1+R)^N / ((1+R)^N - 1)
            BigDecimal numerator = principal.multiply(r, MC).multiply(onePlusRPowN, MC);
            BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, MC);
            BigDecimal rawEmi = numerator.divide(denominator, MC);

            emi = rawEmi.setScale(2, RoundingMode.HALF_UP);
        }

        // totalPayable = EMI * N
        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP);
        // totalInterest = totalPayable - P
        BigDecimal totalInterest = totalPayable.subtract(principal).setScale(2, RoundingMode.HALF_UP);

        return new Result(emi, totalInterest, totalPayable);
    }
}
