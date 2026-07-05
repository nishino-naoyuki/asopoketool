package com.asopoketool.mapper;

import com.asopoketool.model.PlayerAccount;
import com.asopoketool.model.PlayerAccountToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

@Mapper
public interface AccountMapper {
    PlayerAccount findById(Long id);
    PlayerAccount findByDisplayName(String displayName);
    void insertAccount(PlayerAccount account);
    void updateDisplayName(@Param("id") Long id, @Param("displayName") String displayName);
    void updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @org.apache.ibatis.annotations.Select("SELECT * FROM player_account ORDER BY created_at DESC")
    java.util.List<PlayerAccount> findAllAccounts();
    
    PlayerAccountToken findTokenByToken(String token);
    void insertToken(PlayerAccountToken token);
    void deleteToken(String token);
    void deleteExpiredTokens();
    void updateLastUsed(@Param("token") String token, @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
