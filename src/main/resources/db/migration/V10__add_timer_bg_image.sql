-- ============================================================
-- V10: timer_setting に背景画像パスを追加
-- ============================================================
ALTER TABLE timer_setting ADD COLUMN bg_image_path VARCHAR(500) DEFAULT NULL COMMENT '背景画像のサーバーパス' AFTER bgm_file_id;
