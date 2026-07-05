package com.asopoketool.mapper;

import com.asopoketool.model.BgmFile;
import com.asopoketool.model.TimerSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TimerMapper {
    TimerSetting findByTournamentAndRound(@Param("tournamentId") Long tournamentId, @Param("roundNumber") int roundNumber);
    void upsert(TimerSetting setting);
    List<BgmFile> findAllBgm();
    BgmFile findBgmById(Long id);
    void insertBgm(BgmFile bgm);
    void deleteByTournamentId(Long tournamentId);
}
