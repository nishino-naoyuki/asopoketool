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
public class Entry {
    private Long id;
    private Long tournamentId;
    private String playerName;
    private Long accountId; // NULL for guest
    private String sessionToken;
    private String qrToken;
    private boolean checkinFlg;
    private LocalDateTime checkinAt;
    private boolean dropoutFlg;
    private LocalDateTime dropoutAt;
    private boolean manualEntryFlg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient fields (Not in DB)
    private PlayerAccount account;
}
