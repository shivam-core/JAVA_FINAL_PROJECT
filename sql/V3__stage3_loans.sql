-- Stage 3: Loan Applications table
CREATE TABLE IF NOT EXISTS loan_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    loan_amount DECIMAL(15,2) NOT NULL,
    loan_type ENUM('HOME', 'PERSONAL', 'EDUCATION', 'VEHICLE') NOT NULL,
    loan_duration_months INT NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    annual_income DECIMAL(15,2) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'NEEDS_MORE_INFO') NOT NULL DEFAULT 'PENDING',
    application_date DATETIME NOT NULL,
    rejection_reason VARCHAR(255) NULL,
    INDEX idx_loan_user_id (user_id),
    INDEX idx_loan_status (status),
    CONSTRAINT fk_loan_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
