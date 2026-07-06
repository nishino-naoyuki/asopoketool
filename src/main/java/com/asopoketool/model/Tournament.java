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
public class Tournament {
    private Long id;
    private String name;
    private LocalDate heldDate;
    private int capacity;
    private int totalRounds;
    private int currentRound;
    private int winPoint;
    private int losePoint;
    private int participationPoint;
    private int bracketGroupSize;
    private String status; // ENTRY, IN_PROGRESS, BRACKET, FINISHED
    private String description;
    private String venue;
    private String iconPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
