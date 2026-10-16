package com.shivamcore.loanapp.dao;

import com.shivamcore.loanapp.model.LoanApplication;
import com.shivamcore.loanapp.model.LoanStatus;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Loan Applications.
 */
public interface LoanDao {
    LoanApplication save(LoanApplication loan);
    Optional<LoanApplication> findById(Long id);
    List<LoanApplication> findByUserId(Long userId);
    List<LoanApplication> findAll();
    List<LoanApplication> findByStatus(LoanStatus status);
    void updateStatus(Long id, LoanStatus newStatus, String rejectionReason);
}
