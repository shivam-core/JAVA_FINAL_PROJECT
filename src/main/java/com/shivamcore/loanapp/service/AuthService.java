package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.exception.AuthenticationException;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.util.PasswordHasher;
import com.shivamcore.loanapp.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service managing user authentication, login lockout, and credential updates.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates a user by email or username and plaintext password.
     * Enforces the 3-attempt lockout rule with atomic database updates.
     *
     * @param identifier Email or username
     * @param password   Plaintext password
     * @return Authenticated User object
     */
    public User login(String identifier, String password) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new ValidationException("Username or Email cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password cannot be empty.");
        }

        String cleanIdentifier = identifier.trim();
        Optional<User> userOpt = userDao.findByIdentifier(cleanIdentifier);

        // Unknown user: error message without modifying any counter (Decision 4)
        if (userOpt.isEmpty()) {
            logger.warn("Authentication failed: user not found for identifier: {}", cleanIdentifier);
            throw new AuthenticationException("User not found.");
        }

        User user = userOpt.get();

        // Verify password
        boolean passwordMatches = PasswordHasher.verify(password, user.getPasswordHash());

        if (!passwordMatches) {
            // Check if already inactive
            if (user.getStatus() == Status.INACTIVE) {
                logger.warn("Authentication failed with invalid credentials for inactive user id: {}", user.getId());
                throw new AuthenticationException("Invalid credentials.");
            }

            // Increment failed attempts atomically
            int newAttempts = userDao.incrementFailedAttempts(user.getId());
            logger.warn("User id {} failed login attempt #{}", user.getId(), newAttempts);

            if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                userDao.lockAccount(user.getId());
                logger.warn("User id {} has been locked due to {} consecutive failed attempts", user.getId(), newAttempts);
                throw new AuthenticationException("Account locked due to 3 consecutive failed login attempts. Please contact support.");
            } else {
                int remaining = MAX_FAILED_ATTEMPTS - newAttempts;
                throw new AuthenticationException("Invalid credentials. You have " + remaining + " attempt(s) remaining before your account is locked.");
            }
        }

        // Decision 5: If password is correct but account is INACTIVE, inform account is locked
        if (user.getStatus() == Status.INACTIVE) {
            logger.warn("Authentication rejected: password correct but account is INACTIVE for user id: {}", user.getId());
            throw new AuthenticationException("Your account is locked or inactive. Please contact support.");
        }

        // Password matches and account is ACTIVE -> reset counter if needed
        if (user.getFailedAttempts() > 0) {
            userDao.resetFailedAttempts(user.getId());
            user.setFailedAttempts(0);
            logger.info("Reset failed_attempts to 0 after successful login for user id: {}", user.getId());
        }

        logger.info("Authentication successful for user id: {}, username: {}", user.getId(), user.getUsername());
        return user;
    }

    /**
     * Resets a user's password using username or email as identifier.
     * Blocked for INACTIVE accounts to prevent bypassing lockout (Decision 6).
     *
     * @param identifier  Username or email
     * @param newPassword New plaintext password satisfying password policy
     */
    public void resetPassword(String identifier, String newPassword) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new ValidationException("Username or Email cannot be empty.");
        }

        String cleanIdentifier = identifier.trim();
        Optional<User> userOpt = userDao.findByIdentifier(cleanIdentifier);

        if (userOpt.isEmpty()) {
            logger.warn("Password reset failed: user not found for identifier: {}", cleanIdentifier);
            throw new AuthenticationException("User not found.");
        }

        User user = userOpt.get();

        // Decision 6: Block reset for INACTIVE/locked accounts
        if (user.getStatus() == Status.INACTIVE) {
            logger.warn("Password reset rejected: account is INACTIVE/locked for user id: {}", user.getId());
            throw new AuthenticationException("Password reset is not permitted for locked or inactive accounts. Please contact support.");
        }

        // Validate new password policy
        Validator.validatePassword(newPassword);

        // Hash and persist
        String newHash = PasswordHasher.hash(newPassword);
        userDao.updatePassword(user.getId(), newHash);
        logger.info("Password successfully reset for user id: {}, username: {}", user.getId(), user.getUsername());
    }

    public UserDao getUserDao() {
        return userDao;
    }
}
