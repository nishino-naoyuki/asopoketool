-- ============================================================
-- V2: player_account_token (自動ログイントークン)
-- ============================================================
CREATE TABLE IF NOT EXISTS player_account_token (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    account_id   BIGINT UNSIGNED NOT NULL COMMENT 'アカウントID',
    token        VARCHAR(512) NOT NULL COMMENT '自動ログイントークン(UUID)',
    expire_at    DATETIME NOT NULL COMMENT 'トークン有効期限（発行から120日）',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at DATETIME DEFAULT NULL COMMENT '最終使用日時',
    FOREIGN KEY (account_id) REFERENCES player_account(id) ON DELETE CASCADE,
    UNIQUE KEY uk_token (token),
    INDEX idx_account_id (account_id),
    INDEX idx_expire_at  (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参加者自動ログイントークン';
