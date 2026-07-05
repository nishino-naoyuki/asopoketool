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
public class PlayerAccountToken {
    private Long id;
    private Long accountId;
    private String token;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
