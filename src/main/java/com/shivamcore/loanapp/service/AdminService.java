package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.AuditLogDao;
import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.exception.AccessDeniedException;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.AuditLog;
import com.shivamcore.loanapp.model.Role;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service providing administrative functions guarded by role-based access control.
 */
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("audit");

    private final UserDao userDao;
    private final AuditLogDao auditLogDao;
    private final EmailService emailService;
    private final com.shivamcore.loanapp.dao.LoanDao loanDao;

    public AdminService(UserDao userDao, AuditLogDao auditLogDao, EmailService emailService) {
        this(userDao, auditLogDao, emailService, null);
    }

    public AdminService(UserDao userDao, AuditLogDao auditLogDao, EmailService emailService, com.shivamcore.loanapp.dao.LoanDao loanDao) {
        this.userDao = userDao;
        this.auditLogDao = auditLogDao;
        this.emailService = emailService;
        this.loanDao = loanDao;
    }

    public void requireAdmin(User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.ROLE_ADMIN) {
            logger.warn("Access denied: User {} lacks ROLE_ADMIN privilege",
                    currentUser != null ? currentUser.getUsername() : "ANONYMOUS");
            throw new AccessDeniedException("Access denied: Administrative privileges required.");
        }
    }

    public List<User> searchUsers(User currentUser, String field, String query) {
        requireAdmin(currentUser);
        return userDao.search(field, query);
    }

    public List<User> getAllUsers(User currentUser) {
        requireAdmin(currentUser);
        return userDao.findAll();
    }

    public Optional<User> getUserById(User currentUser, Long userId) {
        requireAdmin(currentUser);
        return userDao.findById(userId);
    }

    public User editUser(User currentUser, Long targetUserId, String fname, String lname,
                         String mobile, String email, String location) {
        requireAdmin(currentUser);

        User target = userDao.findById(targetUserId)
                .orElseThrow(() -> new ValidationException("User not found with id: " + targetUserId));

        Validator.validateName("First Name", fname);
        Validator.validateName("Last Name", lname);
        Validator.validateMobile(mobile);
        Validator.validateEmail(email);
        Validator.validateLocation(location);

        // If email changed, check uniqueness
        if (!target.getEmail().equalsIgnoreCase(email.trim()) && userDao.existsByEmail(email.trim())) {
            throw new ValidationException("Email is already registered by another account.");
        }

        target.setFname(fname.trim());
        target.setLname(lname.trim());
        target.setMobile(mobile.trim());
        target.setEmail(email.trim());
        target.setLocation(location.trim());
        target.setLastModifiedDate(LocalDateTime.now());

        userDao.update(target);
        logger.info("Admin {} updated details for user id: {}", currentUser.getUsername(), targetUserId);

        emailService.sendEditEmail(target.getEmail(), target.getFname());
        return target;
    }

    public void activateUser(User currentUser, Long targetUserId) {
        requireAdmin(currentUser);

        User target = userDao.findById(targetUserId)
                .orElseThrow(() -> new ValidationException("User not found with id: " + targetUserId));

        target.setStatus(Status.ACTIVE);
        // Decision 3: Admin Activate must also reset failed_attempts = 0
        target.setFailedAttempts(0);
        target.setLastModifiedDate(LocalDateTime.now());

        userDao.update(target);
        userDao.resetFailedAttempts(targetUserId);
        logger.info("Admin {} activated user id: {}", currentUser.getUsername(), targetUserId);

        emailService.sendActivationEmail(target.getEmail(), target.getFname());
    }

    public void deactivateUser(User currentUser, Long targetUserId) {
        requireAdmin(currentUser);

        User target = userDao.findById(targetUserId)
                .orElseThrow(() -> new ValidationException("User not found with id: " + targetUserId));

        // Decision: Admin cannot deactivate their own account or another admin
        if (target.getId().equals(currentUser.getId())) {
            throw new ValidationException("Admins cannot deactivate their own account.");
        }
        if (target.isAdmin()) {
            throw new ValidationException("Admins cannot deactivate another admin account.");
        }

        target.setStatus(Status.INACTIVE);
        target.setLastModifiedDate(LocalDateTime.now());

        userDao.update(target);
        logger.info("Admin {} deactivated user id: {}", currentUser.getUsername(), targetUserId);

        emailService.sendDeactivationEmail(target.getEmail(), target.getFname());
    }

    public void deleteUser(User currentUser, Long targetUserId) {
        requireAdmin(currentUser);

        User target = userDao.findById(targetUserId)
                .orElseThrow(() -> new ValidationException("User not found with id: " + targetUserId));

        // Decision: Admin cannot delete their own account or another admin
        if (target.getId().equals(currentUser.getId())) {
            throw new ValidationException("Admins cannot delete their own account.");
        }
        if (target.isAdmin()) {
            throw new ValidationException("Admins cannot delete another admin account.");
        }

        // Snapshot details before deletion
        String targetUsername = target.getUsername();
        String targetEmail = target.getEmail();
        String targetFname = target.getFname();

        // 1. Write audit log to database and file logger
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("DELETE_USER");
        auditLog.setPerformedBy(currentUser.getUsername());
        auditLog.setTargetUserId(targetUserId);
        auditLog.setTargetUsername(targetUsername);
        auditLog.setDetails("Admin " + currentUser.getUsername() + " deleted user " + targetUsername + " (email: " + targetEmail + ")");
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogDao.save(auditLog);

        auditLogger.info("AUDIT: Action=DELETE_USER | PerformedBy={} | TargetUserId={} | TargetUsername={} | Details={}",
                currentUser.getUsername(), targetUserId, targetUsername, auditLog.getDetails());

        // 2. Delete user from database
        userDao.deleteById(targetUserId);
        logger.info("Admin {} deleted user id: {}, username: {}", currentUser.getUsername(), targetUserId, targetUsername);

        // 3. Send final notification email
        emailService.sendDeletionEmail(targetEmail, targetFname);
    }

    public List<com.shivamcore.loanapp.model.LoanApplication> getAllLoans(User currentUser) {
        requireAdmin(currentUser);
        if (loanDao == null) {
            throw new IllegalStateException("LoanDao is not initialized.");
        }
        return loanDao.findAll();
    }

    public List<com.shivamcore.loanapp.model.LoanApplication> getLoansByStatus(User currentUser, com.shivamcore.loanapp.model.LoanStatus status) {
        requireAdmin(currentUser);
        if (loanDao == null) {
            throw new IllegalStateException("LoanDao is not initialized.");
        }
        return loanDao.findByStatus(status);
    }

    public Optional<com.shivamcore.loanapp.model.LoanApplication> getLoanById(User currentUser, Long loanId) {
        requireAdmin(currentUser);
        if (loanDao == null) {
            throw new IllegalStateException("LoanDao is not initialized.");
        }
        return loanDao.findById(loanId);
    }
}
