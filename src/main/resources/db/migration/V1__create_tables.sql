-- ============================================================
-- V1: Tạo toàn bộ bảng cho banking app
-- ENUM values dùng UPPERCASE để khớp với Java enum name()
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100)                    NOT NULL,
    email         VARCHAR(150)                    NOT NULL UNIQUE,
    phone         VARCHAR(15)                     NOT NULL UNIQUE,
    password_hash VARCHAR(255)                    NOT NULL,
    pin_hash      VARCHAR(255),
    avatar_url    VARCHAR(255),
    status        ENUM ('ACTIVE','LOCKED')         NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME                         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME                         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT                           NOT NULL,
    account_number VARCHAR(20)                      NOT NULL UNIQUE,
    balance        DECIMAL(15, 2)                   NOT NULL DEFAULT 0.00,
    currency       VARCHAR(3)                       NOT NULL DEFAULT 'VND',
    account_type   ENUM ('PAYMENT','SAVINGS')        NOT NULL DEFAULT 'PAYMENT',
    status         ENUM ('ACTIVE','LOCKED')          NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_accounts_user (user_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_account_id      BIGINT,
    to_account_id        BIGINT,
    to_external_account  VARCHAR(20),
    to_bank_code         VARCHAR(20),
    amount               DECIMAL(15, 2)                                                           NOT NULL,
    fee                  DECIMAL(15, 2)                                                           NOT NULL DEFAULT 0.00,
    type                 ENUM ('INTERNAL','INTERBANK','TOPUP','SAVINGS_DEPOSIT','SAVINGS_WITHDRAW') NOT NULL,
    status               ENUM ('PENDING','SUCCESS','FAILED')                                      NOT NULL DEFAULT 'PENDING',
    description          VARCHAR(255),
    reference_code       VARCHAR(30)                                                              NOT NULL UNIQUE,
    created_at           DATETIME                                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_from FOREIGN KEY (from_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_tx_to FOREIGN KEY (to_account_id) REFERENCES accounts (id),
    INDEX idx_tx_from (from_account_id),
    INDEX idx_tx_to (to_account_id),
    INDEX idx_tx_created (created_at),
    INDEX idx_tx_type (type)
);

CREATE TABLE IF NOT EXISTS otp_codes (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT                                              NOT NULL,
    code       VARCHAR(10)                                         NOT NULL,
    purpose    ENUM ('REGISTER','LOGIN','TRANSFER','RESET_PASSWORD') NOT NULL,
    channel    ENUM ('SMS','EMAIL')                                NOT NULL,
    expires_at DATETIME                                            NOT NULL,
    is_used    BOOLEAN                                             NOT NULL DEFAULT FALSE,
    created_at DATETIME                                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_otp_user (user_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT                                      NOT NULL,
    title      VARCHAR(150)                                NOT NULL,
    content    TEXT,
    type       ENUM ('TRANSACTION','BALANCE','SYSTEM')     NOT NULL DEFAULT 'SYSTEM',
    is_read    BOOLEAN                                     NOT NULL DEFAULT FALSE,
    created_at DATETIME                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id)
);

CREATE TABLE IF NOT EXISTS savings (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT                             NOT NULL,
    source_account_id BIGINT                             NOT NULL,
    principal         DECIMAL(15, 2)                     NOT NULL,
    interest_rate     DECIMAL(5, 2)                      NOT NULL,
    term_months       INT                                NOT NULL,
    start_date        DATE                               NOT NULL,
    maturity_date     DATE                               NOT NULL,
    accrued_interest  DECIMAL(15, 2)                     NOT NULL DEFAULT 0.00,
    status            ENUM ('ACTIVE','MATURED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_savings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_savings_account FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    INDEX idx_savings_user (user_id)
);

CREATE TABLE IF NOT EXISTS devices (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    device_name       VARCHAR(100),
    device_id         VARCHAR(255),
    push_token        VARCHAR(255),
    biometric_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at     DATETIME,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_devices_user (user_id)
);
