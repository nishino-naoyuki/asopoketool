package com.asopoketool.controller;

import com.asopoketool.mapper.MatchGameMapper;
import com.asopoketool.mapper.MatchResultMapper;
import com.asopoketool.model.Entry;
import com.asopoketool.model.MatchGame;
import com.asopoketool.model.MatchResult;
import com.asopoketool.model.Tournament;
import com.asopoketool.service.EntryService;
import com.asopoketool.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tournament")
public class ResultController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private MatchGameMapper matchGameMapper;

    @Autowired
    private MatchResultMapper matchResultMapper;

    @Autowired
    private EntryService entryService;

    @GetMapping("/{id}/result/{matchId}")
    public String showResultForm(@PathVariable Long id, @PathVariable Long matchId, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        MatchGame match = matchGameMapper.findById(matchId);
        if (match == null) {
            return "redirect:/tournament/" + id + "/matching";
        }
        model.addAttribute("match", match);

        return "player/result-register";
    }

    @PostMapping("/{id}/result/{matchId}")
    public String registerResult(@PathVariable Long id,
                                 @PathVariable Long matchId,
                                 @RequestParam Long winnerEntryId,
                                 Model model) {
        try {
            MatchResult existing = matchResultMapper.findByMatchId(matchId);
            if (existing != null) {
                existing.setWinnerEntryId(winnerEntryId);
                existing.setRegisteredBy("PLAYER");
                matchResultMapper.update(existing);
            } else {
                MatchResult result = MatchResult.builder()
                        .matchId(matchId)
                        .winnerEntryId(winnerEntryId)
                        .registeredBy("PLAYER")
                        .build();
                matchResultMapper.insert(result);
            }
            return "redirect:/tournament/" + id + "/matching?success=result";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return showResultForm(id, matchId, model);
        }
    }
}
