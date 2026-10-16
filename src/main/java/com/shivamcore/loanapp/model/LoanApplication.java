package com.shivamcore.loanapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a loan application submitted by a user.
 */
public class LoanApplication {
    private Long id;
    private Long userId;
    private BigDecimal loanAmount;
    private LoanType loanType;
    private int loanDurationMonths;
    private BigDecimal interestRate;
    private BigDecimal annualIncome;
    private String purpose;
    private LoanStatus status;
    private LocalDateTime applicationDate;
    private String rejectionReason;

    // Optional snapshot fields populated on join/display
    private String applicantName;
    private String applicantEmail;
    private String applicantMobile;

    // Stage 5 calculation fields
    private BigDecimal emiAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalPayable;

    public LoanApplication() {
        this.status = LoanStatus.PENDING;
        this.applicationDate = LocalDateTime.now();
    }

    public LoanApplication(Long id, Long userId, BigDecimal loanAmount, LoanType loanType,
                           int loanDurationMonths, BigDecimal interestRate, BigDecimal annualIncome,
                           String purpose, LoanStatus status, LocalDateTime applicationDate,
                           String rejectionReason) {
        this.id = id;
        this.userId = userId;
        this.loanAmount = loanAmount;
        this.loanType = loanType;
        this.loanDurationMonths = loanDurationMonths;
        this.interestRate = interestRate;
        this.annualIncome = annualIncome;
        this.purpose = purpose;
        this.status = status != null ? status : LoanStatus.PENDING;
        this.applicationDate = applicationDate != null ? applicationDate : LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public int getLoanDurationMonths() {
        return loanDurationMonths;
    }

    public void setLoanDurationMonths(int loanDurationMonths) {
        this.loanDurationMonths = loanDurationMonths;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRemarks() {
        return rejectionReason;
    }

    public void setRemarks(String remarks) {
        this.rejectionReason = remarks;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getApplicantMobile() {
        return applicantMobile;
    }

    public void setApplicantMobile(String applicantMobile) {
        this.applicantMobile = applicantMobile;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public void setEmiAmount(BigDecimal emiAmount) {
        this.emiAmount = emiAmount;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getTotalPayable() {
        return totalPayable;
    }

    public void setTotalPayable(BigDecimal totalPayable) {
        this.totalPayable = totalPayable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanApplication that = (LoanApplication) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
