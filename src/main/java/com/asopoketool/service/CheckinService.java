package com.asopoketool.service;

import com.asopoketool.mapper.EntryMapper;
import com.asopoketool.model.Entry;
import com.asopoketool.util.QRCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class CheckinService {

    @Autowired
    private EntryMapper entryMapper;

    @Value("${app.qr.hmac-secret}")
    private String hmacSecret;

    @Transactional
    public Entry checkinByQrPayload(String payload) throws Exception {
        Long entryId = QRCodeUtil.verifyPayload(payload, hmacSecret);
        Entry entry = entryMapper.findById(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("エントリーが見つかりません。");
        }
        if (entry.isCheckinFlg()) {
            return entry; // already checked in
        }
        entryMapper.checkin(entryId, LocalDateTime.now());
        entry.setCheckinFlg(true);
        entry.setCheckinAt(LocalDateTime.now());
        return entry;
    }

    @Transactional
    public void manualCheckin(Long entryId) {
        Entry entry = entryMapper.findById(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("エントリーが見つかりません。");
        }
        entryMapper.checkin(entryId, LocalDateTime.now());
    }
}
