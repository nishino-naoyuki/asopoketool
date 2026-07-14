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
    /** 対象アカウントが出場した全決勝T試合（勝者決定済み or BYE）を取得 */
    List<BracketMatch> findByAccountId(Long accountId);
    /** 大会IDから全決勝T試合を取得（優勝・準優勝判定用） */
    List<BracketMatch> findAllMatchesByTournamentId(Long tournamentId);
}
