-- ============================================================
-- V9: 初期データ投入
-- ============================================================

-- 管理者アカウント（初期パスワード: admin123）
-- BCrypt: $2a$10$8.2c65v7exV16Flv8myifeE9ZCS64w1SO0.4tpiLz1WjK.M.O9Jz.
INSERT INTO admin_user (username, password_hash, role)
VALUES ('admin', '$2a$10$8.2c65v7exV16Flv8myifeE9ZCS64w1SO0.4tpiLz1WjK.M.O9Jz.', 'ADMIN')
ON DUPLICATE KEY UPDATE username = username;

-- 内蔵BGMファイル（ファイルは static/audio/ に配置）
INSERT INTO bgm_file (display_name, file_path, is_builtin) VALUES
('バトルBGM 1',  '/asopoketool/audio/battle1.mp3',  1),
('バトルBGM 2',  '/asopoketool/audio/battle2.mp3',  1),
('フィナーレBGM', '/asopoketool/audio/finale.mp3',   1)
ON DUPLICATE KEY UPDATE display_name = display_name;
