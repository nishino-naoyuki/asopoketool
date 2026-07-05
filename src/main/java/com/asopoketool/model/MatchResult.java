package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {
    private Long id;
    private Long matchId;
    private Long winnerEntryId;
    private String registeredBy; // PLAYER, ADMIN
    private LocalDateTime registeredAt;
}
