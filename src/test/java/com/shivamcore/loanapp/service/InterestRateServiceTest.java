package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.LoanType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class InterestRateServiceTest {

    private final InterestRateService service = new InterestRateService();

    @Test
    @DisplayName("Boundary at 600 returns maximum interest rate for each loan type")
    void testBoundaryAt600ReturnsMax() {
        assertEquals(new BigDecimal("8.50"), service.calculate(LoanType.HOME, 600));
        assertEquals(new BigDecimal("12.00"), service.calculate(LoanType.PERSONAL, 600));
        assertEquals(new BigDecimal("6.00"), service.calculate(LoanType.EDUCATION, 600));
        assertEquals(new BigDecimal("9.00"), service.calculate(LoanType.VEHICLE, 600));
    }

    @Test
    @DisplayName("Boundary at 900 returns minimum interest rate for each loan type")
    void testBoundaryAt900ReturnsMin() {
        assertEquals(new BigDecimal("6.50"), service.calculate(LoanType.HOME, 900));
        assertEquals(new BigDecimal("9.00"), service.calculate(LoanType.PERSONAL, 900));
        assertEquals(new BigDecimal("4.00"), service.calculate(LoanType.EDUCATION, 900));
        assertEquals(new BigDecimal("7.00"), service.calculate(LoanType.VEHICLE, 900));
    }

    @Test
    @DisplayName("Midpoint at 750 returns average of range")
    void testMidpointAt750() {
        assertEquals(new BigDecimal("7.50"), service.calculate(LoanType.HOME, 750));
        assertEquals(new BigDecimal("10.50"), service.calculate(LoanType.PERSONAL, 750));
        assertEquals(new BigDecimal("5.00"), service.calculate(LoanType.EDUCATION, 750));
        assertEquals(new BigDecimal("8.00"), service.calculate(LoanType.VEHICLE, 750));
    }

    @Test
    @DisplayName("CIBIL score 720 produces 7.70% for Home Loan per linear interpolation")
    void testIllustrative720HomeLoan() {
        assertEquals(new BigDecimal("7.70"), service.calculate(LoanType.HOME, 720));
    }

    @ParameterizedTest
    @CsvSource({
            "HOME, 600, 8.50",
            "HOME, 750, 7.50",
            "HOME, 900, 6.50",
            "PERSONAL, 600, 12.00",
            "PERSONAL, 750, 10.50",
            "PERSONAL, 900, 9.00",
            "EDUCATION, 600, 6.00",
            "EDUCATION, 750, 5.00",
            "EDUCATION, 900, 4.00",
            "VEHICLE, 600, 9.00",
            "VEHICLE, 750, 8.00",
            "VEHICLE, 900, 7.00"
    })
    void testMatrix(LoanType type, int cibil, String expectedRate) {
        assertEquals(new BigDecimal(expectedRate), service.calculate(type, cibil));
    }

    @Test
    @DisplayName("Invalid CIBIL scores and null loan types throw ValidationException")
    void testValidation() {
        assertThrows(ValidationException.class, () -> service.calculate(null, 700));
        assertThrows(ValidationException.class, () -> service.calculate(LoanType.HOME, 299));
        assertThrows(ValidationException.class, () -> service.calculate(LoanType.HOME, 901));
    }

    @Test
    @DisplayName("Format summary produces exact specification output string")
    void testFormatSummary() {
        String summary = service.formatSummary("ABCDE1234F", 720, LoanType.HOME, new BigDecimal("7.70"));
        assertEquals("For PAN ABCDE1234F, with a CIBIL score of 720, the interest rate for Home Loan is 7.70%.", summary);
    }
}
