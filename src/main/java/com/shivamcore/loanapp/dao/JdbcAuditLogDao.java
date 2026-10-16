package com.shivamcore.loanapp.dao;

import com.shivamcore.loanapp.config.DatabaseManager;
import com.shivamcore.loanapp.exception.DataAccessException;
import com.shivamcore.loanapp.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcAuditLogDao implements AuditLogDao {
    private static final Logger logger = LoggerFactory.getLogger(JdbcAuditLogDao.class);
    private final DatabaseManager databaseManager;

    public JdbcAuditLogDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        String sql = "INSERT INTO audit_log (action, performed_by, target_user_id, target_username, details, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = auditLog.getCreatedAt() != null ? auditLog.getCreatedAt() : LocalDateTime.now();
            ps.setString(1, auditLog.getAction());
            ps.setString(2, auditLog.getPerformedBy());
            if (auditLog.getTargetUserId() != null) {
                ps.setLong(3, auditLog.getTargetUserId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setString(4, auditLog.getTargetUsername());
            ps.setString(5, auditLog.getDetails());
            ps.setTimestamp(6, Timestamp.valueOf(now));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    auditLog.setId(rs.getLong(1));
                }
            }
            auditLog.setCreatedAt(now);
            return auditLog;
        } catch (SQLException e) {
            logger.error("Failed to insert audit log entry", e);
            throw new DataAccessException("Failed to record audit log", e);
        }
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT id, action, performed_by, target_user_id, target_username, details, created_at " +
                "FROM audit_log ORDER BY id DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setId(rs.getLong("id"));
                    log.setAction(rs.getString("action"));
                    log.setPerformedBy(rs.getString("performed_by"));
                    long targetId = rs.getLong("target_user_id");
                    if (!rs.wasNull()) {
                        log.setTargetUserId(targetId);
                    }
                    log.setTargetUsername(rs.getString("target_username"));
                    log.setDetails(rs.getString("details"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        log.setCreatedAt(ts.toLocalDateTime());
                    }
                    list.add(log);
                }
            }
            return list;
        } catch (SQLException e) {
            logger.error("Failed to retrieve audit log entries", e);
            throw new DataAccessException("Failed to read audit log", e);
        }
    }
}
