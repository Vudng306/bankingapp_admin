-- V5: Thêm bảng phone_topups và cards (Phần 3)

CREATE TABLE IF NOT EXISTS phone_topups (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT         NOT NULL UNIQUE,
    carrier        VARCHAR(20)    NOT NULL,
    phone_number   VARCHAR(15)    NOT NULL,
    face_value     DECIMAL(15, 2) NOT NULL,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_topup_tx FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    INDEX idx_topup_tx (transaction_id)
);

CREATE TABLE IF NOT EXISTS cards (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT                  NOT NULL,
    card_number      VARCHAR(20)             NOT NULL UNIQUE,
    expiry_date      DATE                    NOT NULL,
    cardholder_name  VARCHAR(100)            NOT NULL,
    status           ENUM ('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    daily_limit      DECIMAL(15, 2),
    created_at       DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    INDEX idx_cards_account (account_id)
);
