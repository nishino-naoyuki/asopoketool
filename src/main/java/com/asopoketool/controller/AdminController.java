package com.asopoketool.controller;

import com.asopoketool.mapper.*;
import com.asopoketool.model.*;
import com.asopoketool.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private MatchGameMapper matchGameMapper;

    @Autowired
    private MatchResultMapper matchResultMapper;

    @Autowired
    private SwissDrawService swissDrawService;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private BracketService bracketService;

    @Autowired
    private com.asopoketool.mapper.BracketMapper bracketMapper;

    @Autowired
    private EntryService entryService;

    @Autowired
    private TimerService timerService;

    @Autowired
    private PrizePointMapper prizePointMapper;

    @Autowired
    private TournamentRoundMapper roundMapper;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PointMapper pointMapper;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private AccountController accountController;

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Tournament> tournaments = tournamentService.getAllTournaments();
        model.addAttribute("tournaments", tournaments);
        return "admin/dashboard";
    }

    @GetMapping("/tournament/create")
    public String showCreateForm(Model model) {
        return "admin/tournament-create";
    }

    @PostMapping("/tournament/create")
    public String createTournament(@RequestParam String name,
                                   @RequestParam String heldDate,
                                   @RequestParam int capacity,
                                   @RequestParam int totalRounds,
                                   @RequestParam int winPoint,
                                   @RequestParam int losePoint,
                                   @RequestParam int participationPoint,
                                   @RequestParam int bracketGroupSize,
                                   @RequestParam(required = false) String venue,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) String iconBase64,
                                   @RequestParam Map<String, String> allParams) {
        
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
                String filename = "icon_" + System.currentTimeMillis() + ".png";
                java.io.File dest = new java.io.File(dir, filename);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    fos.write(decodedBytes);
                }
                iconPath = "/asopoketool/timer-bg-files/" + filename;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Tournament tournament = Tournament.builder()
                .name(name)
                .heldDate(LocalDate.parse(heldDate))
                .capacity(capacity)
                .totalRounds(totalRounds)
                .winPoint(winPoint)
                .losePoint(losePoint)
                .participationPoint(participationPoint)
                .bracketGroupSize(bracketGroupSize)
                .venue(venue)
                .description(description)
                .iconPath(iconPath)
                .build();
        tournamentService.createTournament(tournament);

        // Save Prize point settings for 1st to 16th place
        List<PrizePointSetting> prizeSettings = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            String paramVal = allParams.get("prize_point_" + i);
            int ptVal = (paramVal != null && !paramVal.trim().isEmpty()) ? Integer.parseInt(paramVal) : 0;
            prizeSettings.add(PrizePointSetting.builder()
                    .tournamentId(tournament.getId())
                    .prizeRank(i)
                    .point(ptVal)
                    .build());
        }
        prizePointMapper.insertBatch(prizeSettings);

        return "redirect:/admin";
    }

    @GetMapping("/tournament/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/admin";
        }
        List<PrizePointSetting> prizeSettings = prizePointMapper.findByTournamentId(id);
        Map<Integer, Integer> prizePointsMap = new HashMap<>();
        for (PrizePointSetting p : prizeSettings) {
            prizePointsMap.put(p.getPrizeRank(), p.getPoint());
        }
        model.addAttribute("tournament", tournament);
        model.addAttribute("prizePointsMap", prizePointsMap);
        return "admin/tournament-edit";
    }

    @PostMapping("/tournament/{id}/edit")
    public String editTournament(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam String heldDate,
                                 @RequestParam int capacity,
                                 @RequestParam int totalRounds,
                                 @RequestParam int winPoint,
                                 @RequestParam int losePoint,
                                 @RequestParam int participationPoint,
                                 @RequestParam int bracketGroupSize,
                                 @RequestParam(required = false) String venue,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String iconBase64,
                                 @RequestParam Map<String, String> allParams) {
        
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/admin";
        }

        String iconPath = tournament.getIconPath();
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
                String filename = "icon_" + System.currentTimeMillis() + ".png";
                java.io.File dest = new java.io.File(dir, filename);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    fos.write(decodedBytes);
                }
                iconPath = "/asopoketool/timer-bg-files/" + filename;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tournament.setName(name);
        tournament.setHeldDate(LocalDate.parse(heldDate));
        tournament.setCapacity(capacity);
        tournament.setTotalRounds(totalRounds);
        tournament.setWinPoint(winPoint);
        tournament.setLosePoint(losePoint);
        tournament.setParticipationPoint(participationPoint);
        tournament.setBracketGroupSize(bracketGroupSize);
        tournament.setVenue(venue);
        tournament.setDescription(description);
        tournament.setIconPath(iconPath);
        tournamentService.updateTournament(tournament);

        // Update prize points (delete existing and insert new batch)
        prizePointMapper.deleteByTournamentId(id);
        List<PrizePointSetting> prizeSettings = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            String paramVal = allParams.get("prize_point_" + i);
            int ptVal = (paramVal != null && !paramVal.trim().isEmpty()) ? Integer.parseInt(paramVal) : 0;
            prizeSettings.add(PrizePointSetting.builder()
                    .tournamentId(id)
                    .prizeRank(i)
                    .point(ptVal)
                    .build());
        }
        prizePointMapper.insertBatch(prizeSettings);

        return "redirect:/admin/tournament/" + id;
    }

    @GetMapping("/tournament/{id}")
    public String manageTournament(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/admin";
        }
        model.addAttribute("tournament", tournament);
        model.addAttribute("entryCount", tournamentService.countEntries(id));
        model.addAttribute("checkedInCount", tournamentService.countCheckedIn(id));
        model.addAttribute("dropoutCount", tournamentService.countDropout(id));

        // Check if there are registered results for current round to disable/enable rematch button
        boolean hasRegisteredResults = false;
        if ("IN_PROGRESS".equals(tournament.getStatus())) {
            TournamentRound currentRoundObj = roundMapper.findByTournamentAndRound(id, tournament.getCurrentRound());
            if (currentRoundObj != null) {
                List<MatchGame> matches = matchGameMapper.findByRoundId(currentRoundObj.getId());
                for (MatchGame match : matches) {
                    if (!match.isBye()) {
                        MatchResult r = matchResultMapper.findByMatchId(match.getId());
                        if (r != null) {
                            hasRegisteredResults = true;
                            break;
                        }
                    }
                }
            }
        }
        model.addAttribute("hasRegisteredResults", hasRegisteredResults);

        return "admin/tournament-manage";
    }

    @RequestMapping(value = "/tournament/{id}/delete", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return "redirect:/admin";
    }

    @PostMapping("/tournament/{id}/start")
    @org.springframework.transaction.annotation.Transactional
    public String startTournament(@PathVariable Long id, Model model) {
        try {
            Tournament tournament = tournamentService.getTournamentById(id);
            List<TournamentRound> rounds = roundMapper.findByTournamentId(id);
            if ("ENTRY".equals(tournament.getStatus())) {
                tournamentService.startTournament(id);
                swissDrawService.generateMatching(id);
            } else if ("IN_PROGRESS".equals(tournament.getStatus()) && rounds.isEmpty()) {
                swissDrawService.generateMatching(id);
            } else {
                throw new IllegalStateException("この大会はすでに開始されているか、開始できないステータスです。");
            }
            return "redirect:/admin/tournament/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return manageTournament(id, model);
        }
    }

    @PostMapping("/tournament/{id}/next-round")
    @org.springframework.transaction.annotation.Transactional
    public String nextRound(@PathVariable Long id, Model model) {
        try {
            Tournament tournament = tournamentService.getTournamentById(id);
            
            // Check if all matches in the current round have results registered
            TournamentRound currentRoundObj = roundMapper.findByTournamentAndRound(id, tournament.getCurrentRound());
            if (currentRoundObj != null) {
                List<MatchGame> matches = matchGameMapper.findByRoundId(currentRoundObj.getId());
                for (MatchGame match : matches) {
                    if (!match.isBye() && match.getResultWinnerEntryId() == null) {
                        throw new IllegalStateException("未登録の対戦結果があります。すべての試合結果を登録してください。");
                    }
                }
            }

            boolean onlyOneUndefeated = swissDrawService.isOnlyOneUndefeated(id);
            if (tournament.getCurrentRound() >= tournament.getTotalRounds() || onlyOneUndefeated) {
                // If final round finished or only one undefeated player remains, proceed to Bracket phase
                tournamentService.startBracketPhase(id);
                bracketService.generateBrackets(id);
            } else {
                swissDrawService.generateMatching(id);
            }
            return "redirect:/admin/tournament/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return manageTournament(id, model);
        }
    }

    @PostMapping("/tournament/{id}/force-bracket")
    @org.springframework.transaction.annotation.Transactional
    public String forceBracket(@PathVariable Long id, Model model) {
        try {
            Tournament tournament = tournamentService.getTournamentById(id);
            if (!"IN_PROGRESS".equals(tournament.getStatus())) {
                throw new IllegalStateException("予選進行中の大会のみ操作できます。");
            }

            // 現ラウンドに未入力の試合がある場合は強制移行不可（通常の「次のラウンドへ」と同じ制約）
            TournamentRound currentRoundObj = roundMapper.findByTournamentAndRound(id, tournament.getCurrentRound());
            if (currentRoundObj != null) {
                List<MatchGame> matches = matchGameMapper.findByRoundId(currentRoundObj.getId());
                for (MatchGame match : matches) {
                    if (!match.isBye() && match.getResultWinnerEntryId() == null) {
                        throw new IllegalStateException("未登録の対戦結果があります。すべての試合結果を登録してから強制移行してください。");
                    }
                }
            }

            // 現在の予選順位のまま決勝トーナメントフェーズへ移行
            tournamentService.startBracketPhase(id);
            bracketService.generateBrackets(id);

            return "redirect:/admin/tournament/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return manageTournament(id, model);
        }
    }

    @PostMapping("/tournament/{id}/rematch")
    @org.springframework.transaction.annotation.Transactional
    public String rematch(@PathVariable Long id, Model model) {
        try {
            Tournament tournament = tournamentService.getTournamentById(id);
            if (tournament == null) {
                throw new IllegalArgumentException("大会が見つかりません。");
            }
            if (!"IN_PROGRESS".equals(tournament.getStatus())) {
                throw new IllegalStateException("進行中以外の大会はリマッチできません。");
            }
            swissDrawService.rematchRound(id, tournament.getCurrentRound());
            return "redirect:/admin/tournament/" + id + "?success=rematch";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return manageTournament(id, model);
        }
    }

    @PostMapping("/tournament/{id}/finish")
    public String finishTournament(@PathVariable Long id, Model model) {
        try {
            tournamentService.finishTournament(id);
            return "redirect:/admin/tournament/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return manageTournament(id, model);
        }
    }

    @GetMapping("/tournament/{id}/checkin")
    public String checkinManage(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        model.addAttribute("tournament", tournament);
        List<Entry> entries = entryMapper.findByTournamentId(id);
        model.addAttribute("entries", entries);
        return "admin/checkin-manage";
    }

    @PostMapping("/tournament/{id}/checkin/{entryId}")
    public String manualCheckin(@PathVariable Long id, @PathVariable Long entryId) {
        checkinService.manualCheckin(entryId);
        return "redirect:/admin/tournament/" + id + "/checkin";
    }

    @PostMapping("/tournament/{id}/checkin/{entryId}/cancel")
    public String adminCancelEntry(@PathVariable Long id, @PathVariable Long entryId) {
        try {
            entryService.cancelEntry(entryId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/admin/tournament/" + id + "/checkin";
    }

    @PostMapping("/tournament/{id}/checkin/scan")
    @ResponseBody
    public Map<String, Object> scanCheckin(@PathVariable Long id, @RequestParam String qrToken) {
        Map<String, Object> result = new HashMap<>();
        try {
            Entry entry = checkinService.checkinByQrPayload(qrToken);
            result.put("success", true);
            result.put("playerName", entry.getPlayerName());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/tournament/{id}/matching")
    public String matchingManage(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        model.addAttribute("tournament", tournament);

        List<TournamentRound> rounds = roundMapper.findByTournamentId(id);
        model.addAttribute("rounds", rounds);

        Map<Long, List<MatchGame>> roundMatches = new HashMap<>();
        for (TournamentRound r : rounds) {
            roundMatches.put(r.getId(), matchGameMapper.findByRoundId(r.getId()));
        }
        model.addAttribute("roundMatches", roundMatches);
        
        // Keep current round for quick focus
        int currentRound = tournament.getCurrentRound();
        if (currentRound > 0) {
            TournamentRound round = roundMapper.findByTournamentAndRound(id, currentRound);
            model.addAttribute("round", round);
        }

        return "admin/matching-manage";
    }

    @PostMapping("/tournament/{id}/result/{matchId}")
    public String saveResult(@PathVariable Long id, @PathVariable Long matchId, @RequestParam Long winnerEntryId) {
        MatchResult existing = matchResultMapper.findByMatchId(matchId);
        if (existing != null) {
            existing.setWinnerEntryId(winnerEntryId);
            existing.setRegisteredBy("ADMIN");
            matchResultMapper.update(existing);
        } else {
            MatchResult result = MatchResult.builder()
                    .matchId(matchId)
                    .winnerEntryId(winnerEntryId)
                    .registeredBy("ADMIN")
                    .build();
            matchResultMapper.insert(result);
        }
        return "redirect:/admin/tournament/" + id + "/matching";
    }

    @GetMapping("/tournament/{id}/dropout")
    public String dropoutManage(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        model.addAttribute("tournament", tournament);
        model.addAttribute("entries", entryMapper.findByTournamentId(id));
        return "admin/dropout-manage";
    }

    @PostMapping("/tournament/{id}/dropout/{entryId}")
    public String processDropout(@PathVariable Long id, @PathVariable Long entryId) {
        tournamentService.dropout(entryId);
        return "redirect:/admin/tournament/" + id + "/dropout";
    }

    @PostMapping("/tournament/{id}/dropout/{entryId}/cancel")
    public String cancelDropout(@PathVariable Long id, @PathVariable Long entryId) {
        tournamentService.cancelDropout(entryId);
        return "redirect:/admin/tournament/" + id + "/dropout";
    }

    @GetMapping("/tournament/{id}/bracket")
    public String bracketManage(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        model.addAttribute("tournament", tournament);
        List<BracketTournament> brackets = bracketService.getBracketsByTournament(id);
        model.addAttribute("brackets", brackets);

        // Find active bracket round (the first round that is not finished)
        int activeRound = 0;
        String activeRoundName = "";
        Map<Integer, Boolean> isRoundFinishedMap = new HashMap<>();

        if (brackets != null && !brackets.isEmpty()) {
            int maxRound = brackets.get(0).getMatches().stream()
                    .mapToInt(BracketMatch::getRoundNumber)
                    .max()
                    .orElse(1);

            for (int r = 1; r <= maxRound; r++) {
                TournamentRound tr = roundMapper.findByTournamentAndRound(id, 100 + r);
                boolean isFinished = tr != null && "FINISHED".equals(tr.getStatus());
                isRoundFinishedMap.put(r, isFinished);

                if (activeRound == 0 && !isFinished) {
                    activeRound = r;
                    if (r == 1) {
                        activeRoundName = "1回戦";
                    } else if (r == maxRound - 1 && maxRound > 2) {
                        activeRoundName = "準決勝";
                    } else if (r == maxRound) {
                        activeRoundName = "決勝戦";
                    } else {
                        activeRoundName = r + "回戦";
                    }
                }
            }
        }
        model.addAttribute("activeRound", activeRound);
        model.addAttribute("activeRoundName", activeRoundName);
        model.addAttribute("isRoundFinishedMap", isRoundFinishedMap);

        return "admin/bracket-manage";
    }

    @PostMapping("/tournament/{id}/bracket/finish-round")
    public String finishBracketRound(@PathVariable Long id, @RequestParam int roundNumber) {
        TournamentRound tr = roundMapper.findByTournamentAndRound(id, 100 + roundNumber);
        if (tr != null) {
            roundMapper.finish(tr.getId(), LocalDateTime.now());
        }
        return "redirect:/admin/tournament/" + id + "/bracket";
    }

    @GetMapping("/tournament/{id}/bracket/matches")
    public String showBracketMatches(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/admin";
        }
        model.addAttribute("tournament", tournament);
        model.addAttribute("isAdmin", true);

        List<BracketTournament> brackets = bracketService.getBracketsByTournament(id);
        
        List<TournamentController.BracketMatchDisplay> round1Matches = new ArrayList<>();
        List<TournamentController.BracketMatchDisplay> round2Matches = new ArrayList<>();
        List<TournamentController.BracketMatchDisplay> round3Matches = new ArrayList<>();

        int round1TableAcc = 1;
        int round2TableAcc = 1;
        int round3TableAcc = 1;

        if (brackets != null) {
            brackets.sort((b1, b2) -> b1.getGroupName().compareTo(b2.getGroupName()));

            for (BracketTournament bt : brackets) {
                List<BracketMatch> matches = bt.getMatches();
                if (matches == null) continue;
                
                matches.sort((m1, m2) -> {
                    if (m1.getRoundNumber() != m2.getRoundNumber()) {
                        return Integer.compare(m1.getRoundNumber(), m2.getRoundNumber());
                    }
                    return Integer.compare(m1.getMatchNumber(), m2.getMatchNumber());
                });

                for (BracketMatch bm : matches) {
                    if (bm.getRoundNumber() == 1) {
                        int tableNum = round1TableAcc++;
                        round1Matches.add(new TournamentController.BracketMatchDisplay(
                            bt.getGroupName(), tableNum, bm.getPlayer1Name(), bm.getPlayer2Name(),
                            bm.isBye(), bm.getWinnerEntryId(), bm.getPlayer1EntryId(), bm.getPlayer2EntryId(),
                            bm.getRoundNumber(), bm.getMatchNumber()
                        ));
                    } else if (bm.getRoundNumber() == 2) {
                        int tableNum = round2TableAcc++;
                        round2Matches.add(new TournamentController.BracketMatchDisplay(
                            bt.getGroupName(), tableNum, bm.getPlayer1Name(), bm.getPlayer2Name(),
                            bm.isBye(), bm.getWinnerEntryId(), bm.getPlayer1EntryId(), bm.getPlayer2EntryId(),
                            bm.getRoundNumber(), bm.getMatchNumber()
                        ));
                    } else if (bm.getRoundNumber() == 3) {
                        int tableNum = round3TableAcc++;
                        round3Matches.add(new TournamentController.BracketMatchDisplay(
                            bt.getGroupName(), tableNum, bm.getPlayer1Name(), bm.getPlayer2Name(),
                            bm.isBye(), bm.getWinnerEntryId(), bm.getPlayer1EntryId(), bm.getPlayer2EntryId(),
                            bm.getRoundNumber(), bm.getMatchNumber()
                        ));
                    }
                }
            }
        }

        model.addAttribute("round1Matches", round1Matches);
        model.addAttribute("round2Matches", round2Matches);
        model.addAttribute("round3Matches", round3Matches);

        return "player/bracket-matches";
    }

    @PostMapping("/tournament/{id}/bracket/generate")
    public String generateBrackets(@PathVariable Long id) {
        bracketService.generateBrackets(id);
        return "redirect:/admin/tournament/" + id + "/bracket";
    }

    @PostMapping("/bracket-match/{matchId}/result")
    @ResponseBody
    public Map<String, Object> registerBracketResult(@PathVariable Long matchId, @RequestParam Long winnerEntryId) {
        Map<String, Object> result = new HashMap<>();
        try {
            BracketMatch match = bracketMapper.findMatchById(matchId);
            if (match != null) {
                BracketTournament bt = bracketMapper.findById(match.getBracketTournamentId());
                if (bt != null) {
                    TournamentRound tr = roundMapper.findByTournamentAndRound(bt.getTournamentId(), 100 + match.getRoundNumber());
                    if (tr != null && "FINISHED".equals(tr.getStatus())) {
                        result.put("success", false);
                        result.put("message", "該当ラウンドの結果は既に確定しているため、変更できません。");
                        return result;
                    }
                }
            }
            bracketService.registerBracketResult(matchId, winnerEntryId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/tournament/{id}/timer")
    public String timerManage(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        model.addAttribute("tournament", tournament);
        model.addAttribute("timerSetting", timerService.getTimerSetting(id, tournament.getCurrentRound()));
        model.addAttribute("bgmFiles", timerService.getAllBgm());
        return "admin/timer-manage";
    }

    @PostMapping("/tournament/{id}/timer")
    public String saveTimerSetting(@PathVariable Long id,
                                   @RequestParam int durationMinutes,
                                   @RequestParam(required = false) Long bgmFileId) {
        Tournament t = tournamentService.getTournamentById(id);
        
        // 新規オブジェクトをゼロからビルドせず、既存のレコード（画像パス等を含む）を取得して上書きする
        TimerSetting setting = timerService.getTimerSetting(id, t.getCurrentRound());
        
        // 取得したレコードがデフォルトのフォールバックだった場合、ラウンド番号が一致しない可能性があるので明示的にセット
        setting.setTournamentId(id);
        setting.setRoundNumber(t.getCurrentRound());
        
        setting.setDurationMinutes(durationMinutes);
        setting.setBgmFileId(bgmFileId);
        
        // BGMファイルのパスと名前を即座に反映させるため transient フィールドを補完
        if (bgmFileId != null) {
            BgmFile bgm = timerService.getBgmById(bgmFileId);
            if (bgm != null) {
                setting.setBgmDisplayName(bgm.getDisplayName());
                setting.setBgmFilePath(bgm.getFilePath());
            }
        } else {
            setting.setBgmDisplayName(null);
            setting.setBgmFilePath(null);
        }

        timerService.saveTimerSetting(setting);
        return "redirect:/admin/tournament/" + id + "/timer?success=true";
    }

    @PostMapping("/tournament/{id}/timer/bgm-upload")
    public String uploadBgm(@PathVariable Long id,
                            @RequestParam String displayName,
                            @RequestParam MultipartFile bgmFile,
                            Model model) {
        if (!bgmFile.isEmpty()) {
            try {
                timerService.uploadBgm(displayName, bgmFile.getBytes(), bgmFile.getOriginalFilename());
                return "redirect:/admin/tournament/" + id + "/timer?success=upload";
            } catch (IOException e) {
                model.addAttribute("error", "BGMファイルの保存に失敗しました: " + e.getMessage());
            }
        } else {
            model.addAttribute("error", "ファイルを選択してください。");
        }
        return timerManage(id, model);
    }

    @GetMapping("/tournament/{id}/manual-entry")
    public String showManualEntryForm(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        model.addAttribute("tournament", tournament);

        // Get all accounts
        List<PlayerAccount> allAccounts = accountMapper.findAllAccounts();
        
        // Get already entered account IDs
        List<Entry> entries = entryMapper.findByTournamentId(id);
        Set<Long> enteredAccountIds = new HashSet<>();
        for (Entry e : entries) {
            if (e.getAccountId() != null) {
                enteredAccountIds.add(e.getAccountId());
            }
        }

        // Filter out already entered accounts
        List<PlayerAccount> availableAccounts = new ArrayList<>();
        for (PlayerAccount acc : allAccounts) {
            if (!enteredAccountIds.contains(acc.getId())) {
                availableAccounts.add(acc);
            }
        }
        model.addAttribute("availableAccounts", availableAccounts);

        return "admin/manual-entry";
    }

    @PostMapping("/tournament/{id}/manual-entry")
    @org.springframework.transaction.annotation.Transactional
    public String manualEntry(@PathVariable Long id,
                              @RequestParam(required = false) String playerNames,
                              @RequestParam(required = false) List<Long> selectedAccountIds,
                              Model model) {
        try {
            List<String> registeredList = new ArrayList<>();

            // 1. Process account selections
            if (selectedAccountIds != null && !selectedAccountIds.isEmpty()) {
                for (Long accountId : selectedAccountIds) {
                    PlayerAccount acc = accountMapper.findById(accountId);
                    if (acc != null) {
                        entryService.enterTournament(id, acc.getDisplayName(), accountId, null, true);
                        registeredList.add(acc.getDisplayName());
                    }
                }
            }

            // 2. Process text inputs
            if (playerNames != null && !playerNames.trim().isEmpty()) {
                String[] rawNames = playerNames.split("[\\r\\n,]+");
                for (String raw : rawNames) {
                    String name = raw.trim();
                    if (!name.isEmpty()) {
                        tournamentService.manualRegister(id, name);
                        registeredList.add(name);
                    }
                }
            }

            if (registeredList.isEmpty()) {
                throw new IllegalArgumentException("登録する選手を正しく選択するか、選手名を入力してください。");
            }
            
            Tournament tournament = tournamentService.getTournamentById(id);
            if (tournament != null && "IN_PROGRESS".equals(tournament.getStatus())) {
                return "redirect:/admin/tournament/" + id;
            }
            return "redirect:/admin/tournament/" + id + "/checkin";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("playerNames", playerNames);
            return showManualEntryForm(id, model);
        }
    }

    @GetMapping("/tournament/{id}/ranking")
    public String tournamentRanking(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/admin";
        }
        model.addAttribute("tournament", tournament);
        model.addAttribute("ranking", rankingService.getRanking(id));
        return "admin/ranking";
    }

    @PostMapping("/tournament/{id}/timer/bg-upload")
    public String timerBgUpload(@PathVariable Long id, @RequestParam("bgImage") MultipartFile file, Model model) {
        if (!file.isEmpty()) {
            try {
                timerService.uploadBgImage(id, file.getBytes(), file.getOriginalFilename());
                return "redirect:/admin/tournament/" + id + "/timer?success=bg_upload";
            } catch (IOException e) {
                model.addAttribute("error", "背景画像の保存に失敗しました: " + e.getMessage());
            }
        } else {
            model.addAttribute("error", "ファイルを選択してください。");
        }
        return timerManage(id, model);
    }

    @GetMapping("/users")
    public String listAdminUsers(Model model) {
        model.addAttribute("users", adminUserMapper.findAll());
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createAdminUser(@RequestParam String username, @RequestParam String password, Model model) {
        try {
            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                throw new IllegalArgumentException("ユーザー名とパスワードを入力してください。");
            }
            if (adminUserMapper.findByUsername(username) != null) {
                throw new IllegalArgumentException("すでに存在するユーザー名です。");
            }
            AdminUser user = AdminUser.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .role("ADMIN")
                    .build();
            adminUserMapper.insert(user);
            return "redirect:/admin/users?success=create";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return listAdminUsers(model);
        }
    }

    @GetMapping("/players")
    public String listPlayers(Model model) {
        List<PlayerAccount> accounts = accountMapper.findAllAccounts();
        
        // Build summary maps for views
        Map<Long, Integer> entryCounts = new HashMap<>();
        Map<Long, Integer> cumulativePoints = new HashMap<>();
        
        for (PlayerAccount acc : accounts) {
            Long accId = acc.getId();
            entryCounts.put(accId, entryMapper.findByAccountId(accId).size());
            
            PlayerCumulativePoint pt = pointMapper.findByAccountId(accId);
            cumulativePoints.put(accId, (pt != null) ? pt.getTotalPoint() : 0);
        }
        
        model.addAttribute("players", accounts);
        model.addAttribute("entryCounts", entryCounts);
        model.addAttribute("cumulativePoints", cumulativePoints);
        return "admin/player-list";
    }

    @PostMapping("/players/{id}/reset-password")
    public String resetPlayerPassword(@PathVariable Long id, @RequestParam String newPassword, Model model) {
        try {
            if (newPassword == null || newPassword.isEmpty()) {
                throw new IllegalArgumentException("新しいパスワードを入力してください。");
            }
            accountMapper.updatePassword(id, passwordEncoder.encode(newPassword));
            return "redirect:/admin/players?success=reset";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return listPlayers(model);
        }
    }

    @GetMapping("/tournament/{id}/players")
    public String showTournamentPlayers(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/admin";
        }
        model.addAttribute("tournament", tournament);

        List<Entry> entries = entryMapper.findByTournamentId(id);
        Map<Long, Integer> cumulativePoints = new HashMap<>();
        for (Entry entry : entries) {
            if (entry.getAccountId() != null) {
                PlayerCumulativePoint pt = pointMapper.findByAccountId(entry.getAccountId());
                cumulativePoints.put(entry.getAccountId(), (pt != null) ? pt.getTotalPoint() : 0);
            }
        }
        model.addAttribute("entries", entries);
        model.addAttribute("cumulativePoints", cumulativePoints);

        return "admin/players-manage-list";
    }

    /** 管理者によるユーザー戦績閲覧（records.htmlを流用） */
    @GetMapping("/players/{id}/records")
    public String showPlayerRecords(@PathVariable Long id, Model model) {
        PlayerAccount account = accountMapper.findById(id);
        if (account == null) {
            return "redirect:/admin/players";
        }
        // AccountControllerのbuildRecordsModelと同じロジックをこちらでも呼び出す
        // AccountControllerはURLが違うので、モデルビルドに必要なMapperをここでも直接使う
        accountController.buildRecordsModelPublic(id, account.getDisplayName(), model);
        model.addAttribute("isAdminView", true);
        return "account/records";
    }
}
