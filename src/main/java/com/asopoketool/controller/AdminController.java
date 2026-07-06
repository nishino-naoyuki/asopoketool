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
                                   @RequestParam Map<String, String> allParams) {
        
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
                .build();
        tournamentService.createTournament(tournament);

        // Save Prize point settings for 1st to 16th place
        List<PrizePointSetting> prizeSettings = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            String paramVal = allParams.get("prize_point_" + i);
            int ptVal = (paramVal != null && !paramVal.trim().isEmpty()) ? Integer.parseInt(paramVal) : 0;
            prizeSettings.add(PrizePointSetting.builder()
                    .tournamentId(tournament.getId())
                    .rank(i)
                    .point(ptVal)
                    .build());
        }
        prizePointMapper.insertBatch(prizeSettings);

        return "redirect:/admin";
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
        model.addAttribute("brackets", bracketService.getBracketsByTournament(id));
        return "admin/bracket-manage";
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
        TimerSetting setting = TimerSetting.builder()
                .tournamentId(id)
                .roundNumber(t.getCurrentRound())
                .durationMinutes(durationMinutes)
                .bgmFileId(bgmFileId)
                .build();
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
        return "admin/manual-entry";
    }

    @PostMapping("/tournament/{id}/manual-entry")
    @org.springframework.transaction.annotation.Transactional
    public String manualEntry(@PathVariable Long id, @RequestParam String playerNames, Model model) {
        try {
            if (playerNames == null || playerNames.trim().isEmpty()) {
                throw new IllegalArgumentException("登録する選手名を入力してください。");
            }
            String[] rawNames = playerNames.split("[\\r\\n,]+");
            List<String> registeredList = new ArrayList<>();
            for (String raw : rawNames) {
                String name = raw.trim();
                if (!name.isEmpty()) {
                    tournamentService.manualRegister(id, name);
                    registeredList.add(name);
                }
            }
            if (registeredList.isEmpty()) {
                throw new IllegalArgumentException("登録できる有効な選手名がありませんでした。");
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
}
