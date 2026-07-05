package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchGame {
    private Long id;
    private Long roundId;
    private int tableNumber;
    private Long player1EntryId;
    private Long player2EntryId; // Nullable (for BYE)
    private boolean isBye;

    // Transient fields
    private String player1Name;
    private String player2Name;
    private Long resultWinnerEntryId;
}
