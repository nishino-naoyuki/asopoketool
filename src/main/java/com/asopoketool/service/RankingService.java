package com.asopoketool.service;

import com.asopoketool.mapper.EntryMapper;
import com.asopoketool.mapper.MatchGameMapper;
import com.asopoketool.mapper.MatchResultMapper;
import com.asopoketool.mapper.TournamentMapper;
import com.asopoketool.mapper.TournamentRoundMapper;
import com.asopoketool.model.Entry;
import com.asopoketool.model.MatchGame;
import com.asopoketool.model.MatchResult;
import com.asopoketool.model.RankingEntry;
import com.asopoketool.model.Tournament;
import com.asopoketool.model.TournamentRound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankingService {

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private MatchGameMapper matchGameMapper;

    @Autowired
    private MatchResultMapper matchResultMapper;

    @Autowired
    private TournamentRoundMapper tournamentRoundMapper;

    public List<RankingEntry> getRanking(Long tournamentId) {
        Tournament tournament = tournamentMapper.findById(tournamentId);
        if (tournament == null) {
            return Collections.emptyList();
        }

        List<Entry> allEntries = entryMapper.findByTournamentId(tournamentId);
        List<MatchGame> allMatches = matchGameMapper.findAllByTournamentId(tournamentId);

        // Pre-build match results for quick lookup
        Map<Long, MatchResult> resultsMap = new HashMap<>();
        for (MatchGame m : allMatches) {
            MatchResult r = matchResultMapper.findByMatchId(m.getId());
            if (r != null) {
                resultsMap.put(m.getId(), r);
            }
        }

        // 1. Calculate basic matches played, wins, losses, winPoints for everyone
        Map<Long, Integer> winsMap = new HashMap<>();
        Map<Long, Integer> matchesPlayedMap = new HashMap<>();
        Map<Long, List<Long>> opponentsMap = new HashMap<>();

        for (Entry e : allEntries) {
            winsMap.put(e.getId(), 0);
            matchesPlayedMap.put(e.getId(), 0);
            opponentsMap.put(e.getId(), new ArrayList<>());
        }

        for (MatchGame m : allMatches) {
            MatchResult r = resultsMap.get(m.getId());
            boolean hasResult = (r != null);

            if (!hasResult && !m.isBye()) {
                continue;
            }

            // Player 1
            Long p1 = m.getPlayer1EntryId();
            if (p1 != null) {
                if (!m.isBye()) {
                    matchesPlayedMap.put(p1, matchesPlayedMap.getOrDefault(p1, 0) + 1);
                    if (m.getPlayer2EntryId() != null) {
                        opponentsMap.get(p1).add(m.getPlayer2EntryId());
                    }
                } else {
                    // BYE counts as a match played and a win, but has no actual opponent
                    matchesPlayedMap.put(p1, matchesPlayedMap.getOrDefault(p1, 0) + 1);
                }
                
                if (hasResult && p1.equals(r.getWinnerEntryId())) {
                    winsMap.put(p1, winsMap.getOrDefault(p1, 0) + 1);
                }
            }

            // Player 2
            Long p2 = m.getPlayer2EntryId();
            if (p2 != null && !m.isBye()) {
                matchesPlayedMap.put(p2, matchesPlayedMap.getOrDefault(p2, 0) + 1);
                opponentsMap.get(p2).add(p1);
                if (hasResult && p2.equals(r.getWinnerEntryId())) {
                    winsMap.put(p2, winsMap.getOrDefault(p2, 0) + 1);
                }
            }
        }

        // Fetch round numbers to map match_game to round numbers
        List<TournamentRound> rounds = tournamentRoundMapper.findByTournamentId(tournamentId);
        Map<Long, Integer> roundIdToNumberMap = rounds.stream()
                .collect(Collectors.toMap(TournamentRound::getId, TournamentRound::getRoundNumber, (a, b) -> a));

        // Tracks rounds each player actually participated in
        Map<Long, Set<Integer>> playedRoundsMap = new HashMap<>();
        for (Entry e : allEntries) {
            playedRoundsMap.put(e.getId(), new HashSet<>());
        }

        for (MatchGame m : allMatches) {
            Integer rNum = roundIdToNumberMap.get(m.getRoundId());
            if (rNum != null) {
                if (m.getPlayer1EntryId() != null) {
                    playedRoundsMap.get(m.getPlayer1EntryId()).add(rNum);
                }
                if (m.getPlayer2EntryId() != null && !m.isBye()) {
                    playedRoundsMap.get(m.getPlayer2EntryId()).add(rNum);
                }
            }
        }

        // Build base DTO list
        List<RankingEntry> rankingList = new ArrayList<>();
        int winPointVal = tournament.getWinPoint();
        int currentRound = tournament.getCurrentRound();

        for (Entry e : allEntries) {
            int wins = winsMap.get(e.getId());
            int actualPlayed = matchesPlayedMap.get(e.getId());
            int winPoints = wins * winPointVal;

            // Count rounds prior to (and including) currentRound that this player did not play in
            Set<Integer> playedRounds = playedRoundsMap.get(e.getId());
            int unplayedRounds = 0;
            for (int r = 1; r <= currentRound; r++) {
                if (!playedRounds.contains(r)) {
                    unplayedRounds++;
                }
            }

            int losses = (actualPlayed - wins) + unplayedRounds;
            int total = actualPlayed + unplayedRounds;

            RankingEntry re = RankingEntry.builder()
                    .entryId(e.getId())
                    .tournamentId(tournamentId)
                    .playerName(e.getPlayerName())
                    .accountId(e.getAccountId())
                    .wins(wins)
                    .losses(losses)
                    .totalMatches(total)
                    .winPoints(winPoints)
                    .isDropout(e.isDropoutFlg())
                    .build();
            rankingList.add(re);
        }

        // Map for quick DTO lookup
        Map<Long, RankingEntry> dtoMap = rankingList.stream()
                .collect(Collectors.toMap(RankingEntry::getEntryId, re -> re));

        // 2. Calculate OMW% (Opponent Match Win Percentage)
        // OMW% = average of opponents' win rates (each win rate floored at 33%)
        for (RankingEntry re : rankingList) {
            List<Long> opponents = opponentsMap.get(re.getEntryId());
            if (opponents.isEmpty()) {
                re.setOmwPercent(0.0);
                re.setOpponentWinPointsTotal(0);
                continue;
            }

            double totalMwp = 0.0;
            int oppWinPtsTotal = 0;
            for (Long oppId : opponents) {
                RankingEntry opp = dtoMap.get(oppId);
                if (opp != null) {
                    oppWinPtsTotal += opp.getWinPoints();
                    
                    // Opponent Win rate = wins / totalMatches
                    double mwp = 0.0;
                    if (opp.getTotalMatches() > 0) {
                        mwp = (double) opp.getWins() / opp.getTotalMatches();
                    }
                    // Apply floor (33% minimum)
                    mwp = Math.max(mwp, 0.33);
                    totalMwp += mwp;
                }
            }
            re.setOmwPercent(totalMwp / opponents.size());
            re.setOpponentWinPointsTotal(oppWinPtsTotal);
        }

        // 3. Calculate OOMW% (Opponent's Opponent Match Win Percentage)
        for (RankingEntry re : rankingList) {
            List<Long> opponents = opponentsMap.get(re.getEntryId());
            if (opponents.isEmpty()) {
                re.setOomwPercent(0.0);
                continue;
            }

            double totalOmw = 0.0;
            for (Long oppId : opponents) {
                RankingEntry opp = dtoMap.get(oppId);
                if (opp != null) {
                    totalOmw += opp.getOmwPercent();
                }
            }
            re.setOomwPercent(totalOmw / opponents.size());
        }

        // 4. Sorting with Tie-breakers
        rankingList.sort((r1, r2) -> {
            // Check dropouts: Non-dropouts are ranked higher than dropouts
            if (r1.isDropout() != r2.isDropout()) {
                return r1.isDropout() ? 1 : -1;
            }

            // 1st: Win points DESC
            if (r1.getWinPoints() != r2.getWinPoints()) {
                return Integer.compare(r2.getWinPoints(), r1.getWinPoints());
            }

            // 2nd: OMW% DESC
            if (Double.compare(r2.getOmwPercent(), r1.getOmwPercent()) != 0) {
                return Double.compare(r2.getOmwPercent(), r1.getOmwPercent());
            }

            // 3rd: Opponents win points total DESC
            if (r1.getOpponentWinPointsTotal() != r2.getOpponentWinPointsTotal()) {
                return Integer.compare(r2.getOpponentWinPointsTotal(), r1.getOpponentWinPointsTotal());
            }

            // 4th: OOMW% DESC
            if (Double.compare(r2.getOomwPercent(), r1.getOomwPercent()) != 0) {
                return Double.compare(r2.getOomwPercent(), r1.getOomwPercent());
            }

            // 5th: Direct match result (if they played each other)
            Long p1 = r1.getEntryId();
            Long p2 = r2.getEntryId();
            for (MatchGame m : allMatches) {
                if (!m.isBye() && ((p1.equals(m.getPlayer1EntryId()) && p2.equals(m.getPlayer2EntryId())) ||
                                  (p2.equals(m.getPlayer1EntryId()) && p1.equals(m.getPlayer2EntryId())))) {
                    MatchResult res = resultsMap.get(m.getId());
                    if (res != null) {
                        if (p1.equals(res.getWinnerEntryId())) return -1;
                        if (p2.equals(res.getWinnerEntryId())) return 1;
                    }
                }
            }

            // Keep index order / equal rank if completely identical
            return 0;
        });

        // 5. Assign Ranks
        int currentRank = 1;
        for (int i = 0; i < rankingList.size(); i++) {
            RankingEntry current = rankingList.get(i);
            if (i > 0) {
                RankingEntry prev = rankingList.get(i - 1);
                boolean sameRank = (current.isDropout() == prev.isDropout()) &&
                                   (current.getWinPoints() == prev.getWinPoints()) &&
                                   (Double.compare(current.getOmwPercent(), prev.getOmwPercent()) == 0) &&
                                   (current.getOpponentWinPointsTotal() == prev.getOpponentWinPointsTotal()) &&
                                   (Double.compare(current.getOomwPercent(), prev.getOomwPercent()) == 0);
                // Directly check if they had head to head tiebreaker too
                if (!sameRank) {
                    currentRank = i + 1;
                }
            }
            current.setRank(currentRank);
        }

        return rankingList;
    }
}
