package com.asopoketool.mapper;

import com.asopoketool.model.TournamentRound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TournamentRoundMapper {
    TournamentRound findById(Long id);
    TournamentRound findByTournamentAndRound(@Param("tournamentId") Long tournamentId, @Param("roundNumber") int roundNumber);
    List<TournamentRound> findByTournamentId(Long tournamentId);
    void insert(TournamentRound round);
    void finish(@Param("id") Long id, @Param("finishedAt") LocalDateTime finishedAt);

    @org.apache.ibatis.annotations.Delete("DELETE FROM tournament_round WHERE tournament_id = #{tournamentId}")
    void deleteByTournamentId(Long tournamentId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM tournament_round WHERE tournament_id = #{tournamentId} AND round_number >= 101")
    void deleteBracketsRoundsByTournamentId(Long tournamentId);
}
