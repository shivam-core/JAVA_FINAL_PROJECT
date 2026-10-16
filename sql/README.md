# Database Migration Scripts

Execute the migration scripts in sequential order against your MySQL database:

1. `V1__stage1_users.sql`: Creates the base `users` table with authentication, password hashing, and failed attempt tracking.
2. `V2__stage2_roles_audit.sql`: Adds `user_role` column and creates the `audit_log` table (Stage 2).
3. `V3__stage3_loans.sql`: Creates `loan_applications` table (Stage 3).
4. `V4__stage4_pan_cibil.sql`: Adds `pan_card` and `cibil_score` columns to `users` (Stage 4).
5. `V5__stage5_emi.sql`: Adds `emi_amount`, `total_interest`, and `total_payable` to `loan_applications` (Stage 5).

Example command:
```bash
mysql -u root -p loan_app < sql/V1__stage1_users.sql
```
