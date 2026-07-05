package com.asopoketool.mapper;

import com.asopoketool.model.PlayerCumulativePoint;
import com.asopoketool.model.PointHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PointMapper {
    List<PlayerCumulativePoint> findAllOrderByPoint();
    PlayerCumulativePoint findByAccountId(Long accountId);
    void upsertCumulativePoint(@Param("accountId") Long accountId, @Param("addPoint") int addPoint);
    void insertHistory(PointHistory history);
    List<PointHistory> findHistoryByAccountId(Long accountId);
    List<PointHistory> findHistoryByTournamentId(Long tournamentId);
    void deleteHistoryByTournamentId(Long tournamentId);
}
