package com.asopoketool.controller;

import com.asopoketool.mapper.MatchGameMapper;
import com.asopoketool.mapper.TournamentRoundMapper;
import com.asopoketool.model.Entry;
import com.asopoketool.model.MatchGame;
import com.asopoketool.model.PlayerAccount;
import com.asopoketool.model.Tournament;
import com.asopoketool.model.TournamentRound;
import com.asopoketool.service.EntryService;
import com.asopoketool.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/tournament")
public class MatchingController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TournamentRoundMapper roundMapper;

    @Autowired
    private MatchGameMapper matchGameMapper;

    @Autowired
    private EntryService entryService;

    @GetMapping("/{id}/matching")
    public String showMatching(@PathVariable Long id, HttpServletRequest request, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        // Fetch current round
        int currentRoundNum = tournament.getCurrentRound();
        if (currentRoundNum == 0) {
            model.addAttribute("noMatchingYet", true);
            return "player/matching";
        }

        TournamentRound round = roundMapper.findByTournamentAndRound(id, currentRoundNum);
        model.addAttribute("round", round);

        List<MatchGame> matches = matchGameMapper.findByRoundId(round.getId());
        model.addAttribute("matches", matches);

        // Find user's own match
        String sessionToken = (String) request.getAttribute("sessionToken");
        PlayerAccount account = (PlayerAccount) request.getAttribute("currentAccount");
        Long accountId = (account != null) ? account.getId() : null;

        Entry myEntry = entryService.getEntryForCurrentUser(id, sessionToken, accountId);
        if (myEntry != null) {
            model.addAttribute("myEntry", myEntry);
            MatchGame myMatch = null;
            for (MatchGame m : matches) {
                if (myEntry.getId().equals(m.getPlayer1EntryId()) || myEntry.getId().equals(m.getPlayer2EntryId())) {
                    myMatch = m;
                    break;
                }
            }
            model.addAttribute("myMatch", myMatch);
        }

        return "player/matching";
    }

    @GetMapping("/{id}/matching/project")
    public String showMatchingProject(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        int currentRoundNum = tournament.getCurrentRound();
        if (currentRoundNum == 0) {
            model.addAttribute("noMatchingYet", true);
            return "player/matching-project";
        }

        TournamentRound round = roundMapper.findByTournamentAndRound(id, currentRoundNum);
        model.addAttribute("round", round);

        List<MatchGame> matches = matchGameMapper.findByRoundId(round.getId());
        model.addAttribute("matches", matches);

        return "player/matching-project";
    }

    @GetMapping("/{id}/matching/rounds")
    public String showMatchingRounds(@PathVariable Long id, HttpServletRequest request, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        int currentRoundNum = tournament.getCurrentRound();
        if (currentRoundNum == 0) {
            model.addAttribute("isBeforeTournament", true);
            return "player/matching-rounds";
        }

        // Fetch user's entry info
        String sessionToken = (String) request.getAttribute("sessionToken");
        PlayerAccount account = (PlayerAccount) request.getAttribute("currentAccount");
        Long accountId = (account != null) ? account.getId() : null;
        Entry myEntry = entryService.getEntryForCurrentUser(id, sessionToken, accountId);
        model.addAttribute("myEntry", myEntry);

        // Build RoundDisplay objects for all rounds up to currentRound
        List<RoundDisplay> roundsList = new ArrayList<>();
        for (int r = 1; r <= currentRoundNum; r++) {
            TournamentRound roundObj = roundMapper.findByTournamentAndRound(id, r);
            if (roundObj != null) {
                List<MatchGame> matches = matchGameMapper.findByRoundId(roundObj.getId());
                roundsList.add(new RoundDisplay(r, matches));
            }
        }
        model.addAttribute("roundsList", roundsList);
        model.addAttribute("currentRoundNum", currentRoundNum);

        return "player/matching-rounds";
    }

    public static class RoundDisplay {
        private int roundNumber;
        private List<MatchGame> matches;

        public RoundDisplay(int roundNumber, List<MatchGame> matches) {
            this.roundNumber = roundNumber;
            this.matches = matches;
        }

        public int getRoundNumber() {
            return roundNumber;
        }

        public List<MatchGame> getMatches() {
            return matches;
        }
    }
}
