-- ============================================================
-- V8: admin_user (管理者)
-- ============================================================
CREATE TABLE IF NOT EXISTS admin_user (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL COMMENT 'BCryptハッシュ',
    role            VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理者アカウント';

-- ============================================================
-- V8: BGM・タイマー関連テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS bgm_file (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL COMMENT '表示名',
    file_path    VARCHAR(500) NOT NULL COMMENT 'サーバー上のパス',
    file_size    BIGINT NOT NULL DEFAULT 0,
    is_builtin   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '内蔵BGMフラグ',
    uploaded_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BGMファイル';

CREATE TABLE IF NOT EXISTS timer_setting (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tournament_id    BIGINT UNSIGNED NOT NULL,
    round_number     INT NOT NULL DEFAULT 0 COMMENT '0=全体デフォルト',
    duration_minutes INT NOT NULL DEFAULT 30 COMMENT '対戦時間（分）',
    bgm_file_id      BIGINT UNSIGNED DEFAULT NULL,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES tournament(id) ON DELETE CASCADE,
    FOREIGN KEY (bgm_file_id)   REFERENCES bgm_file(id) ON DELETE SET NULL,
    UNIQUE KEY uk_tournament_round_timer (tournament_id, round_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='タイマー設定';
