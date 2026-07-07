package com.asopoketool.service;

import com.asopoketool.mapper.AccountMapper;
import com.asopoketool.model.PlayerAccount;
import com.asopoketool.model.PlayerAccountToken;
import com.asopoketool.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional
    public String register(String displayName, String password, String iconPath) {
        if (accountMapper.findByDisplayName(displayName) != null) {
            throw new IllegalArgumentException("この表示名はすでに使用されています。");
        }

        PlayerAccount account = PlayerAccount.builder()
                .displayName(displayName)
                .passwordHash(passwordEncoder.encode(password))
                .iconPath(iconPath)
                .build();
        accountMapper.insertAccount(account);

        return createAutoLoginToken(account.getId());
    }

    @Transactional
    public String register(String displayName, String password) {
        return register(displayName, password, null);
    }

    @Transactional
    public String login(String displayName, String password) {
        PlayerAccount account = accountMapper.findByDisplayName(displayName);
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new IllegalArgumentException("表示名またはパスワードが正しくありません。");
        }

        return createAutoLoginToken(account.getId());
    }

    @Transactional
    public void logout(String token) {
        accountMapper.deleteToken(token);
    }

    @Transactional
    public PlayerAccount findByToken(String token) {
        PlayerAccountToken pat = accountMapper.findTokenByToken(token);
        if (pat == null) {
            return null;
        }

        if (pat.getExpireAt().isBefore(LocalDateTime.now())) {
            accountMapper.deleteToken(token);
            return null;
        }

        accountMapper.updateLastUsed(token, LocalDateTime.now());
        return accountMapper.findById(pat.getAccountId());
    }

    public PlayerAccount findById(Long id) {
        return accountMapper.findById(id);
    }

    @Transactional
    public void updateDisplayName(Long id, String newName) {
        PlayerAccount existing = accountMapper.findByDisplayName(newName);
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("この表示名はすでに使用されています。");
        }
        accountMapper.updateDisplayName(id, newName);
    }

    @Transactional
    public void updatePassword(Long id, String currentPassword, String newPassword) {
        PlayerAccount account = accountMapper.findById(id);
        if (account == null || !passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("現在のパスワードが正しくありません。");
        }
        accountMapper.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    @Transactional
    public String createAutoLoginToken(Long accountId) {
        String token = TokenUtil.generateToken();
        PlayerAccountToken pat = PlayerAccountToken.builder()
                .accountId(accountId)
                .token(token)
                .expireAt(LocalDateTime.now().plusDays(120)) // 120 days persistent session
                .lastUsedAt(LocalDateTime.now())
                .build();
        accountMapper.insertToken(pat);
        return token;
    }

    @Transactional
    public void updateIconPath(Long id, String iconPath) {
        accountMapper.updateIconPath(id, iconPath);
    }
}
