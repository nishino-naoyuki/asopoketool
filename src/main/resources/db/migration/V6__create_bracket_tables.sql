-- ============================================================
-- V6: 決勝トーナメント関連テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS bracket_tournament (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tournament_id   BIGINT UNSIGNED NOT NULL COMMENT '大会ID',
    group_number    INT NOT NULL COMMENT 'グループ番号（1始まり）',
    group_name      VARCHAR(50) DEFAULT NULL COMMENT 'グループ名（例: グループA）',
    rank_from       INT NOT NULL COMMENT '順位範囲（開始）',
    rank_to         INT NOT NULL COMMENT '順位範囲（終了）',
    FOREIGN KEY (tournament_id) REFERENCES tournament(id) ON DELETE CASCADE,
    UNIQUE KEY uk_tournament_group (tournament_id, group_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='決勝トーナメントグループ';

CREATE TABLE IF NOT EXISTS bracket_match (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bracket_tournament_id   BIGINT UNSIGNED NOT NULL COMMENT '決勝T ID',
    round_number            INT NOT NULL COMMENT '決勝ラウンド番号（1:QF,2:SF,3:F）',
    match_number            INT NOT NULL COMMENT 'ラウンド内試合番号',
    player1_entry_id        BIGINT UNSIGNED DEFAULT NULL,
    player2_entry_id        BIGINT UNSIGNED DEFAULT NULL,
    winner_entry_id         BIGINT UNSIGNED DEFAULT NULL,
    is_bye                  TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'BYEフラグ',
    FOREIGN KEY (bracket_tournament_id) REFERENCES bracket_tournament(id) ON DELETE CASCADE,
    FOREIGN KEY (player1_entry_id)      REFERENCES entry(id),
    FOREIGN KEY (player2_entry_id)      REFERENCES entry(id),
    FOREIGN KEY (winner_entry_id)       REFERENCES entry(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='決勝対戦';
