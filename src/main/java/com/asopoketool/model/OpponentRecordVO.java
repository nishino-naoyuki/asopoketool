package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 対戦相手別戦績のView Object（DB永続化なし・集計結果格納用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpponentRecordVO {
    private String opponentName;   // 相手のエントリー名（ゲストも含む）
    private int wins;
    private int losses;
    private String lastPlayedDate; // 最終対戦日（YYYY-MM-DD）

    public int getTotalGames() { return wins + losses; }
}
