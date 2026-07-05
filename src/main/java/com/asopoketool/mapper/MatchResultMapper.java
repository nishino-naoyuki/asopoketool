package com.asopoketool.mapper;

import com.asopoketool.model.MatchResult;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MatchResultMapper {
    MatchResult findByMatchId(Long matchId);
    void insert(MatchResult result);
    void update(MatchResult result);

    @org.apache.ibatis.annotations.Delete("DELETE FROM match_result WHERE match_id IN (SELECT id FROM match_game WHERE round_id IN (SELECT id FROM tournament_round WHERE tournament_id = #{tournamentId}))")
    void deleteByTournamentId(Long tournamentId);
}
