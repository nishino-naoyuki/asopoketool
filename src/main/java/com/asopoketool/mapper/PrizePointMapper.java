package com.asopoketool.mapper;

import com.asopoketool.model.PrizePointSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PrizePointMapper {
    List<PrizePointSetting> findByTournamentId(Long tournamentId);
    PrizePointSetting findByTournamentAndRank(@Param("tournamentId") Long tournamentId, @Param("rank") int rank);
    void insertBatch(List<PrizePointSetting> settings);
    void deleteByTournamentId(Long tournamentId);
}
