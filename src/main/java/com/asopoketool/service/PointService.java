package com.asopoketool.service;

import com.asopoketool.mapper.BracketMapper;
import com.asopoketool.mapper.EntryMapper;
import com.asopoketool.mapper.PointMapper;
import com.asopoketool.mapper.PrizePointMapper;
import com.asopoketool.mapper.TournamentMapper;
import com.asopoketool.model.BracketMatch;
import com.asopoketool.model.BracketTournament;
import com.asopoketool.model.Entry;
import com.asopoketool.model.PointHistory;
import com.asopoketool.model.PrizePointSetting;
import com.asopoketool.model.Tournament;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PointService {

    @Autowired
    private PointMapper pointMapper;

    @Autowired
    private PrizePointMapper prizePointMapper;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private BracketMapper bracketMapper;

    @Transactional
    public void awardPoints(Long tournamentId) {
        Tournament tournament = tournamentMapper.findById(tournamentId);
        if (tournament == null) {
            throw new IllegalArgumentException("Tournament not found");
        }

        List<Entry> entries = entryMapper.findByTournamentId(tournamentId);
        List<BracketTournament> brackets = bracketMapper.findByTournamentId(tournamentId);

        // Fetch matches for each bracket to resolve final standing
        Map<Long, Integer> finalRanks = new HashMap<>();
        resolveFinalStandings(brackets, finalRanks);

        int partPoint = tournament.getParticipationPoint();

        for (Entry entry : entries) {
            // Find final rank
            Integer rank = finalRanks.get(entry.getId());
            int prizePoint = 0;

            if (rank != null && rank <= 16) {
                PrizePointSetting setting = prizePointMapper.findByTournamentAndRank(tournamentId, rank);
                if (setting != null) {
                    prizePoint = setting.getPoint();
                }
            }

            int totalPoint = prizePoint + partPoint;

            // Record history
            PointHistory history = PointHistory.builder()
                    .tournamentId(tournamentId)
                    .accountId(entry.getAccountId())
                    .entryId(entry.getId())
                    .playerName(entry.getPlayerName())
                    .finalRank(rank)
                    .prizePoint(prizePoint)
                    .participationPoint(partPoint)
                    .totalPoint(totalPoint)
                    .build();
            pointMapper.insertHistory(history);

            // If entry is linked to an account, update cumulative points
            if (entry.getAccountId() != null) {
                pointMapper.upsertCumulativePoint(entry.getAccountId(), totalPoint);
            }
        }
    }

    private void resolveFinalStandings(List<BracketTournament> brackets, Map<Long, Integer> finalRanks) {
        for (BracketTournament bt : brackets) {
            List<BracketMatch> matches = bracketMapper.findMatchesByBracketId(bt.getId());
            if (matches.isEmpty()) {
                continue;
            }

            // Find group winner (Winner of the Final match)
            // Final match is the one with the highest roundNumber
            BracketMatch finalMatch = matches.stream()
                    .max(Comparator.comparingInt(BracketMatch::getRoundNumber))
                    .orElse(null);

            if (finalMatch != null && finalMatch.getWinnerEntryId() != null) {
                Long winnerId = finalMatch.getWinnerEntryId();
                Long runnerUpId = winnerId.equals(finalMatch.getPlayer1EntryId()) ? 
                        finalMatch.getPlayer2EntryId() : finalMatch.getPlayer1EntryId();

                // Group 1 gets rank 1 & 2
                // Group 2 (places 9-16) gets rank 9 & 10, etc.
                int baseRank = bt.getRankFrom();
                finalRanks.put(winnerId, baseRank); // Winner gets baseRank
                if (runnerUpId != null) {
                    finalRanks.put(runnerUpId, baseRank + 1); // Runner-up gets baseRank + 1
                }

                // Semifinal losers get baseRank + 2 & baseRank + 3 (approx)
                int sfRound = finalMatch.getRoundNumber() - 1;
                if (sfRound > 0) {
                    int sfLoserRank = baseRank + 2;
                    for (BracketMatch m : matches) {
                        if (m.getRoundNumber() == sfRound) {
                            Long loserId = getLoser(m);
                            if (loserId != null) {
                                finalRanks.put(loserId, sfLoserRank++);
                            }
                        }
                    }
                }
            }
        }
    }

    private Long getLoser(BracketMatch match) {
        if (match.getWinnerEntryId() == null) {
            return null;
        }
        if (match.getWinnerEntryId().equals(match.getPlayer1EntryId())) {
            return match.getPlayer2EntryId();
        }
        return match.getPlayer1EntryId();
    }
}
