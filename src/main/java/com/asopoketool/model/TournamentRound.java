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
public class TournamentRound {
    private Long id;
    private Long tournamentId;
    private int roundNumber;
    private String status; // IN_PROGRESS, FINISHED
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
