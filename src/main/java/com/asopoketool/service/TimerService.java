package com.asopoketool.service;

import com.asopoketool.mapper.TimerMapper;
import com.asopoketool.model.BgmFile;
import com.asopoketool.model.TimerSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimerService {

    @Autowired
    private TimerMapper timerMapper;

    @Value("${app.bgm.upload-dir}")
    private String uploadDir;

    public TimerSetting getTimerSetting(Long tournamentId, int roundNumber) {
        // Try to get specific round setting
        TimerSetting setting = timerMapper.findByTournamentAndRound(tournamentId, roundNumber);
        if (setting == null && roundNumber > 0) {
            // Fallback to round 0 (default settings for the tournament)
            setting = timerMapper.findByTournamentAndRound(tournamentId, 0);
        }
        if (setting == null) {
            // Fallback to absolute system default
            setting = TimerSetting.builder()
                    .tournamentId(tournamentId)
                    .roundNumber(roundNumber)
                    .durationMinutes(30) // Default 30 min
                    .build();
        }
        return setting;
    }

    @Transactional
    public void saveTimerSetting(TimerSetting setting) {
        timerMapper.upsert(setting);
    }

    public List<BgmFile> getAllBgm() {
        return timerMapper.findAllBgm();
    }

    public BgmFile getBgmById(Long id) {
        return timerMapper.findBgmById(id);
    }

    @Transactional
    public BgmFile uploadBgm(String displayName, byte[] fileBytes, String originalFilename) throws IOException {
        // Ensure upload directory exists
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Generate unique local filename
        String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;
        File destFile = new File(dir, uniqueFilename);
        
        // Write file bytes to disk
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(fileBytes);
        }

        // Web path config maps to resource handler /bgm-files/**
        String fileWebPath = "/asopoketool/bgm-files/" + uniqueFilename;

        BgmFile bgm = BgmFile.builder()
                .displayName(displayName)
                .filePath(fileWebPath)
                .fileSize(fileBytes.length)
                .isBuiltin(false)
                .uploadedAt(LocalDateTime.now())
                .build();
        timerMapper.insertBgm(bgm);
        return bgm;
    }

    @Value("${app.timer.upload-dir:/opt/asopoketool/images}")
    private String timerImageDir;

    @Transactional
    public String uploadBgImage(Long tournamentId, byte[] fileBytes, String originalFilename) throws IOException {
        File dir = new File(timerImageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String uniqueFilename = "bg_" + tournamentId + "_" + System.currentTimeMillis() + "_" + originalFilename;
        File destFile = new File(dir, uniqueFilename);
        
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(fileBytes);
        }

        String fileWebPath = "/asopoketool/timer-bg-files/" + uniqueFilename;
        
        TimerSetting setting = getTimerSetting(tournamentId, 0);
        setting.setBgImagePath(fileWebPath);
        saveTimerSetting(setting);

        return fileWebPath;
    }
}
