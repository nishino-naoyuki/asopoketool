-- ============================================================
-- V4: entry (エントリー)
-- ============================================================
CREATE TABLE IF NOT EXISTS entry (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tournament_id    BIGINT UNSIGNED NOT NULL COMMENT '大会ID',
    player_name      VARCHAR(50) NOT NULL COMMENT 'エントリー名（大会ごとに任意）',
    account_id       BIGINT UNSIGNED DEFAULT NULL COMMENT 'アカウントID（ゲストはNULL）',
    session_token    VARCHAR(255) NOT NULL COMMENT 'セッショントークン（ゲスト識別用）',
    qr_token         VARCHAR(512) NOT NULL COMMENT 'チェックイン用QRトークン(HMAC付)',
    checkin_flg      TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'チェックインフラグ',
    checkin_at       DATETIME DEFAULT NULL COMMENT 'チェックイン日時',
    dropout_flg      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '棄権フラグ',
    dropout_at       DATETIME DEFAULT NULL COMMENT '棄権日時',
    manual_entry_flg TINYINT(1) NOT NULL DEFAULT 0 COMMENT '手動登録フラグ（飛び込み参加）',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES tournament(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id)    REFERENCES player_account(id) ON DELETE SET NULL,
    INDEX idx_tournament_session (tournament_id, session_token),
    INDEX idx_tournament_account (tournament_id, account_id),
    UNIQUE KEY uk_qr_token (qr_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大会エントリー';
