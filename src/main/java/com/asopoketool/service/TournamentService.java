package com.asopoketool.service;

import com.asopoketool.mapper.EntryMapper;
import com.asopoketool.mapper.TournamentMapper;
import com.asopoketool.model.Entry;
import com.asopoketool.model.Tournament;
import com.asopoketool.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TournamentService {

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private PointService pointService;

    @Autowired
    private com.asopoketool.mapper.MatchResultMapper matchResultMapper;

    @Autowired
    private com.asopoketool.mapper.MatchGameMapper matchGameMapper;

    @Autowired
    private com.asopoketool.mapper.TournamentRoundMapper roundMapper;

    @Autowired
    private com.asopoketool.mapper.PrizePointMapper prizePointMapper;

    @Autowired
    private com.asopoketool.mapper.TimerMapper timerMapper;

    @Autowired
    private com.asopoketool.mapper.BracketMapper bracketMapper;

    @Autowired
    private com.asopoketool.mapper.PointMapper pointMapper;

    @Transactional
    public void deleteTournament(Long id) {
        // Cascade delete in safe order (dependencies first)
        matchResultMapper.deleteByTournamentId(id);
        matchGameMapper.deleteByTournamentId(id);
        roundMapper.deleteByTournamentId(id);
        
        bracketMapper.deleteMatchesByTournamentId(id);
        bracketMapper.deleteByTournamentId(id);
        pointMapper.deleteHistoryByTournamentId(id);
        
        entryMapper.deleteByTournamentId(id);
        prizePointMapper.deleteByTournamentId(id);
        timerMapper.deleteByTournamentId(id);
        
        tournamentMapper.delete(id);
    }

    public List<Tournament> getAllTournaments() {
        return tournamentMapper.findAll();
    }

    public List<Tournament> getActiveTournaments() {
        return tournamentMapper.findActive();
    }

    public Tournament getTournamentById(Long id) {
        return tournamentMapper.findById(id);
    }

    public Tournament getTournamentWithCounts(Long id) {
        Tournament tournament = tournamentMapper.findById(id);
        // XML mapper has separate count methods, we don't have transient fields in Tournament entity for counts,
        // but we can query them dynamically or add to logs. Let's just return the tournament.
        return tournament;
    }

    @Transactional
    public void createTournament(Tournament tournament) {
        tournament.setStatus("ENTRY");
        tournament.setCurrentRound(0);
        tournamentMapper.insert(tournament);
    }

    @Transactional
    public void updateTournament(Tournament tournament) {
        tournamentMapper.update(tournament);
    }

    @Transactional
    public void startTournament(Long id) {
        Tournament tournament = tournamentMapper.findById(id);
        if (tournament == null) {
            throw new IllegalArgumentException("大会が見つかりません。");
        }
        if (!"ENTRY".equals(tournament.getStatus())) {
            throw new IllegalStateException("エントリー受付中以外の大会を開始することはできません。");
        }
        
        int entries = tournamentMapper.countCheckedIn(id);
        if (entries < 2) {
            throw new IllegalStateException("チェックイン完了した選手が2名未満のため、大会を開始できません。");
        }

        tournamentMapper.updateStatus(id, "IN_PROGRESS");
    }

    @Transactional
    public void startBracketPhase(Long id) {
        Tournament tournament = tournamentMapper.findById(id);
        if (tournament == null) {
            throw new IllegalArgumentException("大会が見つかりません。");
        }
        if (!"IN_PROGRESS".equals(tournament.getStatus())) {
            throw new IllegalStateException("スイスドロー進行中以外の大会を決勝トーナメントに進めることはできません。");
        }
        tournamentMapper.updateStatus(id, "BRACKET");
    }

    @Transactional
    public void finishTournament(Long id) {
        Tournament tournament = tournamentMapper.findById(id);
        if (tournament == null) {
            throw new IllegalArgumentException("大会が見つかりません。");
        }
        if (!"BRACKET".equals(tournament.getStatus()) && !"IN_PROGRESS".equals(tournament.getStatus())) {
            throw new IllegalStateException("進行中以外の大会を終了させることはできません。");
        }

        tournamentMapper.updateStatus(id, "FINISHED");
        
        // Award points to all participants (includes logic for members to aggregate total points)
        pointService.awardPoints(id);
    }

    @Transactional
    public void dropout(Long entryId) {
        Entry entry = entryMapper.findById(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("エントリーが見つかりません。");
        }
        entryMapper.dropout(entryId, LocalDateTime.now());
    }

    @Transactional
    public void cancelDropout(Long entryId) {
        Entry entry = entryMapper.findById(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("エントリーが見つかりません。");
        }
        entryMapper.cancelDropout(entryId);
    }

    @Transactional
    public void manualRegister(Long tournamentId, String playerName) {
        Tournament tournament = tournamentMapper.findById(tournamentId);
        if (tournament == null) {
            throw new IllegalArgumentException("大会が見つかりません。");
        }

        Entry entry = Entry.builder()
                .tournamentId(tournamentId)
                .playerName(playerName)
                .accountId(null) // guest/manual
                .sessionToken("MANUAL_" + TokenUtil.generateToken())
                .qrToken("MANUAL_QR_" + TokenUtil.generateToken())
                .checkinFlg(true) // manual registration automatically checked in
                .checkinAt(LocalDateTime.now())
                .dropoutFlg(false)
                .manualEntryFlg(true)
                .build();
        entryMapper.insert(entry);
    }

    public int countEntries(Long tournamentId) {
        return tournamentMapper.countEntries(tournamentId);
    }

    public int countCheckedIn(Long tournamentId) {
        return tournamentMapper.countCheckedIn(tournamentId);
    }

    public int countDropout(Long tournamentId) {
        return tournamentMapper.countDropout(tournamentId);
    }
}
