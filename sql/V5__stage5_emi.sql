-- Migration V5: Add EMI calculation columns to loan_applications
ALTER TABLE loan_applications ADD COLUMN emi_amount DECIMAL(15,2) NULL;
ALTER TABLE loan_applications ADD COLUMN total_interest DECIMAL(15,2) NULL;
ALTER TABLE loan_applications ADD COLUMN total_payable DECIMAL(15,2) NULL;
