-- Lưu ý định chuyển tiền giữa bước xác thực PIN và xác thực OTP.
-- Phiên tự hết hạn sau 5 phút (kiểm tra tại tầng ứng dụng).
-- used = TRUE sau khi giao dịch được thực thi, ngăn replay attack.
CREATE TABLE transfer_sessions (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    confirm_token        VARCHAR(36)  NOT NULL,
    user_id              BIGINT       NOT NULL,
    transfer_type        VARCHAR(20)  NOT NULL,          -- INTERNAL | INTERBANK
    from_account_id      BIGINT       NOT NULL,
    to_account_number    VARCHAR(20)  NOT NULL,
    to_account_name      VARCHAR(100) NULL,              -- liên ngân hàng
    to_bank_code         VARCHAR(20)  NULL,              -- liên ngân hàng
    amount               DECIMAL(15,2) NOT NULL,
    description          VARCHAR(255) NULL,
    expires_at           DATETIME     NOT NULL,
    used                 TINYINT(1)   NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_confirm_token (confirm_token),
    CONSTRAINT fk_ts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
