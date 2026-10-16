package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.config.ConfigLoader;
import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.model.Role;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.util.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Seeds a predefined admin on application startup if no admin exists (Decision 2).
 */
public class AdminSeeder {
    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserDao userDao;
    private final ConfigLoader config;

    public AdminSeeder(UserDao userDao, ConfigLoader config) {
        this.userDao = userDao;
        this.config = config;
    }

    public void seedAdminIfAbsent() {
        if (userDao.existsByRole(Role.ROLE_ADMIN)) {
            logger.info("Admin user already present in the database.");
            return;
        }

        String username = config.getOrDefault("admin.username", "admin");
        String password = config.getOrDefault("admin.password", "Admin@1234");
        String email = config.getOrDefault("admin.email", "admin@loanapp.com");
        String fname = config.getOrDefault("admin.fname", "System");
        String lname = config.getOrDefault("admin.lname", "Admin");
        String mobile = config.getOrDefault("admin.mobile", "9999999999");
        String location = config.getOrDefault("admin.location", "Headquarters");

        User admin = new User();
        admin.setFname(fname);
        admin.setLname(lname);
        admin.setMobile(mobile);
        admin.setEmail(email);
        admin.setLocation(location);
        admin.setUsername(username);
        admin.setPasswordHash(PasswordHasher.hash(password));
        admin.setStatus(Status.ACTIVE);
        admin.setRole(Role.ROLE_ADMIN);
        admin.setFailedAttempts(0);
        admin.setLastModifiedDate(LocalDateTime.now());

        userDao.save(admin);
        logger.info("Successfully seeded default administrator account: {}", username);
    }
}
