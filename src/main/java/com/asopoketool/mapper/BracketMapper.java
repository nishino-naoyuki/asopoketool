package com.asopoketool.mapper;

import com.asopoketool.model.BracketMatch;
import com.asopoketool.model.BracketTournament;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BracketMapper {
    List<BracketTournament> findByTournamentId(Long tournamentId);
    BracketTournament findById(Long id);
    void insertBracketTournament(BracketTournament bt);
    void insertBracketMatch(BracketMatch bm);
    void updateBracketMatchWinner(@Param("id") Long id, @Param("winnerEntryId") Long winnerEntryId);
    void updateBracketMatch(BracketMatch bm);
    List<BracketMatch> findMatchesByBracketId(Long bracketTournamentId);
    void deleteByTournamentId(Long tournamentId);
    void deleteMatchesByTournamentId(Long tournamentId);
    BracketMatch findMatchById(Long id);
}
