-- Stage 2 Database Migration: Admin Roles and Audit Log

-- Add user_role to users table
ALTER TABLE users 
    ADD COLUMN user_role ENUM('ROLE_USER', 'ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER' AFTER password_hash;

-- Create audit_log table (independent of users to preserve logs on user deletion)
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(40) NOT NULL,
    performed_by VARCHAR(50) NOT NULL,
    target_user_id BIGINT NULL,
    target_username VARCHAR(50) NOT NULL,
    details VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_audit_log_created_at (created_at),
    INDEX idx_audit_log_performed_by (performed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
