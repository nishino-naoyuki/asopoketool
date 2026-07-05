package com.asopoketool.service;

import com.asopoketool.mapper.EntryMapper;
import com.asopoketool.mapper.TournamentMapper;
import com.asopoketool.model.Entry;
import com.asopoketool.model.Tournament;
import com.asopoketool.util.QRCodeUtil;
import com.asopoketool.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntryService {

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Value("${app.qr.hmac-secret}")
    private String hmacSecret;

    @Transactional
    public Entry enterTournament(Long tournamentId, String playerName, Long accountId, String sessionToken) {
        Tournament tournament = tournamentMapper.findById(tournamentId);
        if (tournament == null) {
            throw new IllegalArgumentException("指定された大会が見つかりません。");
        }
        if (!"ENTRY".equals(tournament.getStatus())) {
            throw new IllegalStateException("この大会は現在エントリーを受け付けていません。");
        }

        // Capacity check
        int currentEntries = tournamentMapper.countEntries(tournamentId);
        if (currentEntries >= tournament.getCapacity()) {
            throw new IllegalStateException("大会の定員に達しているためエントリーできません。");
        }

        // Duplicate entry check
        if (accountId != null) {
            Entry existing = entryMapper.findByTournamentAndAccount(tournamentId, accountId);
            if (existing != null) {
                throw new IllegalStateException("すでにこの大会にエントリーしています。");
            }
        } else {
            Entry existing = entryMapper.findByTournamentAndSession(tournamentId, sessionToken);
            if (existing != null) {
                throw new IllegalStateException("すでにこの大会にエントリーしています。");
            }
        }

        // Setup entry info
        String qrToken = TokenUtil.generateToken(); // temporary unique code

        Entry entry = Entry.builder()
                .tournamentId(tournamentId)
                .playerName(playerName)
                .accountId(accountId)
                .sessionToken(sessionToken)
                .qrToken(qrToken)
                .checkinFlg(false)
                .dropoutFlg(false)
                .manualEntryFlg(false)
                .build();
        entryMapper.insert(entry);

        // Calculate secure QR code HMAC payload using generated entry id
        String securePayload = QRCodeUtil.generatePayload(entry.getId(), tournamentId, hmacSecret);
        entry.setQrToken(securePayload);
        entryMapper.update(entry);

        return entry;
    }

    @Transactional
    public void cancelEntry(Long entryId) {
        Entry entry = entryMapper.findById(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("エントリーが見つかりません。");
        }
        Tournament tournament = tournamentMapper.findById(entry.getTournamentId());
        if (tournament == null || !"ENTRY".equals(tournament.getStatus())) {
            throw new IllegalStateException("エントリー受付中以外の大会はキャンセルできません。");
        }
        entryMapper.cancelEntry(entryId);
    }

    public Entry getEntryById(Long id) {
        return entryMapper.findById(id);
    }

    public Entry getEntryByQrToken(String qrToken) {
        return entryMapper.findByQrToken(qrToken);
    }

    public Entry getEntryForCurrentUser(Long tournamentId, String sessionToken, Long accountId) {
        if (accountId != null) {
            return entryMapper.findByTournamentAndAccount(tournamentId, accountId);
        }
        return entryMapper.findByTournamentAndSession(tournamentId, sessionToken);
    }

    public byte[] generateQRCodeBytes(Long entryId) throws Exception {
        Entry entry = entryMapper.findById(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("Entry not found");
        }
        return QRCodeUtil.generateQRCode(entry.getQrToken(), 300, 300);
    }

    public List<Entry> getEntriesByTournament(Long tournamentId) {
        return entryMapper.findByTournamentId(tournamentId);
    }
}
