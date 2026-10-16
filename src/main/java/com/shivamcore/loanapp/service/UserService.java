package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.exception.DuplicateResourceException;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.util.PasswordHasher;
import com.shivamcore.loanapp.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Service managing user registration and profile management.
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Registers a new user after validating all fields and checking uniqueness.
     *
     * @return Persisted User instance with generated ID
     */
    public User register(String fname, String lname, String mobile, String email,
                         String location, String username, String password) {
        logger.info("Initiating registration for username: {}, email: {}", username, email);

        // 1. Validation
        Validator.validateUserRegistration(fname, lname, mobile, email, location, username, password);

        // 2. Pre-check uniqueness in service layer
        String cleanUsername = username.trim();
        String cleanEmail = email.trim();

        if (userDao.existsByUsername(cleanUsername)) {
            logger.warn("Registration rejected: username '{}' already exists", cleanUsername);
            throw new DuplicateResourceException("Username '" + cleanUsername + "' is already taken.");
        }
        if (userDao.existsByEmail(cleanEmail)) {
            logger.warn("Registration rejected: email '{}' already registered", cleanEmail);
            throw new DuplicateResourceException("Email '" + cleanEmail + "' is already registered.");
        }

        // 3. Password hashing
        String passwordHash = PasswordHasher.hash(password);

        // 4. Construct domain model
        User newUser = new User();
        newUser.setFname(fname.trim());
        newUser.setLname(lname.trim());
        newUser.setMobile(mobile.trim());
        newUser.setEmail(cleanEmail);
        newUser.setLocation(location.trim());
        newUser.setUsername(cleanUsername);
        newUser.setPasswordHash(passwordHash);
        newUser.setStatus(Status.ACTIVE);
        newUser.setFailedAttempts(0);
        newUser.setLastModifiedDate(LocalDateTime.now());

        // 5. Persist via DAO (DB constraints act as backstop)
        User savedUser = userDao.save(newUser);
        logger.info("Successfully registered user id: {}, username: {}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    public UserDao getUserDao() {
        return userDao;
    }
}
