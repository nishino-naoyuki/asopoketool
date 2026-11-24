package com.asopoketool.service;

import com.asopoketool.mapper.*;
import com.asopoketool.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RankingServiceTest {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private SwissDrawService swissDrawService;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private MatchResultMapper matchResultMapper;

    @Test
    public void testRankingExcludesUncheckedInEntries() {
        // 1. Create a tournament
        Tournament tournament = Tournament.builder()
                .name("Ranking Checkin Test Tournament")
                .heldDate(LocalDate.now())
                .capacity(10)
                .totalRounds(3)
                .currentRound(0)
                .status("IN_PROGRESS")
                .winPoint(3)
                .bracketGroupSize(8)
                .build();
        tournamentMapper.insert(tournament);
        Long tournamentId = tournament.getId();

        // 2. Create entries: 3 checked in, 2 NOT checked in
        Entry p1 = Entry.builder()
                .tournamentId(tournamentId)
                .playerName("Player 1 (Checked-in)")
                .sessionToken("SESS_P1")
                .qrToken("QR_P1")
                .checkinFlg(true)
                .checkinAt(LocalDateTime.now())
                .dropoutFlg(false)
                .build();
        entryMapper.insert(p1);

        Entry p2 = Entry.builder()
                .tournamentId(tournamentId)
                .playerName("Player 2 (Checked-in)")
                .sessionToken("SESS_P2")
                .qrToken("QR_P2")
                .checkinFlg(true)
                .checkinAt(LocalDateTime.now())
                .dropoutFlg(false)
                .build();
        entryMapper.insert(p2);

        Entry p3 = Entry.builder()
                .tournamentId(tournamentId)
                .playerName("Player 3 (Checked-in)")
                .sessionToken("SESS_P3")
                .qrToken("QR_P3")
                .checkinFlg(true)
                .checkinAt(LocalDateTime.now())
                .dropoutFlg(false)
                .build();
        entryMapper.insert(p3);

        Entry p4 = Entry.builder()
                .tournamentId(tournamentId)
                .playerName("Player 4 (Unchecked-in)")
                .sessionToken("SESS_P4")
                .qrToken("QR_P4")
                .checkinFlg(false)
                .dropoutFlg(false)
                .build();
        entryMapper.insert(p4);

        Entry p5 = Entry.builder()
                .tournamentId(tournamentId)
                .playerName("Player 5 (Unchecked-in)")
                .sessionToken("SESS_P5")
                .qrToken("QR_P5")
                .checkinFlg(false)
                .dropoutFlg(false)
                .build();
        entryMapper.insert(p5);

        // 3. Before any matches, getRanking should only return 3 checked-in players
        List<RankingEntry> rankingBefore = rankingService.getRanking(tournamentId);
        assertEquals(3, rankingBefore.size(), "Only 3 checked-in players should be in ranking");
        assertTrue(rankingBefore.stream().anyMatch(r -> r.getEntryId().equals(p1.getId())));
        assertTrue(rankingBefore.stream().anyMatch(r -> r.getEntryId().equals(p2.getId())));
        assertTrue(rankingBefore.stream().anyMatch(r -> r.getEntryId().equals(p3.getId())));
        assertFalse(rankingBefore.stream().anyMatch(r -> r.getEntryId().equals(p4.getId())), "Player 4 should NOT be in ranking");
        assertFalse(rankingBefore.stream().anyMatch(r -> r.getEntryId().equals(p5.getId())), "Player 5 should NOT be in ranking");

        // 4. Generate Round 1 matches (3 active players -> 1 match + 1 BYE)
        List<MatchGame> matches = swissDrawService.generateMatching(tournamentId);
        tournament.setCurrentRound(1);
        tournamentMapper.update(tournament);

        // Register match result: p1 wins against p2
        for (MatchGame m : matches) {
            if (!m.isBye()) {
                MatchResult mr = MatchResult.builder()
                        .matchId(m.getId())
                        .winnerEntryId(p1.getId())
                        .registeredBy("ADMIN")
                        .build();
                matchResultMapper.insert(mr);
            }
        }

        // 5. Check rankings after Round 1
        List<RankingEntry> rankingAfterR1 = rankingService.getRanking(tournamentId);
        assertEquals(3, rankingAfterR1.size(), "Still only 3 checked-in players should be in ranking");
        assertFalse(rankingAfterR1.stream().anyMatch(r -> r.getEntryId().equals(p4.getId())));
        assertFalse(rankingAfterR1.stream().anyMatch(r -> r.getEntryId().equals(p5.getId())));

        // Verify ranks are assigned
        assertEquals(1, rankingAfterR1.get(0).getRank());
        assertTrue(rankingAfterR1.get(2).getRank() >= 2, "Last place should be rank 2 or 3");
    }

    @Test
    public void testRankingEmptyWhenNoCheckedInEntries() {
        Tournament tournament = Tournament.builder()
                .name("Empty Ranking Tournament")
                .heldDate(LocalDate.now())
                .capacity(10)
                .totalRounds(3)
                .currentRound(0)
                .status("IN_PROGRESS")
                .winPoint(3)
                .bracketGroupSize(8)
                .build();
        tournamentMapper.insert(tournament);

        Entry p1 = Entry.builder()
                .tournamentId(tournament.getId())
                .playerName("Player 1 (Unchecked-in)")
                .sessionToken("SESS_EMPTY_1")
                .qrToken("QR_EMPTY_1")
                .checkinFlg(false)
                .build();
        entryMapper.insert(p1);

        List<RankingEntry> ranking = rankingService.getRanking(tournament.getId());
        assertTrue(ranking.isEmpty(), "Ranking should be empty when no players are checked in");
    }
}
