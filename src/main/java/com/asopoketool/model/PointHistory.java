package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistory {
    private Long id;
    private Long tournamentId;
    private Long accountId; // NULL for guest
    private Long entryId;
    private String playerName; // entry name during that tournament
    private Integer finalRank;
    private int prizePoint;
    private int participationPoint;
    private int totalPoint;
    private LocalDateTime awardedAt;

    // Transient fields
    private String tournamentName;
    private LocalDate heldDate;
}
