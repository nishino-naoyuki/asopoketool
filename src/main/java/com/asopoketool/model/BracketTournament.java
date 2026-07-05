package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BracketTournament {
    private Long id;
    private Long tournamentId;
    private int groupNumber;
    private String groupName;
    private int rankFrom;
    private int rankTo;

    // Transient fields
    private List<BracketMatch> matches;
}
