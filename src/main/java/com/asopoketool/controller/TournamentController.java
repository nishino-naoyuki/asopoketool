package com.asopoketool.controller;

import com.asopoketool.model.*;
import com.asopoketool.service.BracketService;
import com.asopoketool.service.EntryService;
import com.asopoketool.service.RankingService;
import com.asopoketool.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/")
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private EntryService entryService;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private BracketService bracketService;

    @Autowired
    private com.asopoketool.mapper.TournamentRoundMapper roundMapper;

    @GetMapping
    public String index(Model model) {
        List<Tournament> all = tournamentService.getAllTournaments();
        List<Tournament> active = new ArrayList<>();
        List<Tournament> inProgress = new ArrayList<>();
        List<Tournament> finished = new ArrayList<>();
        for (Tournament t : all) {
            if ("FINISHED".equals(t.getStatus())) {
                finished.add(t);
            } else if ("IN_PROGRESS".equals(t.getStatus()) || "BRACKET".equals(t.getStatus())) {
                inProgress.add(t);
            } else {
                active.add(t);
            }
        }
        model.addAttribute("activeTournaments", active);
        model.addAttribute("inProgressTournaments", inProgress);
        model.addAttribute("finishedTournaments", finished);
        return "player/tournament-list";
    }

    @Autowired
    private com.asopoketool.mapper.PrizePointMapper prizePointMapper;

    @GetMapping("/tournament/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        // Fetch counts
        model.addAttribute("entryCount", tournamentService.countEntries(id));
        model.addAttribute("checkedInCount", tournamentService.countCheckedIn(id));

        // Fetch Prize Point Settings
        List<PrizePointSetting> prizePoints = prizePointMapper.findByTournamentId(id);
        List<PrizePointSetting> activePrizePoints = new java.util.ArrayList<>();
        if (prizePoints != null) {
            for (PrizePointSetting p : prizePoints) {
                if (p.getPoint() > 0) {
                    activePrizePoints.add(p);
                }
            }
        }
        model.addAttribute("prizePoints", activePrizePoints);

        // Check if user already entered
        String sessionToken = (String) request.getAttribute("sessionToken");
        PlayerAccount account = (PlayerAccount) request.getAttribute("currentAccount");
        Long accountId = (account != null) ? account.getId() : null;

        Entry myEntry = entryService.getEntryForCurrentUser(id, sessionToken, accountId);
        model.addAttribute("myEntry", myEntry);

        return "player/tournament-detail";
    }

    @PostMapping("/tournament/{id}/enter")
    public String enterGuest(@PathVariable Long id,
                             @RequestParam String playerName,
                             HttpServletRequest request,
                             Model model) {
        String sessionToken = (String) request.getAttribute("sessionToken");
        try {
            entryService.enterTournament(id, playerName, null, sessionToken, false);
            return "redirect:/tournament/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return detail(id, request, model);
        }
    }

    @PostMapping("/tournament/{id}/enter-quick")
    public String enterQuick(@PathVariable Long id,
                             HttpServletRequest request,
                             Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount account = (PlayerAccount) request.getAttribute("currentAccount");
        String sessionToken = (String) request.getAttribute("sessionToken");
        try {
            entryService.enterTournament(id, account.getDisplayName(), account.getId(), sessionToken, false);
            return "redirect:/tournament/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return detail(id, request, model);
        }
    }

    @GetMapping("/tournament/{id}/entry-name")
    public String showEntryNameForm(@PathVariable Long id, HttpServletRequest request, Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);
        
        PlayerAccount account = (PlayerAccount) request.getAttribute("currentAccount");
        model.addAttribute("defaultName", account.getDisplayName());
        return "player/entry-name";
    }

    @PostMapping("/tournament/{id}/enter-named")
    public String enterNamed(@PathVariable Long id,
                              @RequestParam String playerName,
                              HttpServletRequest request,
                              Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount account = (PlayerAccount) request.getAttribute("currentAccount");
        String sessionToken = (String) request.getAttribute("sessionToken");
        try {
            entryService.enterTournament(id, playerName, account.getId(), sessionToken, false);
            return "redirect:/tournament/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return showEntryNameForm(id, request, model);
        }
    }

    @PostMapping("/tournament/{id}/cancel-entry")
    public String cancelEntry(@PathVariable Long id,
                               @RequestParam Long entryId) {
        try {
            entryService.cancelEntry(entryId);
        } catch (Exception e) {
            // handle error
        }
        return "redirect:/tournament/" + id;
    }

    @GetMapping("/tournament/{id}/ranking")
    public String ranking(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        List<RankingEntry> ranking = rankingService.getRanking(id);
        model.addAttribute("ranking", ranking);

        return "player/ranking";
    }

    @GetMapping("/tournament/{id}/bracket")
    public String bracket(@PathVariable Long id, HttpServletRequest request, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        // Fetch user's entry info
        String sessionToken = (String) request.getAttribute("sessionToken");
        PlayerAccount account = (PlayerAccount) request.getAttribute("currentAccount");
        Long accountId = (account != null) ? account.getId() : null;
        Entry myEntry = entryService.getEntryForCurrentUser(id, sessionToken, accountId);
        model.addAttribute("myEntry", myEntry);

        List<BracketTournament> brackets = bracketService.getBracketsByTournament(id);
        model.addAttribute("brackets", brackets);

        Map<Integer, Boolean> isRoundFinishedMap = new HashMap<>();
        if (brackets != null && !brackets.isEmpty()) {
            int maxRound = brackets.get(0).getMatches().stream()
                    .mapToInt(BracketMatch::getRoundNumber)
                    .max()
                    .orElse(1);
            for (int r = 1; r <= maxRound; r++) {
                TournamentRound tr = roundMapper.findByTournamentAndRound(id, 100 + r);
                isRoundFinishedMap.put(r, tr != null && "FINISHED".equals(tr.getStatus()));
            }
        }
        model.addAttribute("isRoundFinishedMap", isRoundFinishedMap);

        return "player/bracket";
    }

    @GetMapping("/tournament/{id}/bracket/matches")
    public String showBracketMatches(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        List<BracketTournament> brackets = bracketService.getBracketsByTournament(id);
        
        List<BracketMatchDisplay> round1Matches = new ArrayList<>();
        List<BracketMatchDisplay> round2Matches = new ArrayList<>();
        List<BracketMatchDisplay> round3Matches = new ArrayList<>();

        int round1TableAcc = 1;
        int round2TableAcc = 1;
        int round3TableAcc = 1;

        if (brackets != null) {
            // グループ名でソートして、グループA, B, C の順にする
            brackets.sort((b1, b2) -> b1.getGroupName().compareTo(b2.getGroupName()));

            for (BracketTournament bt : brackets) {
                List<BracketMatch> matches = bt.getMatches();
                if (matches == null) continue;
                
                // ラウンド数とマッチ数でソート
                matches.sort((m1, m2) -> {
                    if (m1.getRoundNumber() != m2.getRoundNumber()) {
                        return Integer.compare(m1.getRoundNumber(), m2.getRoundNumber());
                    }
                    return Integer.compare(m1.getMatchNumber(), m2.getMatchNumber());
                });

                for (BracketMatch bm : matches) {
                    if (bm.getRoundNumber() == 1) {
                        int tableNum = round1TableAcc++;
                        round1Matches.add(new BracketMatchDisplay(
                            bt.getGroupName(), tableNum, bm.getPlayer1Name(), bm.getPlayer2Name(),
                            bm.isBye(), bm.getWinnerEntryId(), bm.getPlayer1EntryId(), bm.getPlayer2EntryId(),
                            bm.getRoundNumber(), bm.getMatchNumber()
                        ));
                    } else if (bm.getRoundNumber() == 2) {
                        int tableNum = round2TableAcc++;
                        round2Matches.add(new BracketMatchDisplay(
                            bt.getGroupName(), tableNum, bm.getPlayer1Name(), bm.getPlayer2Name(),
                            bm.isBye(), bm.getWinnerEntryId(), bm.getPlayer1EntryId(), bm.getPlayer2EntryId(),
                            bm.getRoundNumber(), bm.getMatchNumber()
                        ));
                    } else if (bm.getRoundNumber() == 3) {
                        int tableNum = round3TableAcc++;
                        round3Matches.add(new BracketMatchDisplay(
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

    public static class BracketMatchDisplay {
        private String groupName;
        private int tableNumber;
        private String player1Name;
        private String player2Name;
        private boolean isBye;
        private Long winnerEntryId;
        private Long player1EntryId;
        private Long player2EntryId;
        private int roundNumber;
        private int matchNumber;

        public BracketMatchDisplay(String groupName, int tableNumber, String player1Name, String player2Name, boolean isBye, Long winnerEntryId, Long player1EntryId, Long player2EntryId, int roundNumber, int matchNumber) {
            this.groupName = groupName;
            this.tableNumber = tableNumber;
            this.player1Name = player1Name;
            this.player2Name = player2Name;
            this.isBye = isBye;
            this.winnerEntryId = winnerEntryId;
            this.player1EntryId = player1EntryId;
            this.player2EntryId = player2EntryId;
            this.roundNumber = roundNumber;
            this.matchNumber = matchNumber;
        }

        public String getGroupName() { return groupName; }
        public int getTableNumber() { return tableNumber; }
        public String getPlayer1Name() { return player1Name; }
        public String getPlayer2Name() { return player2Name; }
        public boolean isBye() { return isBye; }
        public Long getWinnerEntryId() { return winnerEntryId; }
        public Long getPlayer1EntryId() { return player1EntryId; }
        public Long getPlayer2EntryId() { return player2EntryId; }
        public int getRoundNumber() { return roundNumber; }
        public int getMatchNumber() { return matchNumber; }
    }

    @GetMapping("/tournament/{id}/players")
    public String showPlayersList(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        List<Entry> entries = entryService.getEntriesByTournament(id);
        model.addAttribute("entries", entries);

        return "player/players-list";
    }
}
