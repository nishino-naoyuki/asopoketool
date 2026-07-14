package com.asopoketool.controller;

import com.asopoketool.model.PlayerAccount;
import com.asopoketool.model.PointHistory;
import com.asopoketool.model.TournamentRecordVO;
import com.asopoketool.model.OpponentRecordVO;
import com.asopoketool.model.Entry;
import com.asopoketool.model.MatchGame;
import com.asopoketool.model.BracketMatch;
import com.asopoketool.service.AccountService;
import com.asopoketool.service.PointService;
import com.asopoketool.mapper.PointMapper;
import com.asopoketool.mapper.MatchGameMapper;
import com.asopoketool.mapper.BracketMapper;
import com.asopoketool.mapper.EntryMapper;
import com.asopoketool.mapper.TournamentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private PointMapper pointMapper;

    @Autowired
    private MatchGameMapper matchGameMapper;

    @Autowired
    private BracketMapper bracketMapper;

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private TournamentMapper tournamentMapper;

    @GetMapping("/register")
    public String showRegisterForm(HttpServletRequest request, Model model) {
        if (Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/";
        }
        return "account/register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam String displayName,
                                  @RequestParam String password,
                                  @RequestParam String passwordConfirm,
                                  @RequestParam(required = false) String iconBase64,
                                  HttpServletResponse response,
                                  Model model) {
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("error", "パスワードと確認用パスワードが一致しません。");
            return "account/register";
        }

        try {
            String iconPath = null;
            if (iconBase64 != null && !iconBase64.trim().isEmpty()) {
                try {
                    String base64Data = iconBase64;
                    if (base64Data.contains(",")) {
                        base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                    }
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);

                    java.io.File dir = new java.io.File(System.getProperty("user.dir") + "/data/images");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    String filename = "user_icon_" + System.currentTimeMillis() + ".png";
                    java.io.File dest = new java.io.File(dir, filename);
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                        fos.write(decodedBytes);
                    }
                    iconPath = "/asopoketool/timer-bg-files/" + filename;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String token = accountService.register(displayName, password, iconPath);
            setAuthCookie(response, token);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "account/register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request) {
        if (Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/";
        }
        return "account/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String displayName,
                               @RequestParam String password,
                               HttpServletResponse response,
                               Model model) {
        try {
            String token = accountService.login(displayName, password);
            setAuthCookie(response, token);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "account/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("APT_TOKEN".equals(cookie.getName())) {
                    accountService.logout(cookie.getValue());
                    Cookie clearCookie = new Cookie("APT_TOKEN", null);
                    clearCookie.setPath("/");
                    clearCookie.setMaxAge(0);
                    response.addCookie(clearCookie);
                    break;
                }
            }
        }
        return "redirect:/";
    }

    @GetMapping("/mypage")
    public String showMyPage(HttpServletRequest request, Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        model.addAttribute("account", current);
        
        // Cumulative point & history
        model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
        List<PointHistory> histories = pointMapper.findHistoryByAccountId(current.getId());
        model.addAttribute("histories", histories);

        return "account/mypage";
    }

    @PostMapping("/update-name")
    public String updateDisplayName(HttpServletRequest request, @RequestParam String displayName, Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        try {
            accountService.updateDisplayName(current.getId(), displayName);
            current.setDisplayName(displayName); // reflect updated name in request context
            return "redirect:/account/mypage?success=name";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", current);
            model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
            model.addAttribute("histories", pointMapper.findHistoryByAccountId(current.getId()));
            return "account/mypage";
        }
    }

    @PostMapping("/update-password")
    public String updatePassword(HttpServletRequest request,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String newPasswordConfirm,
                                 Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        if (!newPassword.equals(newPasswordConfirm)) {
            model.addAttribute("error", "新しいパスワードと確認用が一致しません。");
            model.addAttribute("account", current);
            model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
            model.addAttribute("histories", pointMapper.findHistoryByAccountId(current.getId()));
            return "account/mypage";
        }

        try {
            accountService.updatePassword(current.getId(), currentPassword, newPassword);
            return "redirect:/account/mypage?success=password";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", current);
            model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
            model.addAttribute("histories", pointMapper.findHistoryByAccountId(current.getId()));
            return "account/mypage";
        }
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("APT_TOKEN", token);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 120); // 120 days persistent session
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // Uncomment if deploying on HTTPS
        response.addCookie(cookie);
    }

    // -----------------------------------------------------------------------
    //  戦績ページ（ユーザー自身）
    // -----------------------------------------------------------------------
    @GetMapping("/records")
    public String showRecords(HttpServletRequest request, Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }
        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        buildRecordsModelPublic(current.getId(), current.getDisplayName(), model);
        model.addAttribute("isAdminView", false);
        return "account/records";
    }

    @PostMapping("/update-icon")
    public String updateIconPath(HttpServletRequest request,
                                 @RequestParam(required = false) String iconBase64,
                                 Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        String iconPath = null;
        if (iconBase64 != null && !iconBase64.trim().isEmpty()) {
            try {
                String base64Data = iconBase64;
                if (base64Data.contains(",")) {
                    base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                }
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);

                java.io.File dir = new java.io.File(System.getProperty("user.dir") + "/data/images");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String filename = "user_icon_" + System.currentTimeMillis() + ".png";
                java.io.File dest = new java.io.File(dir, filename);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    fos.write(decodedBytes);
                }
                iconPath = "/asopoketool/timer-bg-files/" + filename;
                accountService.updateIconPath(current.getId(), iconPath);
                current.setIconPath(iconPath);
                return "redirect:/account/mypage?success=icon";
            } catch (Exception e) {
                model.addAttribute("error", "アイコンの更新に失敗しました: " + e.getMessage());
            }
        }
        
        model.addAttribute("account", current);
        model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
        model.addAttribute("histories", pointMapper.findHistoryByAccountId(current.getId()));
        return "account/mypage";
    }

    // -----------------------------------------------------------------------
    //  戦績集計ヘルパー（ユーザー・管理者共用）
    // -----------------------------------------------------------------------

    /**
     * 指定アカウントIDの全試合データを集計し、modelに以下を追加する:
     *   - tournamentRecords : List<TournamentRecordVO>  大会別戦績
     *   - opponentRecords   : List<OpponentRecordVO>   対戦相手別戦績
     *   - targetDisplayName : String                   対象ユーザーの表示名
     */
    public void buildRecordsModelPublic(Long accountId, String displayName, Model model) {
        model.addAttribute("targetDisplayName", displayName);

        // アカウントが出場したエントリー一覧（大会情報も取れる）
        List<Entry> myEntries = entryMapper.findByAccountId(accountId);
        // entryId → tournamentId のマップ
        Map<Long, Long> entryToTournament = new HashMap<>();
        // tournamentId → entry の最初の1件（大会ごとのエントリー名取得用）
        Map<Long, Entry> tournamentToEntry = new LinkedHashMap<>();
        for (Entry e : myEntries) {
            entryToTournament.put(e.getId(), e.getTournamentId());
            tournamentToEntry.putIfAbsent(e.getTournamentId(), e);
        }

        // 大会情報（TournamentRecordVOの初期化用）
        Map<Long, com.asopoketool.model.Tournament> tournamentMap = new LinkedHashMap<>();
        for (Long tid : tournamentToEntry.keySet()) {
            com.asopoketool.model.Tournament t = tournamentMapper.findById(tid);
            if (t != null) tournamentMap.put(tid, t);
        }

        // tournamentId → TournamentRecordVO（大会別集計）
        Map<Long, TournamentRecordVO> recordMap = new LinkedHashMap<>();
        for (Long tid : tournamentMap.keySet()) {
            com.asopoketool.model.Tournament t = tournamentMap.get(tid);
            Entry e = tournamentToEntry.get(tid);
            TournamentRecordVO vo = TournamentRecordVO.builder()
                    .tournamentId(tid)
                    .tournamentName(t.getName())
                    .heldDate(t.getHeldDate() != null ? t.getHeldDate().toString() : "")
                    .entryName(e != null ? e.getPlayerName() : "")
                    .build();
            recordMap.put(tid, vo);
        }

        // ─── スイス式試合の集計 ───────────────────────────────────────────
        List<MatchGame> swissMatches = matchGameMapper.findByAccountId(accountId);
        // 対戦相手別: key=相手のエントリー名, value=[wins, losses, lastDate]
        Map<String, int[]> opponentStats = new LinkedHashMap<>();   // [wins, losses]
        Map<String, String> opponentLastDate = new LinkedHashMap<>();

        // 自分のエントリーID一覧（account横断）
        Set<Long> myEntryIds = myEntries.stream().map(Entry::getId).collect(Collectors.toSet());

        for (MatchGame mg : swissMatches) {
            Long tid = mg.getTournamentId();
            TournamentRecordVO vo = recordMap.get(tid);
            if (vo == null) continue;

            com.asopoketool.model.Tournament t = tournamentMap.get(tid);
            String heldDate = (t != null && t.getHeldDate() != null) ? t.getHeldDate().toString() : "";

            // BYE処理
            if (mg.isBye()) {
                vo.setSwissWins(vo.getSwissWins() + 1);
                vo.setSwissByes(vo.getSwissByes() + 1);
                continue;
            }

            // 自分がplayer1かplayer2か判定
            boolean iAmPlayer1 = myEntryIds.contains(mg.getPlayer1EntryId());
            boolean won = (mg.getResultWinnerEntryId() != null)
                    && myEntryIds.contains(mg.getResultWinnerEntryId());

            String opponentName = iAmPlayer1 ? mg.getPlayer2Name() : mg.getPlayer1Name();
            if (opponentName == null) opponentName = "（不明）";

            if (won) {
                vo.setSwissWins(vo.getSwissWins() + 1);
            } else {
                vo.setSwissLosses(vo.getSwissLosses() + 1);
            }

            // 対戦相手別集計
            opponentStats.putIfAbsent(opponentName, new int[]{0, 0});
            if (won) opponentStats.get(opponentName)[0]++;
            else     opponentStats.get(opponentName)[1]++;
            if (!opponentLastDate.containsKey(opponentName) 
                    || heldDate.compareTo(opponentLastDate.get(opponentName)) > 0) {
                opponentLastDate.put(opponentName, heldDate);
            }
        }

        // ─── 決勝トーナメント試合の集計 ──────────────────────────────────
        List<BracketMatch> bracketMatches = bracketMapper.findByAccountId(accountId);

        for (BracketMatch bm : bracketMatches) {
            Long tid = bm.getTournamentId();
            TournamentRecordVO vo = recordMap.get(tid);
            if (vo == null) continue;

            vo.setBracketParticipated(true);
            com.asopoketool.model.Tournament t = tournamentMap.get(tid);
            String heldDate = (t != null && t.getHeldDate() != null) ? t.getHeldDate().toString() : "";

            if (bm.isBye()) {
                vo.setBracketWins(vo.getBracketWins() + 1);
                continue;
            }

            boolean iAmPlayer1 = myEntryIds.contains(bm.getPlayer1EntryId());
            boolean won = (bm.getWinnerEntryId() != null)
                    && myEntryIds.contains(bm.getWinnerEntryId());

            String opponentName = iAmPlayer1 ? bm.getPlayer2Name() : bm.getPlayer1Name();
            if (opponentName == null) opponentName = "（不明）";

            if (won) {
                vo.setBracketWins(vo.getBracketWins() + 1);
            } else {
                vo.setBracketLosses(vo.getBracketLosses() + 1);
            }

            // 対戦相手別集計
            opponentStats.putIfAbsent(opponentName, new int[]{0, 0});
            if (won) opponentStats.get(opponentName)[0]++;
            else     opponentStats.get(opponentName)[1]++;
            if (!opponentLastDate.containsKey(opponentName)
                    || heldDate.compareTo(opponentLastDate.get(opponentName)) > 0) {
                opponentLastDate.put(opponentName, heldDate);
            }
        }

        // ─── 優勝・準優勝の判定 ──────────────────────────────────────────
        // 決勝Tに出場した大会について最終ラウンド(round_number最大)のmatch_number=1を調べる
        Set<Long> checkedTournaments = new HashSet<>();
        for (BracketMatch bm : bracketMatches) {
            Long tid = bm.getTournamentId();
            if (checkedTournaments.contains(tid)) continue;
            checkedTournaments.add(tid);

            TournamentRecordVO vo = recordMap.get(tid);
            if (vo == null || !vo.isBracketParticipated()) continue;

            // 大会の全決勝T試合を取得して最終ラウンドを特定
            List<BracketMatch> allBracket = bracketMapper.findAllMatchesByTournamentId(tid);
            if (allBracket.isEmpty()) continue;

            // 最大round_numberを探す（降順で返ってくるので先頭）
            int maxRound = allBracket.get(0).getRoundNumber();
            // 最終ラウンドのmatch_number=1の試合（決勝戦）を特定
            BracketMatch finalMatch = allBracket.stream()
                    .filter(m -> m.getRoundNumber() == maxRound && m.getMatchNumber() == 1 && m.getWinnerEntryId() != null)
                    .findFirst().orElse(null);
            if (finalMatch == null) continue;

            Long winnerEntryId = finalMatch.getWinnerEntryId();
            Long loserEntryId  = Objects.equals(finalMatch.getPlayer1EntryId(), winnerEntryId)
                    ? finalMatch.getPlayer2EntryId()
                    : finalMatch.getPlayer1EntryId();

            if (myEntryIds.contains(winnerEntryId)) {
                vo.setResult("CHAMPION");
            } else if (myEntryIds.contains(loserEntryId)) {
                vo.setResult("RUNNER_UP");
            }
        }

        // ─── 結果をmodelに格納 ──────────────────────────────────────────
        // 大会別: 開催日降順
        List<TournamentRecordVO> tournamentRecords = new ArrayList<>(recordMap.values());
        tournamentRecords.sort((a, b) -> b.getHeldDate().compareTo(a.getHeldDate()));
        model.addAttribute("tournamentRecords", tournamentRecords);

        // 対戦相手別: 勝利数→対戦数降順
        List<OpponentRecordVO> opponentRecords = opponentStats.entrySet().stream()
                .map(e -> OpponentRecordVO.builder()
                        .opponentName(e.getKey())
                        .wins(e.getValue()[0])
                        .losses(e.getValue()[1])
                        .lastPlayedDate(opponentLastDate.getOrDefault(e.getKey(), ""))
                        .build())
                .sorted(Comparator.comparingInt(OpponentRecordVO::getWins).reversed()
                        .thenComparing(Comparator.comparingInt(OpponentRecordVO::getTotalGames).reversed()))
                .collect(Collectors.toList());
        model.addAttribute("opponentRecords", opponentRecords);
    }
}
