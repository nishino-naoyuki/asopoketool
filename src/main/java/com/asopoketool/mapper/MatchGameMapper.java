package com.asopoketool.mapper;

import com.asopoketool.model.MatchGame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MatchGameMapper {
    List<MatchGame> findByRoundId(Long roundId);
    MatchGame findById(Long id);
    List<MatchGame> findByEntryId(Long entryId);
    List<MatchGame> findAllByTournamentId(Long tournamentId);
    /** 対象アカウントが関わった全スイス式試合（結果登録済み or BYE）を取得 */
    List<MatchGame> findByAccountId(Long accountId);
    void insert(MatchGame matchGame);
    void batchInsert(List<MatchGame> matchGames);

    @org.apache.ibatis.annotations.Delete("DELETE FROM match_game WHERE round_id = #{roundId}")
    void deleteByRoundId(Long roundId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM match_game WHERE round_id IN (SELECT id FROM tournament_round WHERE tournament_id = #{tournamentId})")
    void deleteByTournamentId(Long tournamentId);
}
