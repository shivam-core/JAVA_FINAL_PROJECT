package com.shivamcore.loanapp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EmiCalculatorTest {

    private final EmiCalculator calculator = new EmiCalculator();

    @Test
    @DisplayName("Reference check from spec: P=500000, rate=8.5%, N=60 -> EMI=10258.27")
    void testReferenceCase() {
        BigDecimal principal = new BigDecimal("500000.00");
        BigDecimal rate = new BigDecimal("8.50");
        int months = 60;

        EmiCalculator.Result result = calculator.calculate(principal, rate, months);

        assertNotNull(result);
        assertEquals(new BigDecimal("10258.27"), result.getEmi(), "EMI must match reference check 10258.27");

        // Mutual consistency check: totalPayable = EMI * N
        BigDecimal expectedPayable = new BigDecimal("10258.27").multiply(BigDecimal.valueOf(60));
        assertEquals(expectedPayable, result.getTotalPayable());

        // totalInterest = totalPayable - P
        BigDecimal expectedInterest = expectedPayable.subtract(principal);
        assertEquals(expectedInterest, result.getTotalInterest());
    }

    @Test
    @DisplayName("Zero interest rate distributes principal evenly across months")
    void testZeroInterestRate() {
        BigDecimal principal = new BigDecimal("120000.00");
        BigDecimal rate = BigDecimal.ZERO;
        int months = 12;

        EmiCalculator.Result result = calculator.calculate(principal, rate, months);

        assertEquals(new BigDecimal("10000.00"), result.getEmi());
        assertEquals(new BigDecimal("120000.00"), result.getTotalPayable());
        assertEquals(new BigDecimal("0.00"), result.getTotalInterest());
    }

    @Test
    @DisplayName("Input validations reject non-positive principals, negative rates, and non-positive months")
    void testInputValidations() {
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(BigDecimal.ZERO, new BigDecimal("8.00"), 12));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(new BigDecimal("-1000"), new BigDecimal("8.00"), 12));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(new BigDecimal("50000"), new BigDecimal("-1.00"), 12));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(new BigDecimal("50000"), new BigDecimal("8.00"), 0));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(new BigDecimal("50000"), new BigDecimal("8.00"), -5));
    }
}
