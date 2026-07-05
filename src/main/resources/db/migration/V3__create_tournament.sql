-- ============================================================
-- V3: tournament (大会)
-- ============================================================
CREATE TABLE IF NOT EXISTS tournament (
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL COMMENT '大会名',
    held_date            DATE NOT NULL COMMENT '開催日',
    capacity             INT NOT NULL DEFAULT 32 COMMENT '定員',
    total_rounds         INT NOT NULL DEFAULT 5 COMMENT 'スイスドロー総ラウンド数',
    current_round        INT NOT NULL DEFAULT 0 COMMENT '現在のラウンド（0=未開始）',
    win_point            INT NOT NULL DEFAULT 3 COMMENT '勝利ポイント',
    lose_point           INT NOT NULL DEFAULT 0 COMMENT '敗北ポイント',
    participation_point  INT NOT NULL DEFAULT 1 COMMENT '参加ポイント',
    bracket_group_size   INT NOT NULL DEFAULT 8 COMMENT '決勝T 1グループの人数',
    status               ENUM('ENTRY','IN_PROGRESS','BRACKET','FINISHED') NOT NULL DEFAULT 'ENTRY' COMMENT '大会ステータス',
    description          TEXT DEFAULT NULL COMMENT '大会説明',
    venue                VARCHAR(200) DEFAULT NULL COMMENT '会場',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大会';
