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
public class PlayerCumulativePoint {
    private Long id;
    private Long accountId;
    private int totalPoint;
    private LocalDateTime updatedAt;

    // Transient fields
    private String displayName;
    private String recentResults; // e.g. "1位, 2位, 参加"
}
