package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.LoanDao;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.LoanApplication;
import com.shivamcore.loanapp.model.LoanStatus;
import com.shivamcore.loanapp.model.LoanType;
import com.shivamcore.loanapp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing user loan applications and validation.
 */
public class LoanService {
    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    private final LoanDao loanDao;

    public LoanService(LoanDao loanDao) {
        this.loanDao = loanDao;
    }

    public LoanApplication applyForLoan(User user, BigDecimal amount, LoanType type,
                                        int durationMonths, BigDecimal annualIncome,
                                        String purpose, BigDecimal interestRate) {
        if (user == null || user.getId() == null) {
            throw new ValidationException("User must be logged in to apply for a loan.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Loan amount must be a positive number.");
        }
        if (type == null) {
            throw new ValidationException("Invalid loan type selected.");
        }
        if (durationMonths <= 0) {
            throw new ValidationException("Loan duration must be greater than 0.");
        }
        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Annual income must be a positive number.");
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new ValidationException("Loan purpose is mandatory.");
        }
        if (purpose.trim().length() > 255) {
            throw new ValidationException("Loan purpose cannot exceed 255 characters.");
        }

        BigDecimal rate = interestRate != null ? interestRate : type.getDefaultInterestRate();
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Interest rate must be greater than 0.");
        }

        LoanApplication loan = new LoanApplication();
        loan.setUserId(user.getId());
        loan.setLoanAmount(amount);
        loan.setLoanType(type);
        loan.setLoanDurationMonths(durationMonths);
        loan.setAnnualIncome(annualIncome);
        loan.setPurpose(purpose.trim());
        loan.setInterestRate(rate);
        loan.setStatus(LoanStatus.PENDING);
        loan.setApplicationDate(LocalDateTime.now());

        LoanApplication saved = loanDao.save(loan);
        logger.info("User id {} successfully submitted loan application id: {}", user.getId(), saved.getId());
        return saved;
    }

    public List<LoanApplication> getLoansByUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ValidationException("User must be logged in.");
        }
        return loanDao.findByUserId(user.getId());
    }

    public LoanDao getLoanDao() {
        return loanDao;
    }
}
