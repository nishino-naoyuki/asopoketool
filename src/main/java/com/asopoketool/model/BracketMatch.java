package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BracketMatch {
    private Long id;
    private Long bracketTournamentId;
    private int roundNumber;
    private int matchNumber;
    private Long player1EntryId; // Nullable (if BYE or waiting)
    private Long player2EntryId; // Nullable
    private Long winnerEntryId;
    private boolean isBye;

    // Transient fields
    private String player1Name;
    private String player2Name;
    private String winnerName;
}
