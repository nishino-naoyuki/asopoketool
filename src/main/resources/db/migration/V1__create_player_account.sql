-- ============================================================
-- V1: player_account (参加者アカウント) ← entryより先に作成
-- ============================================================
CREATE TABLE IF NOT EXISTS player_account (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    display_name  VARCHAR(50) NOT NULL COMMENT 'アカウント表示名（即エントリー時のデフォルト名）',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCryptハッシュ',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_display_name (display_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参加者アカウント';
