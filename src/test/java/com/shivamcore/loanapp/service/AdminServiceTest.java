package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.config.ConfigLoader;
import com.shivamcore.loanapp.dao.AuditLogDao;
import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.exception.AccessDeniedException;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.AuditLog;
import com.shivamcore.loanapp.model.Role;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {

    private InMemoryUserDao userDao;
    private InMemoryAuditLogDao auditLogDao;
    private AdminService adminService;
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        auditLogDao = new InMemoryAuditLogDao();
        Properties props = new Properties();
        props.setProperty("mail.enabled", "false");
        EmailService emailService = new EmailService(new ConfigLoader(props, null));
        adminService = new AdminService(userDao, auditLogDao, emailService);

        adminUser = new User();
        adminUser.setFname("Admin");
        adminUser.setLname("User");
        adminUser.setEmail("admin@test.com");
        adminUser.setMobile("9999999999");
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ROLE_ADMIN);
        adminUser.setStatus(Status.ACTIVE);
        userDao.save(adminUser);

        regularUser = new User();
        regularUser.setFname("John");
        regularUser.setLname("Doe");
        regularUser.setEmail("john@test.com");
        regularUser.setMobile("9876543210");
        regularUser.setUsername("johndoe");
        regularUser.setRole(Role.ROLE_USER);
        regularUser.setStatus(Status.ACTIVE);
        userDao.save(regularUser);
    }

    @Test
    @DisplayName("Non-admin user cannot access admin operations")
    void testRoleGuardRejectsNonAdmin() {
        assertThrows(AccessDeniedException.class, () ->
                adminService.getAllUsers(regularUser));
        assertThrows(AccessDeniedException.class, () ->
                adminService.activateUser(regularUser, regularUser.getId()));
        assertThrows(AccessDeniedException.class, () ->
                adminService.deleteUser(regularUser, regularUser.getId()));
    }

    @Test
    @DisplayName("Admin can search users by first name and status")
    void testSearchUsers() {
        List<User> found = adminService.searchUsers(adminUser, "fname", "John");
        assertEquals(1, found.size());
        assertEquals("johndoe", found.get(0).getUsername());

        List<User> activeUsers = adminService.searchUsers(adminUser, "status", "ACTIVE");
        assertEquals(2, activeUsers.size());
    }

    @Test
    @DisplayName("Admin can edit user details with validation")
    void testEditUser() {
        User edited = adminService.editUser(adminUser, regularUser.getId(),
                "Johnny", "Doe", "9876543211", "johnny@test.com", "Mumbai");
        assertEquals("Johnny", edited.getFname());
        assertEquals("9876543211", edited.getMobile());
        assertEquals("johnny@test.com", edited.getEmail());
    }

    @Test
    @DisplayName("Admin cannot deactivate or delete own account")
    void testSelfProtection() {
        assertThrows(ValidationException.class, () ->
                adminService.deactivateUser(adminUser, adminUser.getId()));
        assertThrows(ValidationException.class, () ->
                adminService.deleteUser(adminUser, adminUser.getId()));
    }

    @Test
    @DisplayName("Admin deactivates and activates user with reset failed attempts")
    void testDeactivateAndActivate() {
        regularUser.setFailedAttempts(2);
        userDao.update(regularUser);

        adminService.deactivateUser(adminUser, regularUser.getId());
        User deactivated = userDao.findById(regularUser.getId()).orElseThrow();
        assertEquals(Status.INACTIVE, deactivated.getStatus());

        adminService.activateUser(adminUser, regularUser.getId());
        User activated = userDao.findById(regularUser.getId()).orElseThrow();
        assertEquals(Status.ACTIVE, activated.getStatus());
        assertEquals(0, activated.getFailedAttempts(), "Failed attempts must reset to 0 upon activation (Decision 3)");
    }

    @Test
    @DisplayName("Admin deletes user and writes audit log to DB")
    void testDeleteUserWithAuditLog() {
        adminService.deleteUser(adminUser, regularUser.getId());
        assertFalse(userDao.findById(regularUser.getId()).isPresent());

        List<AuditLog> recentLogs = auditLogDao.findRecent(10);
        assertEquals(1, recentLogs.size());
        assertEquals("DELETE_USER", recentLogs.get(0).getAction());
        assertEquals("admin", recentLogs.get(0).getPerformedBy());
        assertEquals("johndoe", recentLogs.get(0).getTargetUsername());
    }

    private static class InMemoryAuditLogDao implements AuditLogDao {
        private final List<AuditLog> logs = new ArrayList<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public AuditLog save(AuditLog auditLog) {
            if (auditLog.getId() == null) {
                auditLog.setId(idGen.getAndIncrement());
            }
            logs.add(auditLog);
            return auditLog;
        }

        @Override
        public List<AuditLog> findRecent(int limit) {
            return new ArrayList<>(logs);
        }
    }

    private static class InMemoryUserDao implements UserDao {
        private final Map<Long, User> map = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public User save(User user) {
            if (user.getId() == null) {
                user.setId(idGen.getAndIncrement());
            }
            map.put(user.getId(), user);
            return user;
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(map.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return map.values().stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return map.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public Optional<User> findByIdentifier(String identifier) {
            return findByUsername(identifier);
        }

        @Override
        public boolean existsByUsername(String username) {
            return map.values().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
        }

        @Override
        public boolean existsByEmail(String email) {
            return map.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }

        @Override
        public boolean existsByRole(Role role) {
            return map.values().stream().anyMatch(u -> u.getRole() == role);
        }

        @Override
        public int incrementFailedAttempts(Long userId) {
            return 0;
        }

        @Override
        public void lockAccount(Long userId) {
            if (map.containsKey(userId)) map.get(userId).setStatus(Status.INACTIVE);
        }

        @Override
        public void resetFailedAttempts(Long userId) {
            if (map.containsKey(userId)) map.get(userId).setFailedAttempts(0);
        }

        @Override
        public void updatePassword(Long userId, String newPasswordHash) {}

        @Override
        public void update(User user) {
            map.put(user.getId(), user);
        }

        @Override
        public void deleteById(Long id) {
            map.remove(id);
        }

        @Override
        public List<User> search(String field, String query) {
            if (field == null || query == null) return Collections.emptyList();
            String q = query.trim().toLowerCase();
            return map.values().stream()
                    .filter(u -> {
                        switch (field.toLowerCase().trim()) {
                            case "fname":
                            case "first name":
                                return u.getFname() != null && u.getFname().toLowerCase().contains(q);
                            case "lname":
                            case "last name":
                                return u.getLname() != null && u.getLname().toLowerCase().contains(q);
                            case "email":
                                return u.getEmail() != null && u.getEmail().toLowerCase().contains(q);
                            case "mobile":
                                return u.getMobile() != null && u.getMobile().equalsIgnoreCase(q);
                            case "username":
                                return u.getUsername() != null && u.getUsername().toLowerCase().contains(q);
                            case "status":
                                return u.getStatus() != null && u.getStatus().name().equalsIgnoreCase(q);
                            default:
                                return false;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(map.values());
        }
    }
}
