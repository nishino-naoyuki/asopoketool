package com.asopoketool.mapper;

import com.asopoketool.model.Entry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EntryMapper {
    List<Entry> findByTournamentId(Long tournamentId);
    List<Entry> findActiveByTournamentId(Long tournamentId); // checkin_flg = 1 AND dropout_flg = 0
    Entry findById(Long id);
    Entry findByTournamentAndSession(@Param("tournamentId") Long tournamentId, @Param("sessionToken") String sessionToken);
    Entry findByTournamentAndAccount(@Param("tournamentId") Long tournamentId, @Param("accountId") Long accountId);
    Entry findByQrToken(String qrToken);
    void insert(Entry entry);
    void update(Entry entry);
    void checkin(@Param("id") Long id, @Param("checkinAt") LocalDateTime checkinAt);
    void dropout(@Param("id") Long id, @Param("dropoutAt") LocalDateTime dropoutAt);
    void cancelDropout(Long id);
    void cancelEntry(Long id);
    List<Entry> findByAccountId(Long accountId); // for member history
    void deleteByTournamentId(Long tournamentId);
}
