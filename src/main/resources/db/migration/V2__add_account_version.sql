-- Thêm cột version để hỗ trợ Optimistic Locking (@Version) trên entity Account.
-- Default 0 để các row hiện tại không bị coi là "stale" ngay lập tức.
ALTER TABLE accounts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
