package com.asopoketool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrizePointSetting {
    private Long id;
    private Long tournamentId;
    private int prizeRank;
    private int point;
}
