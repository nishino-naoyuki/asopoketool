package com.asopoketool.mapper;

import com.asopoketool.model.Tournament;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TournamentMapper {
    List<Tournament> findAll();
    List<Tournament> findActive(); // status != 'FINISHED'
    Tournament findById(Long id);
    void insert(Tournament tournament);
    void update(Tournament tournament);
    void delete(Long id);
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    void updateCurrentRound(@Param("id") Long id, @Param("round") int round);
    int countEntries(Long tournamentId);
    int countCheckedIn(Long tournamentId);
    int countDropout(Long tournamentId);
}
