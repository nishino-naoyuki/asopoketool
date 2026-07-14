package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大会別戦績のView Object（DB永続化なし・集計結果格納用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRecordVO {
    private Long tournamentId;
    private String tournamentName;
    private String heldDate;       // YYYY-MM-DD 文字列
    private String entryName;      // 大会登録時の名前

    // 予選（スイス式）成績
    private int swissWins;
    private int swissLosses;
    private int swissByes;         // BYE（不戦勝）の回数

    // 決勝トーナメント成績
    private boolean bracketParticipated;
    private int bracketWins;
    private int bracketLosses;

    // 合計
    public int getTotalWins()   { return swissWins   + bracketWins;   }
    public int getTotalLosses() { return swissLosses + bracketLosses; }

    // 結果（"CHAMPION", "RUNNER_UP", または null）
    private String result;

    public boolean isChampion()  { return "CHAMPION".equals(result);  }
    public boolean isRunnerUp()  { return "RUNNER_UP".equals(result); }
}
