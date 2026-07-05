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

    @GetMapping
    public String index(Model model) {
        List<Tournament> active = tournamentService.getActiveTournaments();
        model.addAttribute("tournaments", active);
        return "player/tournament-list";
    }

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
            entryService.enterTournament(id, playerName, null, sessionToken);
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
            entryService.enterTournament(id, account.getDisplayName(), account.getId(), sessionToken);
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
            entryService.enterTournament(id, playerName, account.getId(), sessionToken);
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
    public String bracket(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        List<BracketTournament> brackets = bracketService.getBracketsByTournament(id);
        model.addAttribute("brackets", brackets);

        return "player/bracket";
    }
}
