-- ============================================================
-- V5: tournament_round (ラウンド)
-- ============================================================
CREATE TABLE IF NOT EXISTS tournament_round (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tournament_id   BIGINT UNSIGNED NOT NULL COMMENT '大会ID',
    round_number    INT NOT NULL COMMENT 'ラウンド番号（1始まり）',
    status          ENUM('IN_PROGRESS','FINISHED') NOT NULL DEFAULT 'IN_PROGRESS',
    started_at      DATETIME DEFAULT NULL,
    finished_at     DATETIME DEFAULT NULL,
    FOREIGN KEY (tournament_id) REFERENCES tournament(id) ON DELETE CASCADE,
    UNIQUE KEY uk_tournament_round (tournament_id, round_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='スイスドローラウンド';

-- ============================================================
-- V5: match_game (対戦)
-- ============================================================
CREATE TABLE IF NOT EXISTS match_game (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    round_id            BIGINT UNSIGNED NOT NULL COMMENT 'ラウンドID',
    table_number        INT NOT NULL COMMENT '卓番号',
    player1_entry_id    BIGINT UNSIGNED NOT NULL COMMENT '先手エントリーID',
    player2_entry_id    BIGINT UNSIGNED DEFAULT NULL COMMENT '後手エントリーID（BYE=NULL）',
    is_bye              TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'BYE（不戦勝）フラグ',
    FOREIGN KEY (round_id)           REFERENCES tournament_round(id) ON DELETE CASCADE,
    FOREIGN KEY (player1_entry_id)   REFERENCES entry(id),
    FOREIGN KEY (player2_entry_id)   REFERENCES entry(id),
    INDEX idx_round_table (round_id, table_number),
    INDEX idx_player1 (player1_entry_id),
    INDEX idx_player2 (player2_entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='対戦';

-- ============================================================
-- V5: match_result (対戦結果)
-- ============================================================
CREATE TABLE IF NOT EXISTS match_result (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    match_id         BIGINT UNSIGNED NOT NULL COMMENT '対戦ID',
    winner_entry_id  BIGINT UNSIGNED NOT NULL COMMENT '勝者エントリーID',
    registered_by    ENUM('PLAYER','ADMIN') NOT NULL DEFAULT 'PLAYER',
    registered_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id)         REFERENCES match_game(id) ON DELETE CASCADE,
    FOREIGN KEY (winner_entry_id)  REFERENCES entry(id),
    UNIQUE KEY uk_match_result (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='対戦結果';
