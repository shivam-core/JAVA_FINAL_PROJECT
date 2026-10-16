package com.shivamcore.loanapp.dao;

import com.shivamcore.loanapp.config.DatabaseManager;
import com.shivamcore.loanapp.exception.DataAccessException;
import com.shivamcore.loanapp.exception.DuplicateResourceException;
import com.shivamcore.loanapp.model.Role;
import com.shivamcore.loanapp.model.Status;
import com.shivamcore.loanapp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of UserDao using MySQL and PreparedStatement exclusively.
 */
public class JdbcUserDao implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(JdbcUserDao.class);

    private final DatabaseManager databaseManager;

    public JdbcUserDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = user.getLastModifiedDate() != null ? user.getLastModifiedDate() : LocalDateTime.now();
            ps.setString(1, user.getFname());
            ps.setString(2, user.getLname());
            ps.setString(3, user.getMobile());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getLocation());
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setString(7, user.getStatus() != null ? user.getStatus().name() : Status.ACTIVE.name());
            ps.setString(8, user.getUsername());
            ps.setString(9, user.getPasswordHash());
            ps.setString(10, user.getRole() != null ? user.getRole().name() : Role.ROLE_USER.name());
            ps.setInt(11, user.getFailedAttempts());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
            user.setLastModifiedDate(now);
            return user;
        } catch (SQLException e) {
            handleSqlException(e, "save user: " + user.getUsername());
            throw new DataAccessException("Failed to save user", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts " +
                "FROM users WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", id, e);
            throw new DataAccessException("Failed to query user by id", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts " +
                "FROM users WHERE username = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
            throw new DataAccessException("Failed to query user by username", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts " +
                "FROM users WHERE email = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by email: {}", email, e);
            throw new DataAccessException("Failed to query user by email", e);
        }
    }

    @Override
    public Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();
        if (trimmed.contains("@")) {
            return findByEmail(trimmed);
        } else {
            return findByUsername(trimmed);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking existence by username: {}", username, e);
            throw new DataAccessException("Failed to check username existence", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking existence by email: {}", email, e);
            throw new DataAccessException("Failed to check email existence", e);
        }
    }

    @Override
    public boolean existsByRole(Role role) {
        String sql = "SELECT 1 FROM users WHERE user_role = ? LIMIT 1";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role != null ? role.name() : Role.ROLE_USER.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking existence by role: {}", role, e);
            throw new DataAccessException("Failed to check role existence", e);
        }
    }

    @Override
    public int incrementFailedAttempts(Long userId) {
        String updateSql = "UPDATE users SET failed_attempts = failed_attempts + 1, last_modified_date = ? WHERE id = ?";
        String selectSql = "SELECT failed_attempts FROM users WHERE id = ?";

        try (Connection conn = databaseManager.getConnection()) {
            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                updatePs.setLong(2, userId);
                updatePs.executeUpdate();
            }

            try (PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
                selectPs.setLong(1, userId);
                try (ResultSet rs = selectPs.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            throw new DataAccessException("User not found after incrementing failed attempts: " + userId);
        } catch (SQLException e) {
            logger.error("Error incrementing failed attempts for user id: {}", userId, e);
            throw new DataAccessException("Failed to increment failed attempts", e);
        }
    }

    @Override
    public void lockAccount(Long userId) {
        String sql = "UPDATE users SET status = ?, last_modified_date = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, Status.INACTIVE.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, userId);
            ps.executeUpdate();
            logger.info("User id {} has been locked (status=INACTIVE)", userId);
        } catch (SQLException e) {
            logger.error("Error locking account for user id: {}", userId, e);
            throw new DataAccessException("Failed to lock account", e);
        }
    }

    @Override
    public void resetFailedAttempts(Long userId) {
        String sql = "UPDATE users SET failed_attempts = 0, last_modified_date = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, userId);
            ps.executeUpdate();
            logger.debug("Reset failed_attempts to 0 for user id: {}", userId);
        } catch (SQLException e) {
            logger.error("Error resetting failed attempts for user id: {}", userId, e);
            throw new DataAccessException("Failed to reset failed attempts", e);
        }
    }

    @Override
    public void updatePassword(Long userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, last_modified_date = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPasswordHash);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, userId);
            ps.executeUpdate();
            logger.info("Updated password hash for user id: {}", userId);
        } catch (SQLException e) {
            logger.error("Error updating password for user id: {}", userId, e);
            throw new DataAccessException("Failed to update password", e);
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET fname = ?, lname = ?, mobile = ?, email = ?, location = ?, " +
                "last_modified_date = ?, status = ?, username = ?, password_hash = ?, user_role = ?, failed_attempts = ? " +
                "WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, user.getFname());
            ps.setString(2, user.getLname());
            ps.setString(3, user.getMobile());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getLocation());
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setString(7, user.getStatus().name());
            ps.setString(8, user.getUsername());
            ps.setString(9, user.getPasswordHash());
            ps.setString(10, user.getRole() != null ? user.getRole().name() : Role.ROLE_USER.name());
            ps.setInt(11, user.getFailedAttempts());
            ps.setLong(12, user.getId());

            ps.executeUpdate();
            user.setLastModifiedDate(now);
        } catch (SQLException e) {
            handleSqlException(e, "update user id: " + user.getId());
            throw new DataAccessException("Failed to update user", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            logger.info("Deleted user with id: {}", id);
        } catch (SQLException e) {
            logger.error("Error deleting user with id: {}", id, e);
            throw new DataAccessException("Failed to delete user", e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts " +
                "FROM users ORDER BY id ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
            return list;
        } catch (SQLException e) {
            logger.error("Error fetching all users", e);
            throw new DataAccessException("Failed to fetch all users", e);
        }
    }

    @Override
    public List<User> search(String field, String query) {
        if (field == null || query == null) {
            return Collections.emptyList();
        }
        String cleanQuery = query.trim();
        String sql;
        boolean isLike = true;

        switch (field.toLowerCase().trim()) {
            case "fname":
            case "first name":
            case "firstname":
                sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts FROM users WHERE fname LIKE ? ORDER BY id ASC";
                break;
            case "lname":
            case "last name":
            case "lastname":
                sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts FROM users WHERE lname LIKE ? ORDER BY id ASC";
                break;
            case "email":
                sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts FROM users WHERE email LIKE ? ORDER BY id ASC";
                break;
            case "mobile":
                sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts FROM users WHERE mobile = ? ORDER BY id ASC";
                isLike = false;
                break;
            case "username":
                sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts FROM users WHERE username LIKE ? ORDER BY id ASC";
                break;
            case "status":
                sql = "SELECT id, fname, lname, mobile, email, location, last_modified_date, status, username, password_hash, user_role, failed_attempts FROM users WHERE status = ? ORDER BY id ASC";
                isLike = false;
                break;
            default:
                throw new IllegalArgumentException("Unsupported search field: " + field);
        }

        List<User> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (isLike) {
                ps.setString(1, "%" + cleanQuery + "%");
            } else {
                ps.setString(1, cleanQuery.toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToUser(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            logger.error("Error searching users with field '{}' and query '{}'", field, query, e);
            throw new DataAccessException("Failed to search users", e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setFname(rs.getString("fname"));
        user.setLname(rs.getString("lname"));
        user.setMobile(rs.getString("mobile"));
        user.setEmail(rs.getString("email"));
        user.setLocation(rs.getString("location"));

        Timestamp ts = rs.getTimestamp("last_modified_date");
        if (ts != null) {
            user.setLastModifiedDate(ts.toLocalDateTime());
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            user.setStatus(Status.valueOf(statusStr));
        }

        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));

        String roleStr = rs.getString("user_role");
        if (roleStr != null) {
            try {
                user.setRole(Role.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                user.setRole(Role.ROLE_USER);
            }
        } else {
            user.setRole(Role.ROLE_USER);
        }

        user.setFailedAttempts(rs.getInt("failed_attempts"));
        return user;
    }

    private void handleSqlException(SQLException e, String action) {
        logger.error("SQL Error during {}: [SQLState: {}, ErrorCode: {}] {}",
                action, e.getSQLState(), e.getErrorCode(), e.getMessage());
        // MySQL 1062 is ER_DUP_ENTRY; SQLState 23000 is integrity constraint violation
        if (e.getErrorCode() == 1062 || "23000".equals(e.getSQLState())) {
            throw new DuplicateResourceException("Username or email already exists.", e);
        }
    }
}
