package com.asopoketool.service;

import com.asopoketool.mapper.*;
import com.asopoketool.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SwissDrawService {

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private TournamentRoundMapper roundMapper;

    @Autowired
    private MatchGameMapper matchGameMapper;

    @Transactional
    public List<MatchGame> generateMatching(Long tournamentId) {
        Tournament tournament = tournamentMapper.findById(tournamentId);
        if (tournament == null) {
            throw new IllegalArgumentException("大会が見つかりません。");
        }

        int nextRoundNumber = tournament.getCurrentRound() + 1;
        if (nextRoundNumber > tournament.getTotalRounds()) {
            throw new IllegalStateException("設定された最大ラウンド数に達しています。");
        }

        return generateMatchingForRound(tournament, nextRoundNumber, null);
    }

    @Transactional
    public List<MatchGame> rematchRound(Long tournamentId, int roundNumber) {
        Tournament tournament = tournamentMapper.findById(tournamentId);
        if (tournament == null) {
            throw new IllegalArgumentException("大会が見つかりません。");
        }

        TournamentRound round = roundMapper.findByTournamentAndRound(tournamentId, roundNumber);
        if (round == null) {
            throw new IllegalArgumentException("指定されたラウンドが見つかりません。");
        }

        // Check if there are any results registered (excluding BYE)
        List<MatchGame> existingMatches = matchGameMapper.findByRoundId(round.getId());
        for (MatchGame m : existingMatches) {
            if (!m.isBye()) {
                MatchResult r = matchResultMapper.findByMatchId(m.getId());
                if (r != null) {
                    throw new IllegalStateException("すでに登録されている試合結果があるため、リマッチできません。");
                }
            }
        }

        // Delete existing matches (cascade deletes bye results)
        matchGameMapper.deleteByRoundId(round.getId());

        return generateMatchingForRound(tournament, roundNumber, round);
    }

    private List<MatchGame> generateMatchingForRound(Tournament tournament, int roundNumber, TournamentRound existingRound) {
        Long tournamentId = tournament.getId();

        // 1. Get all active checked-in players (checkin=1, dropout=0)
        List<Entry> activeEntries = entryMapper.findActiveByTournamentId(tournamentId);
        if (activeEntries.size() < 2) {
            throw new IllegalStateException("アクティブな参加者が2名未満のためマッチングを生成できません。");
        }

        // 2. Fetch past matches in this tournament to avoid rematches
        List<MatchGame> pastMatches = matchGameMapper.findAllByTournamentId(tournamentId);
        Set<String> pastPairs = new HashSet<>();
        for (MatchGame pm : pastMatches) {
            if (pm.getPlayer2EntryId() != null) {
                long id1 = Math.min(pm.getPlayer1EntryId(), pm.getPlayer2EntryId());
                long id2 = Math.max(pm.getPlayer1EntryId(), pm.getPlayer2EntryId());
                pastPairs.add(id1 + "_" + id2);
            }
        }

        // 3. Collect win points for each player
        Map<Long, Integer> winPoints = new HashMap<>();
        for (Entry e : activeEntries) {
            winPoints.put(e.getId(), getWinPoints(e.getId(), tournamentId, tournament.getWinPoint()));
        }

        // 4. Group players by win points
        Map<Integer, List<Entry>> groups = new HashMap<>();
        for (Entry e : activeEntries) {
            int pts = winPoints.get(e.getId());
            groups.computeIfAbsent(pts, k -> new ArrayList<>()).add(e);
        }

        // Sorted win point groups descending
        List<Integer> sortedScores = new ArrayList<>(groups.keySet());
        sortedScores.sort(Collections.reverseOrder());

        // Flatten players list, but maintain groupings to prioritize matching within the same win group
        List<Entry> pool = new ArrayList<>();
        for (int score : sortedScores) {
            List<Entry> groupMembers = groups.get(score);
            Collections.shuffle(groupMembers); // Randomize within group
            pool.addAll(groupMembers);
        }

        // 5. Check if total active players is odd -> assign BYE
        Entry byePlayer = null;
        if (pool.size() % 2 != 0) {
            // Find a player with lowest winPoints who has never had a BYE
            List<Entry> sortedForBye = new ArrayList<>(pool);
            sortedForBye.sort(Comparator.comparingInt((Entry e) -> winPoints.get(e.getId()))
                                         .thenComparing(Entry::getId)); // Stable sort
            
            for (Entry candidate : sortedForBye) {
                if (!hasHadBye(candidate.getId(), tournamentId, pastMatches)) {
                    byePlayer = candidate;
                    pool.remove(candidate);
                    break;
                }
            }

            // Fallback in case everyone had a BYE (unlikely, but for safety)
            if (byePlayer == null && !pool.isEmpty()) {
                byePlayer = pool.remove(pool.size() - 1);
            }
        }

        // 6. Pairing algorithm: Global optimal matching using backtracking on sorted player pool
        // Players in 'pool' are already sorted by win points (descending), so adjacent players
        // in the pool have closest win scores. The backtracking will naturally prefer
        // same-wingroup pairs as the first candidate, but will try all combinations to
        // find the globally optimal (no rematches) solution.
        List<MatchGame> generatedMatches = new ArrayList<>();
        List<Entry> bestUnmatched = new ArrayList<>(pool);
        List<MatchGame> bestMatches = new ArrayList<>();
        globalOptimalPairing(pool, pastPairs, new ArrayList<>(), bestUnmatched, bestMatches);
        generatedMatches.addAll(bestMatches);

        // Handle any remaining unmatched players (due to rematch constraints)
        List<Entry> unmatched = new ArrayList<>(bestUnmatched);
        if (!unmatched.isEmpty()) {
            if (unmatched.size() >= 2) {
                // Last resort: pair remaining players ignoring rematch constraints
                generateRelaxedPairs(unmatched, generatedMatches);
            } else {
                // 1 player unmatched in an even-total scenario → give BYE (edge case)
                if (byePlayer == null) {
                    byePlayer = unmatched.get(0);
                }
            }
        }

        // Sanity guard: even player count must never have a BYE
        boolean isOdd = (activeEntries.size() % 2 != 0);
        if (!isOdd && (byePlayer != null)) {
            generatedMatches.clear();
            byePlayer = null;
            generateRelaxedPairs(activeEntries, generatedMatches);
        }

        // 7. Create/Reuse Round record
        TournamentRound round = existingRound;
        if (round == null) {
            round = TournamentRound.builder()
                    .tournamentId(tournamentId)
                    .roundNumber(roundNumber)
                    .status("IN_PROGRESS")
                    .startedAt(LocalDateTime.now())
                    .build();
            roundMapper.insert(round);
        }

        // 8. Assign tables and Insert matches into DB
        int tableNumber = 1;
        for (MatchGame match : generatedMatches) {
            match.setRoundId(round.getId());
            match.setTableNumber(tableNumber++);
            matchGameMapper.insert(match);
        }

        // If there was a BYE, insert the BYE match (table = 0 or last table, let's put it as tableNumber)
        if (byePlayer != null) {
            MatchGame byeMatch = MatchGame.builder()
                    .roundId(round.getId())
                    .tableNumber(tableNumber)
                    .player1EntryId(byePlayer.getId())
                    .player2EntryId(null)
                    .isBye(true)
                    .build();
            matchGameMapper.insert(byeMatch);
            
            // Automatically record win for the BYE player in match_result
            MatchResult byeResult = MatchResult.builder()
                    .matchId(byeMatch.getId())
                    .winnerEntryId(byePlayer.getId())
                    .registeredBy("ADMIN")
                    .build();
            matchResultMapper.insert(byeResult);
        }

        // Update current round in tournament (only if it's a new round)
        if (existingRound == null) {
            tournamentMapper.updateCurrentRound(tournamentId, roundNumber);
        }

        return matchGameMapper.findByRoundId(round.getId());
    }

    @Autowired
    private MatchResultMapper matchResultMapper;

    /**
     * Global optimal pairing via backtracking with pruning.
     * The pool is pre-sorted by win points descending, so adjacent players are the
     * "best" (closest win-score) candidates.  The first player in the pool (highest
     * win points) tries all possible partners in order; for each successful pair the
     * rest of the pool is recursively solved.  If no partner is found, the player
     * is left as the last floater.  We track the globally best (fewest unmatched)
     * solution seen so far and prune branches that cannot improve it.
     *
     * Time complexity: O(N!) worst case, but with pruning effectively O(N^2) for
     * typical Swiss draws (N ≤ 45) because rematches are rare in early rounds and
     * the sorted order means the first candidate is almost always accepted.
     */
    private void globalOptimalPairing(List<Entry> pool,
                                       Set<String> pastPairs,
                                       List<MatchGame> currentMatches,
                                       List<Entry> bestUnmatched,
                                       List<MatchGame> bestMatches) {
        // Pruning: even if every remaining player can be paired, we can't improve
        if (currentMatches.size() + pool.size() / 2 <= bestMatches.size()) {
            return;
        }

        // Base case: 0 or 1 player left
        if (pool.size() < 2) {
            if (pool.size() < bestUnmatched.size()) {
                bestUnmatched.clear();
                bestUnmatched.addAll(pool);
                bestMatches.clear();
                bestMatches.addAll(currentMatches);
            }
            return;
        }

        Entry p1 = pool.get(0);
        boolean matchedAtLeastOnce = false;

        // Try to pair p1 with each other player in pool order
        for (int i = 1; i < pool.size(); i++) {
            Entry p2 = pool.get(i);
            long lo = Math.min(p1.getId(), p2.getId());
            long hi = Math.max(p1.getId(), p2.getId());
            String key = lo + "_" + hi;

            if (!pastPairs.contains(key)) {
                matchedAtLeastOnce = true;
                MatchGame match = MatchGame.builder()
                        .player1EntryId(p1.getId())
                        .player2EntryId(p2.getId())
                        .isBye(false)
                        .build();
                currentMatches.add(match);

                // Build next pool without p1 and p2
                List<Entry> next = new ArrayList<>();
                for (Entry e : pool) {
                    if (!e.getId().equals(p1.getId()) && !e.getId().equals(p2.getId())) {
                        next.add(e);
                    }
                }
                globalOptimalPairing(next, pastPairs, currentMatches, bestUnmatched, bestMatches);
                currentMatches.remove(match);
            }
        }

        // If p1 could not be matched with anyone (all pairs are rematches),
        // skip p1 (it will become a floater) and try to pair the rest.
        if (!matchedAtLeastOnce) {
            List<Entry> next = new ArrayList<>();
            for (Entry e : pool) {
                if (!e.getId().equals(p1.getId())) next.add(e);
            }
            globalOptimalPairing(next, pastPairs, currentMatches, bestUnmatched, bestMatches);
        }
    }

    private void generateRelaxedPairs(List<Entry> pool, List<MatchGame> result) {
        // Pair them up sequentially, ignoring rematch constraints (last-resort fallback)
        for (int i = 0; i + 1 < pool.size(); i += 2) {
            result.add(MatchGame.builder()
                    .player1EntryId(pool.get(i).getId())
                    .player2EntryId(pool.get(i + 1).getId())
                    .isBye(false)
                    .build());
        }
    }

    private int getWinPoints(Long entryId, Long tournamentId, int winPoint) {
        // Count how many matches this player won in the tournament
        List<MatchGame> matches = matchGameMapper.findByEntryId(entryId);
        int wins = 0;
        for (MatchGame m : matches) {
            MatchResult res = matchResultMapper.findByMatchId(m.getId());
            if (res != null && entryId.equals(res.getWinnerEntryId())) {
                wins++;
            }
        }
        return wins * winPoint;
    }

    private boolean hasHadBye(Long entryId, Long tournamentId, List<MatchGame> pastMatches) {
        for (MatchGame m : pastMatches) {
            if (m.isBye() && entryId.equals(m.getPlayer1EntryId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isOnlyOneUndefeated(Long tournamentId) {
        Tournament tournament = tournamentMapper.findById(tournamentId);
        int currentRound = tournament.getCurrentRound();
        if (currentRound == 0) {
            return false;
        }

        List<Entry> activeEntries = entryMapper.findActiveByTournamentId(tournamentId);
        int winPointVal = tournament.getWinPoint();
        int expectedPoints = currentRound * winPointVal;

        long undefeatedCount = activeEntries.stream()
                .filter(e -> getWinPoints(e.getId(), tournamentId, winPointVal) == expectedPoints)
                .count();

        return undefeatedCount == 1;
    }
}
