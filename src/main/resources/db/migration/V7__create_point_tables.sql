-- ============================================================
-- V7: ポイント関連テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS prize_point_setting (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tournament_id   BIGINT UNSIGNED NOT NULL,
    `rank`            INT NOT NULL COMMENT '順位（1〜16）',
    point           INT NOT NULL DEFAULT 0,
    FOREIGN KEY (tournament_id) REFERENCES tournament(id) ON DELETE CASCADE,
    UNIQUE KEY uk_tournament_rank (tournament_id, `rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入賞ポイント設定';

CREATE TABLE IF NOT EXISTS player_cumulative_point (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT UNSIGNED NOT NULL COMMENT 'アカウントID',
    total_point INT NOT NULL DEFAULT 0 COMMENT '累計ポイント',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES player_account(id) ON DELETE CASCADE,
    UNIQUE KEY uk_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='累計ポイント';

CREATE TABLE IF NOT EXISTS point_history (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tournament_id       BIGINT UNSIGNED NOT NULL,
    account_id          BIGINT UNSIGNED DEFAULT NULL COMMENT 'アカウントID（ゲストはNULL）',
    entry_id            BIGINT UNSIGNED NOT NULL COMMENT 'エントリーID',
    player_name         VARCHAR(50) NOT NULL COMMENT '大会でのエントリー名（表示用）',
    final_rank          INT DEFAULT NULL COMMENT '大会最終順位',
    prize_point         INT NOT NULL DEFAULT 0,
    participation_point INT NOT NULL DEFAULT 0,
    total_point         INT NOT NULL DEFAULT 0,
    awarded_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES tournament(id),
    FOREIGN KEY (account_id)    REFERENCES player_account(id) ON DELETE SET NULL,
    FOREIGN KEY (entry_id)      REFERENCES entry(id),
    INDEX idx_tournament   (tournament_id),
    INDEX idx_account      (account_id),
    INDEX idx_entry        (entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ポイント付与履歴';
