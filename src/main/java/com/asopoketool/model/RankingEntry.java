package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingEntry {
    private Long entryId;
    private Long tournamentId;
    private String playerName;
    private Long accountId;
    private int wins;
    private int losses;
    private int totalMatches;
    private int winPoints;
    private double omwPercent;
    private int opponentWinPointsTotal;
    private double oomwPercent;
    private int rank;
    private boolean isDropout;
    private boolean isBye;
}
