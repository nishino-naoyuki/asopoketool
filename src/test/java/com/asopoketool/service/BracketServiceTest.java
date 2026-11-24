package com.asopoketool.service;

import com.asopoketool.mapper.*;
import com.asopoketool.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BracketServiceTest {

    @Autowired
    private BracketService bracketService;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private EntryMapper entryMapper;

    private Tournament createTestTournament(String name, int groupSize) {
        Tournament tournament = Tournament.builder()
                .name(name)
                .heldDate(LocalDate.now())
                .capacity(16)
                .totalRounds(3)
                .bracketGroupSize(groupSize)
                .status("IN_PROGRESS")
                .build();
        tournamentMapper.insert(tournament);
        return tournament;
    }

    private List<Entry> createEntries(Tournament tournament, int count) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Entry entry = Entry.builder()
                    .tournamentId(tournament.getId())
                    .playerName("Player_" + i)
                    .sessionToken("SESS_TEST_" + tournament.getId() + "_" + i)
                    .qrToken("QR_TEST_" + tournament.getId() + "_" + i)
                    .checkinFlg(true)
                    .checkinAt(LocalDateTime.now())
                    .dropoutFlg(false)
                    .manualEntryFlg(true)
                    .build();
            entryMapper.insert(entry);
            entries.add(entry);
        }
        return entries;
    }

    @Test
    public void test5PlayersBracket_ByeHandlingAndAutoAdvance() {
        Tournament t = createTestTournament("5PlayersTournament", 8);
        List<Entry> entries = createEntries(t, 5);

        // Generate brackets
        bracketService.generateBrackets(t.getId());

        List<BracketTournament> brackets = bracketService.getBracketsByTournament(t.getId());
        assertEquals(1, brackets.size());

        BracketTournament bt = brackets.get(0);
        List<BracketMatch> matches = bt.getMatches();
        assertNotNull(matches);

        // Round 1 matches (QF 1 to 4)
        BracketMatch qf1 = matches.stream().filter(m -> m.getRoundNumber() == 1 && m.getMatchNumber() == 1).findFirst().orElse(null);
        BracketMatch qf2 = matches.stream().filter(m -> m.getRoundNumber() == 1 && m.getMatchNumber() == 2).findFirst().orElse(null);
        BracketMatch qf3 = matches.stream().filter(m -> m.getRoundNumber() == 1 && m.getMatchNumber() == 3).findFirst().orElse(null);
        BracketMatch qf4 = matches.stream().filter(m -> m.getRoundNumber() == 1 && m.getMatchNumber() == 4).findFirst().orElse(null);

        assertNotNull(qf1);
        assertNotNull(qf2);
        assertNotNull(qf3);
        assertNotNull(qf4);

        // QF1: Seed 1 (entries[0]) vs Bye (null) -> p1=Seed1, p2=null, isBye=true, winner=Seed1
        assertEquals(entries.get(0).getId(), qf1.getPlayer1EntryId());
        assertNull(qf1.getPlayer2EntryId());
        assertTrue(qf1.isBye());
        assertEquals(entries.get(0).getId(), qf1.getWinnerEntryId());

        // QF2: Seed 5 (entries[4]) vs Seed 4 (entries[3]) -> isBye=false, winner=null
        assertEquals(entries.get(4).getId(), qf2.getPlayer1EntryId());
        assertEquals(entries.get(3).getId(), qf2.getPlayer2EntryId());
        assertFalse(qf2.isBye());
        assertNull(qf2.getWinnerEntryId());

        // QF3: Seed 3 (entries[2]) vs Bye (null) -> p1=Seed3, p2=null, isBye=true, winner=Seed3
        assertEquals(entries.get(2).getId(), qf3.getPlayer1EntryId());
        assertNull(qf3.getPlayer2EntryId());
        assertTrue(qf3.isBye());
        assertEquals(entries.get(2).getId(), qf3.getWinnerEntryId());

        // QF4: Bye (null) vs Seed 2 (entries[1]) -> p1=null, p2=Seed2, isBye=true, winner=Seed2
        assertNull(qf4.getPlayer1EntryId());
        assertEquals(entries.get(1).getId(), qf4.getPlayer2EntryId());
        assertTrue(qf4.isBye());
        assertEquals(entries.get(1).getId(), qf4.getWinnerEntryId());

        // Round 2 matches (SF 1 and 2)
        BracketMatch sf1 = matches.stream().filter(m -> m.getRoundNumber() == 2 && m.getMatchNumber() == 1).findFirst().orElse(null);
        BracketMatch sf2 = matches.stream().filter(m -> m.getRoundNumber() == 2 && m.getMatchNumber() == 2).findFirst().orElse(null);

        assertNotNull(sf1);
        assertNotNull(sf2);

        // SF1: QF1 winner (Seed 1) auto-advanced to p1, QF2 winner not yet decided -> p2=null
        assertEquals(entries.get(0).getId(), sf1.getPlayer1EntryId());
        assertNull(sf1.getPlayer2EntryId());

        // SF2: QF3 winner (Seed 3) auto-advanced to p1, QF4 winner (Seed 2) auto-advanced to p2!
        assertEquals(entries.get(2).getId(), sf2.getPlayer1EntryId());
        assertEquals(entries.get(1).getId(), sf2.getPlayer2EntryId());
    }

    @Test
    public void test6PlayersBracket_ByeHandlingAndAutoAdvance() {
        Tournament t = createTestTournament("6PlayersTournament", 8);
        List<Entry> entries = createEntries(t, 6);

        // Generate brackets
        bracketService.generateBrackets(t.getId());

        List<BracketTournament> brackets = bracketService.getBracketsByTournament(t.getId());
        BracketTournament bt = brackets.get(0);
        List<BracketMatch> matches = bt.getMatches();

        BracketMatch qf1 = matches.stream().filter(m -> m.getRoundNumber() == 1 && m.getMatchNumber() == 1).findFirst().orElse(null);
        BracketMatch qf2 = matches.stream().filter(m -> m.getRoundNumber() == 1 && m.getMatchNumber() == 2).findFirst().orElse(null);
        BracketMatch qf3 = matches.stream().filter(m -> m.getRoundNumber() == 1 && m.getMatchNumber() == 3).findFirst().orElse(null);
        BracketMatch qf4 = matches.stream().filter(m -> m.getRoundNumber() == 1 && m.getMatchNumber() == 4).findFirst().orElse(null);

        // QF1: Seed 1 vs Bye -> isBye=true, winner=Seed 1
        assertTrue(qf1.isBye());
        assertEquals(entries.get(0).getId(), qf1.getWinnerEntryId());

        // QF2: Seed 5 vs Seed 4 -> isBye=false
        assertFalse(qf2.isBye());
        assertNull(qf2.getWinnerEntryId());

        // QF3: Seed 3 vs Seed 6 -> isBye=false
        assertFalse(qf3.isBye());
        assertNull(qf3.getWinnerEntryId());

        // QF4: Bye vs Seed 2 -> isBye=true, winner=Seed 2
        assertTrue(qf4.isBye());
        assertEquals(entries.get(1).getId(), qf4.getWinnerEntryId());

        // Round 2
        BracketMatch sf1 = matches.stream().filter(m -> m.getRoundNumber() == 2 && m.getMatchNumber() == 1).findFirst().orElse(null);
        BracketMatch sf2 = matches.stream().filter(m -> m.getRoundNumber() == 2 && m.getMatchNumber() == 2).findFirst().orElse(null);

        assertEquals(entries.get(0).getId(), sf1.getPlayer1EntryId());
        assertNull(sf1.getPlayer2EntryId());

        assertNull(sf2.getPlayer1EntryId());
        assertEquals(entries.get(1).getId(), sf2.getPlayer2EntryId());
    }

    @Test
    public void test8PlayersBracket_NoByes() {
        Tournament t = createTestTournament("8PlayersTournament", 8);
        List<Entry> entries = createEntries(t, 8);

        bracketService.generateBrackets(t.getId());

        List<BracketTournament> brackets = bracketService.getBracketsByTournament(t.getId());
        BracketTournament bt = brackets.get(0);
        List<BracketMatch> matches = bt.getMatches();

        List<BracketMatch> qfMatches = new ArrayList<>();
        for (BracketMatch m : matches) {
            if (m.getRoundNumber() == 1) {
                qfMatches.add(m);
                assertFalse(m.isBye());
                assertNull(m.getWinnerEntryId());
                assertNotNull(m.getPlayer1EntryId());
                assertNotNull(m.getPlayer2EntryId());
            }
        }
        assertEquals(4, qfMatches.size());
    }
}
