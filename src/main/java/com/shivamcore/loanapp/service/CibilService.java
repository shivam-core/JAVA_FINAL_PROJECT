package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service managing CIBIL score simulation and retrieval.
 */
public class CibilService {
    private static final Logger logger = LoggerFactory.getLogger(CibilService.class);

    private final UserDao userDao;

    public CibilService() {
        this(null);
    }

    public CibilService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Generates a simulated CIBIL score between 600 and 900 (inclusive).
     */
    public int generate() {
        return ThreadLocalRandom.current().nextInt(600, 901);
    }

    /**
     * Retrieves an existing CIBIL score for the PAN card if available, or generates and records a new one.
     */
    public int getOrGenerateCibil(String pan) {
        Validator.validatePan(pan);
        String cleanPan = pan.trim().toUpperCase();

        if (userDao != null) {
            Optional<User> existingUser = userDao.findByPan(cleanPan);
            if (existingUser.isPresent() && existingUser.get().getCibilScore() != null) {
                logger.info("Found existing CIBIL score {} for PAN {}", existingUser.get().getCibilScore(), cleanPan);
                return existingUser.get().getCibilScore();
            }
        }

        int score = generate();
        logger.info("Generated new simulated CIBIL score {} for PAN {}", score, cleanPan);
        return score;
    }
}
