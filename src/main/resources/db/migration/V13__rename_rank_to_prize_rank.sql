DELIMITER //
CREATE PROCEDURE RenameRankToPrizeRank()
BEGIN
    IF EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'prize_point_setting' 
          AND COLUMN_NAME = 'rank'
    ) THEN
        ALTER TABLE prize_point_setting CHANGE `rank` prize_rank INT NOT NULL COMMENT '順位（1〜16）';
    END IF;
END //
DELIMITER ;
CALL RenameRankToPrizeRank();
DROP PROCEDURE RenameRankToPrizeRank;
