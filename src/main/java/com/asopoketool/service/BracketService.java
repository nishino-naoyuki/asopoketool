package com.asopoketool.service;

import com.asopoketool.mapper.BracketMapper;
import com.asopoketool.mapper.TournamentMapper;
import com.asopoketool.model.BracketMatch;
import com.asopoketool.model.BracketTournament;
import com.asopoketool.model.RankingEntry;
import com.asopoketool.model.Tournament;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.asopoketool.mapper.TournamentRoundMapper;
import com.asopoketool.model.TournamentRound;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BracketService {

    @Autowired
    private BracketMapper bracketMapper;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private TournamentRoundMapper roundMapper;

    @Transactional
    public void generateBrackets(Long tournamentId) {
        Tournament tournament = tournamentMapper.findById(tournamentId);
        if (tournament == null) {
            throw new IllegalArgumentException("大会が見つかりません。");
        }

        // Clean up any existing brackets
        bracketMapper.deleteByTournamentId(tournamentId);
        roundMapper.deleteBracketsRoundsByTournamentId(tournamentId);

        // Fetch standings
        List<RankingEntry> standings = rankingService.getRanking(tournamentId);
        // Exclude dropouts for bracket generation
        standings = standings.stream().filter(s -> !s.isDropout()).collect(java.util.stream.Collectors.toList());

        if (standings.isEmpty()) {
            return;
        }

        int groupSize = tournament.getBracketGroupSize();
        int totalPlayers = standings.size();
        int totalGroups = (int) Math.ceil((double) totalPlayers / groupSize);

        for (int g = 0; g < totalGroups; g++) {
            int fromIdx = g * groupSize;
            int toIdx = Math.min(fromIdx + groupSize, totalPlayers);

            List<RankingEntry> groupStandings = standings.subList(fromIdx, toIdx);
            
            // Create Bracket Group
            BracketTournament bt = BracketTournament.builder()
                    .tournamentId(tournamentId)
                    .groupNumber(g + 1)
                    .groupName("グループ " + (char) ('A' + g) + " (" + (fromIdx + 1) + "位～" + toIdx + "位)")
                    .rankFrom(fromIdx + 1)
                    .rankTo(toIdx)
                    .build();
            bracketMapper.insertBracketTournament(bt);

            // Generate initial round matches (Round 1: Quarterfinals / QF if groupSize=8, etc.)
            // We determine how many matches are needed based on next power of 2
            int n = groupStandings.size();
            int nextPowerOfTwo = 2;
            while (nextPowerOfTwo < n) {
                nextPowerOfTwo *= 2;
            }

            int numByes = nextPowerOfTwo - n;
            int numMatches = nextPowerOfTwo / 2;

            // Round 1 is QF (1), Round 2 is SF (2), Round 3 is Final (3) for standard 8-player bracket
            int currentRound = 1;

            // Let's create placeholders for all rounds of the bracket
            // To make advancing players easy, we insert empty matches for next rounds
            Map<String, BracketMatch> roundMatchMap = new HashMap<>();
            
            int roundCount = (int) (Math.log(nextPowerOfTwo) / Math.log(2));
            for (int r = 1; r <= roundCount; r++) {
                // Create TournamentRound for 100 + r (bracket rounds)
                if (roundMapper.findByTournamentAndRound(tournamentId, 100 + r) == null) {
                    TournamentRound tr = TournamentRound.builder()
                            .tournamentId(tournamentId)
                            .roundNumber(100 + r)
                            .status("IN_PROGRESS")
                            .startedAt(LocalDateTime.now())
                            .build();
                    roundMapper.insert(tr);
                }

                int matchesInRound = nextPowerOfTwo / (int) Math.pow(2, r);
                for (int m = 1; m <= matchesInRound; m++) {
                    BracketMatch bm = BracketMatch.builder()
                            .bracketTournamentId(bt.getId())
                            .roundNumber(r)
                            .matchNumber(m)
                            .player1EntryId(null)
                            .player2EntryId(null)
                            .winnerEntryId(null)
                            .isBye(false)
                            .build();
                    bracketMapper.insertBracketMatch(bm);
                    roundMatchMap.put(r + "_" + m, bm);
                }
            }

            // Fill Round 1 matches with players
            // Standard seed pairing: 1 vs 8, 4 vs 5, 3 vs 6, 2 vs 7
            // For general size, we pair seed i with (nextPowerOfTwo - i + 1)
            List<Long> seededIds = new ArrayList<>();
            for (RankingEntry re : groupStandings) {
                seededIds.add(re.getEntryId());
            }
            // Add nulls for BYEs
            for (int i = 0; i < numByes; i++) {
                seededIds.add(null);
            }

            // Re-order to classic tournament tree seeds:
            // For size 8: seeds [1, 8, 5, 4, 3, 6, 7, 2] corresponding to matches 1, 2, 3, 4
            List<Long> bracketOrder = getBracketSeedingOrder(nextPowerOfTwo, seededIds);

            for (int m = 1; m <= numMatches; m++) {
                Long p1 = bracketOrder.get((m - 1) * 2);
                Long p2 = bracketOrder.get((m - 1) * 2 + 1);

                BracketMatch bm = roundMatchMap.get("1_" + m);
                bm.setPlayer1EntryId(p1);
                bm.setPlayer2EntryId(p2);

                if (p2 == null && p1 != null) {
                    // BYE match
                    bm.setWinnerEntryId(p1);
                    bm.setBye(true);
                    bracketMapper.updateBracketMatchWinner(bm.getId(), p1);
                    
                    // Auto-advance to Round 2
                    advanceWinner(bm, p1);
                } else if (p1 == null && p2 == null) {
                    // Double BYE (should not happen, but for safety)
                    bm.setBye(true);
                } else {
                    bracketMapper.updateBracketMatchWinner(bm.getId(), null); // resets winner
                }

                // Update Round 1 match details in database
                bracketMapper.updateBracketMatch(bm);
            }
        }
    }

    private List<Long> getBracketSeedingOrder(int size, List<Long> seededIds) {
        List<Integer> order = getSeedOrder(size);
        List<Long> result = new ArrayList<>();
        for (int index : order) {
            if (index - 1 < seededIds.size()) {
                result.add(seededIds.get(index - 1));
            } else {
                result.add(null);
            }
        }
        return result;
    }

    private List<Integer> getSeedOrder(int size) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        while (list.size() < size) {
            List<Integer> nextList = new ArrayList<>();
            int target = list.size() * 2 + 1;
            for (int i = 0; i < list.size(); i++) {
                int seed = list.get(i);
                if (i % 2 == 0) {
                    nextList.add(seed);
                    nextList.add(target - seed);
                } else {
                    nextList.add(target - seed);
                    nextList.add(seed);
                }
            }
            list = nextList;
        }
        return list;
    }

    @Transactional
    public void registerBracketResult(Long bracketMatchId, Long winnerEntryId) {
        BracketMatch match = bracketMapper.findMatchById(bracketMatchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found");
        }

        bracketMapper.updateBracketMatchWinner(bracketMatchId, winnerEntryId);
        match.setWinnerEntryId(winnerEntryId);

        advanceWinner(match, winnerEntryId);
    }

    private void advanceWinner(BracketMatch match, Long winnerEntryId) {
        // Find next match in the tree
        int nextRound = match.getRoundNumber() + 1;
        int nextMatchNum = (match.getMatchNumber() + 1) / 2;

        List<BracketMatch> groupMatches = bracketMapper.findMatchesByBracketId(match.getBracketTournamentId());
        BracketMatch nextMatch = null;
        for (BracketMatch m : groupMatches) {
            if (m.getRoundNumber() == nextRound && m.getMatchNumber() == nextMatchNum) {
                nextMatch = m;
                break;
            }
        }

        if (nextMatch != null) {
            // Assign to player 1 or player 2 of next match
            if (match.getMatchNumber() % 2 != 0) {
                nextMatch.setPlayer1EntryId(winnerEntryId);
            } else {
                nextMatch.setPlayer2EntryId(winnerEntryId);
            }

            bracketMapper.updateBracketMatch(nextMatch);
        }
    }

    public List<BracketTournament> getBracketsByTournament(Long tournamentId) {
        List<BracketTournament> brackets = bracketMapper.findByTournamentId(tournamentId);
        for (BracketTournament bt : brackets) {
            bt.setMatches(bracketMapper.findMatchesByBracketId(bt.getId()));
        }
        return brackets;
    }
}
