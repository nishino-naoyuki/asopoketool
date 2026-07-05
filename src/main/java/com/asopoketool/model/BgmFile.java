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
public class BgmFile {
    private Long id;
    private String displayName;
    private String filePath;
    private long fileSize;
    private boolean isBuiltin;
    private LocalDateTime uploadedAt;
}
