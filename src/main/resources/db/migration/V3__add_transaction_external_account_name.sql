-- Lưu tên chủ tài khoản đích cho giao dịch liên ngân hàng.
-- Nullable: không áp dụng cho INTERNAL, TOPUP, SAVINGS_*.
ALTER TABLE transactions
    ADD COLUMN to_external_account_name VARCHAR(100) NULL
        AFTER to_external_account;
