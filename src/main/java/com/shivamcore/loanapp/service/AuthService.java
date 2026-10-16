package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.exception.AuthenticationException;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.util.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service managing user authentication, login lockout, and credential updates.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates a user by email or username and plaintext password.
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

        if (userOpt.isEmpty()) {
            logger.warn("Authentication failed: user not found for identifier: {}", cleanIdentifier);
            throw new AuthenticationException("User not found.");
        }

        User user = userOpt.get();

        // Verify password
        boolean passwordMatches = PasswordHasher.verify(password, user.getPasswordHash());

        if (!passwordMatches) {
            logger.warn("Authentication failed: wrong password for user id: {}", user.getId());
            throw new AuthenticationException("Invalid credentials.");
        }

        // Decision 5: If password is correct but account is INACTIVE, inform account is locked
        if (user.getStatus() == Status.INACTIVE) {
            logger.warn("Authentication rejected: account is INACTIVE/locked for user id: {}", user.getId());
            throw new AuthenticationException("Your account is locked or inactive. Please contact support.");
        }

        logger.info("Authentication successful for user id: {}, username: {}", user.getId(), user.getUsername());
        return user;
    }

    public UserDao getUserDao() {
        return userDao;
    }
}
