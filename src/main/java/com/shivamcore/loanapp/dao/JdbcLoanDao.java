package com.shivamcore.loanapp.dao;

import com.shivamcore.loanapp.config.DatabaseManager;
import com.shivamcore.loanapp.exception.DataAccessException;
import com.shivamcore.loanapp.model.LoanApplication;
import com.shivamcore.loanapp.model.LoanStatus;
import com.shivamcore.loanapp.model.LoanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of LoanDao using MySQL and PreparedStatement exclusively.
 */
public class JdbcLoanDao implements LoanDao {
    private static final Logger logger = LoggerFactory.getLogger(JdbcLoanDao.class);
    private final DatabaseManager databaseManager;

    public JdbcLoanDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public LoanApplication save(LoanApplication loan) {
        String sql = "INSERT INTO loan_applications (user_id, loan_amount, loan_type, loan_duration_months, interest_rate, annual_income, purpose, status, application_date, rejection_reason) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = loan.getApplicationDate() != null ? loan.getApplicationDate() : LocalDateTime.now();
            ps.setLong(1, loan.getUserId());
            ps.setBigDecimal(2, loan.getLoanAmount());
            ps.setString(3, loan.getLoanType().name());
            ps.setInt(4, loan.getLoanDurationMonths());
            ps.setBigDecimal(5, loan.getInterestRate());
            ps.setBigDecimal(6, loan.getAnnualIncome());
            ps.setString(7, loan.getPurpose());
            ps.setString(8, loan.getStatus() != null ? loan.getStatus().name() : LoanStatus.PENDING.name());
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.setString(10, loan.getRejectionReason());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    loan.setId(rs.getLong(1));
                }
            }
            loan.setApplicationDate(now);
            return loan;
        } catch (SQLException e) {
            logger.error("Failed to save loan application for user id: {}", loan.getUserId(), e);
            throw new DataAccessException("Failed to submit loan application", e);
        }
    }

    @Override
    public Optional<LoanApplication> findById(Long id) {
        String sql = "SELECT l.id, l.user_id, l.loan_amount, l.loan_type, l.loan_duration_months, l.interest_rate, l.annual_income, l.purpose, l.status, l.application_date, l.rejection_reason, l.emi_amount, l.total_interest, l.total_payable, " +
                "u.fname, u.lname, u.email, u.mobile " +
                "FROM loan_applications l JOIN users u ON l.user_id = u.id WHERE l.id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToLoan(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find loan by id: {}", id, e);
            throw new DataAccessException("Failed to query loan by id", e);
        }
    }

    @Override
    public List<LoanApplication> findByUserId(Long userId) {
        String sql = "SELECT l.id, l.user_id, l.loan_amount, l.loan_type, l.loan_duration_months, l.interest_rate, l.annual_income, l.purpose, l.status, l.application_date, l.rejection_reason, l.emi_amount, l.total_interest, l.total_payable, " +
                "u.fname, u.lname, u.email, u.mobile " +
                "FROM loan_applications l JOIN users u ON l.user_id = u.id WHERE l.user_id = ? ORDER BY l.id DESC";

        List<LoanApplication> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToLoan(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            logger.error("Failed to find loans by user id: {}", userId, e);
            throw new DataAccessException("Failed to query user loans", e);
        }
    }

    @Override
    public List<LoanApplication> findAll() {
        String sql = "SELECT l.id, l.user_id, l.loan_amount, l.loan_type, l.loan_duration_months, l.interest_rate, l.annual_income, l.purpose, l.status, l.application_date, l.rejection_reason, l.emi_amount, l.total_interest, l.total_payable, " +
                "u.fname, u.lname, u.email, u.mobile " +
                "FROM loan_applications l JOIN users u ON l.user_id = u.id ORDER BY l.id DESC";

        List<LoanApplication> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToLoan(rs));
            }
            return list;
        } catch (SQLException e) {
            logger.error("Failed to query all loan applications", e);
            throw new DataAccessException("Failed to retrieve loan applications", e);
        }
    }

    @Override
    public List<LoanApplication> findByStatus(LoanStatus status) {
        String sql = "SELECT l.id, l.user_id, l.loan_amount, l.loan_type, l.loan_duration_months, l.interest_rate, l.annual_income, l.purpose, l.status, l.application_date, l.rejection_reason, l.emi_amount, l.total_interest, l.total_payable, " +
                "u.fname, u.lname, u.email, u.mobile " +
                "FROM loan_applications l JOIN users u ON l.user_id = u.id WHERE l.status = ? ORDER BY l.id DESC";

        List<LoanApplication> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToLoan(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            logger.error("Failed to query loans by status: {}", status, e);
            throw new DataAccessException("Failed to query loans by status", e);
        }
    }

    @Override
    public void updateStatus(Long id, LoanStatus newStatus, String rejectionReason) {
        String sql = "UPDATE loan_applications SET status = ?, rejection_reason = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus.name());
            ps.setString(2, rejectionReason);
            ps.setLong(3, id);
            ps.executeUpdate();
            logger.info("Updated status for loan id {} to {}", id, newStatus);
        } catch (SQLException e) {
            logger.error("Failed to update status for loan id: {}", id, e);
            throw new DataAccessException("Failed to update loan status", e);
        }
    }

    @Override
    public void approveLoan(Long id, BigDecimal emiAmount, BigDecimal totalInterest, BigDecimal totalPayable) {
        String sql = "UPDATE loan_applications SET status = 'APPROVED', rejection_reason = NULL, emi_amount = ?, total_interest = ?, total_payable = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, emiAmount);
                ps.setBigDecimal(2, totalInterest);
                ps.setBigDecimal(3, totalPayable);
                ps.setLong(4, id);
                ps.executeUpdate();
                conn.commit();
                logger.info("Successfully approved loan id {} with EMI {} in single transaction", id, emiAmount);
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Failed to approve loan id: {}, rolling back transaction", id, e);
                throw new DataAccessException("Failed to approve loan application", e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            logger.error("Database connection error during loan approval: {}", id, e);
            throw new DataAccessException("Database error during loan approval", e);
        }
    }

    private LoanApplication mapRowToLoan(ResultSet rs) throws SQLException {
        LoanApplication loan = new LoanApplication();
        loan.setId(rs.getLong("id"));
        loan.setUserId(rs.getLong("user_id"));
        loan.setLoanAmount(rs.getBigDecimal("loan_amount"));
        loan.setLoanType(LoanType.valueOf(rs.getString("loan_type")));
        loan.setLoanDurationMonths(rs.getInt("loan_duration_months"));
        loan.setInterestRate(rs.getBigDecimal("interest_rate"));
        loan.setAnnualIncome(rs.getBigDecimal("annual_income"));
        loan.setPurpose(rs.getString("purpose"));
        loan.setStatus(LoanStatus.valueOf(rs.getString("status")));

        Timestamp ts = rs.getTimestamp("application_date");
        if (ts != null) {
            loan.setApplicationDate(ts.toLocalDateTime());
        }
        loan.setRejectionReason(rs.getString("rejection_reason"));

        try {
            loan.setEmiAmount(rs.getBigDecimal("emi_amount"));
            loan.setTotalInterest(rs.getBigDecimal("total_interest"));
            loan.setTotalPayable(rs.getBigDecimal("total_payable"));
        } catch (SQLException ignored) {
        }

        try {
            String fname = rs.getString("fname");
            String lname = rs.getString("lname");
            loan.setApplicantName(((fname != null ? fname : "") + " " + (lname != null ? lname : "")).trim());
            loan.setApplicantEmail(rs.getString("email"));
            loan.setApplicantMobile(rs.getString("mobile"));
        } catch (SQLException ignored) {
        }

        return loan;
    }
}
