package com.asopoketool.service;

import com.asopoketool.mapper.*;
import com.asopoketool.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SwissDrawServiceTest {

    @Autowired
    private SwissDrawService swissDrawService;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private TournamentRoundMapper roundMapper;

    @Autowired
    private MatchGameMapper matchGameMapper;

    @Autowired
    private MatchResultMapper matchResultMapper;

    @Test
    public void testSwissSimulations() {
        // Run 10 validation patterns requested by user
        runSwissSimulation(4, 3);   // 1. 4 players (even, min)
        runSwissSimulation(7, 3);   // 2. 7 players (under 10, odd)
        runSwissSimulation(10, 4);  // 3. 10 players (under 10, even)
        runSwissSimulation(15, 4);  // 4. 15 players (under 20, odd)
        runSwissSimulation(20, 5);  // 5. 20 players (under 20, even)
        runSwissSimulation(25, 5);  // 6. 25 players (under 30, odd)
        runSwissSimulation(30, 5);  // 7. 30 players (under 30, even)
        runSwissSimulation(35, 5);  // 8. 35 players (under 40, odd)
        runSwissSimulation(40, 6);  // 9. 40 players (under 40, even)
        runSwissSimulation(45, 6);  // 10. 45 players (over 40, odd)
    }

    private void runSwissSimulation(int numPlayers, int numRounds) {
        System.out.println("=== STARTING SWISS SIMULATION: " + numPlayers + " players, " + numRounds + " rounds ===");

        // 1. Create Tournament
        Tournament tournament = Tournament.builder()
                .name("Simulation_" + numPlayers + "P_" + numRounds + "R")
                .heldDate(LocalDate.now())
                .capacity(numPlayers)
                .totalRounds(numRounds)
                .winPoint(3)
                .losePoint(0)
                .participationPoint(1)
                .bracketGroupSize(8)
                .status("ENTRY")
                .description("Automated test simulation")
                .venue("JUnit Test")
                .build();
        tournamentMapper.insert(tournament);
        Long tId = tournament.getId();

        // 2. Insert Entries
        List<Long> entryIds = new ArrayList<>();
        for (int i = 1; i <= numPlayers; i++) {
            Entry entry = Entry.builder()
                    .tournamentId(tId)
                    .playerName("Player_" + i)
                    .accountId(null)
                    .sessionToken("SESS_SIM_" + i)
                    .qrToken("QR_SIM_" + tId + "_" + i)
                    .checkinFlg(true)
                    .checkinAt(LocalDateTime.now())
                    .dropoutFlg(false)
                    .manualEntryFlg(true)
                    .build();
            entryMapper.insert(entry);
            entryIds.add(entry.getId());
        }

        // Set status to IN_PROGRESS to start swiss rounds
        tournamentMapper.updateStatus(tId, "IN_PROGRESS");

        // Keep track of past pairs to verify rematch prevention
        // format: "minId_maxId"
        Set<String> pastPairs = new HashSet<>();
        // Keep track of who got a BYE to verify no duplicate BYEs
        Set<Long> playersWithBye = new HashSet<>();

        // 3. Loop through rounds
        for (int r = 1; r <= numRounds; r++) {
            // Generate Matching
            List<MatchGame> matches = swissDrawService.generateMatching(tId);
            assertNotNull(matches, "Matches should be generated for round " + r);

            int expectedMatches = numPlayers / 2;
            boolean isOdd = (numPlayers % 2 != 0);

            // Verify count of match games
            if (isOdd) {
                assertEquals(expectedMatches + 1, matches.size(), "Odd count of players requires 1 extra BYE match");
            } else {
                assertEquals(expectedMatches, matches.size(), "Even count of players requires exact matches");
            }

            int byeCount = 0;
            for (MatchGame match : matches) {
                if (match.isBye()) {
                    byeCount++;
                    // Verify BYE duplicates
                    Long player = match.getPlayer1EntryId();
                    assertNotNull(player, "BYE match must have a player assigned");
                    assertFalse(playersWithBye.contains(player), "Player " + player + " got a duplicate BYE in round " + r);
                    playersWithBye.add(player);
                } else {
                    // Verify rematch prevention
                    Long p1 = match.getPlayer1EntryId();
                    Long p2 = match.getPlayer2EntryId();
                    assertNotNull(p1, "Player 1 should not be null");
                    assertNotNull(p2, "Player 2 should not be null");

                    long id1 = Math.min(p1, p2);
                    long id2 = Math.max(p1, p2);
                    String pairKey = id1 + "_" + id2;

                    assertFalse(pastPairs.contains(pairKey), "Rematch detected: " + p1 + " vs " + p2 + " in round " + r);
                    pastPairs.add(pairKey);

                    // Register Random Winner Result
                    Long winner = (Math.random() < 0.5) ? p1 : p2;
                    MatchResult result = MatchResult.builder()
                            .matchId(match.getId())
                            .winnerEntryId(winner)
                            .registeredBy("ADMIN")
                            .build();
                    matchResultMapper.insert(result);
                }
            }

            if (isOdd) {
                assertEquals(1, byeCount, "Odd player count must have exactly 1 BYE match");
            } else {
                assertEquals(0, byeCount, "Even player count must have 0 BYE matches");
            }

            // Finish current round
            TournamentRound round = roundMapper.findByTournamentAndRound(tId, r);
            assertNotNull(round, "Round record should exist");
            roundMapper.finish(round.getId(), LocalDateTime.now());
        }

        System.out.println("=== SIMULATION COMPLETED SUCCESSFULLY ===");
    }
}
