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
public class TimerSetting {
    private Long id;
    private Long tournamentId;
    private int roundNumber; // 0 for default
    private int durationMinutes;
    private Long bgmFileId;
    private String bgImagePath;
    private LocalDateTime updatedAt;

    // Transient fields
    private String bgmDisplayName;
    private String bgmFilePath;
}
