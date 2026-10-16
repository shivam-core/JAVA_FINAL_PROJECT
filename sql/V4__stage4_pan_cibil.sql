-- Migration V4: Add PAN card and CIBIL score to users table
ALTER TABLE users ADD COLUMN pan_card VARCHAR(10) NULL UNIQUE AFTER location;
ALTER TABLE users ADD COLUMN cibil_score INT NULL AFTER pan_card;
ALTER TABLE users ADD CONSTRAINT chk_cibil CHECK (cibil_score IS NULL OR (cibil_score >= 300 AND cibil_score <= 900));
