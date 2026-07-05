package com.asopoketool.controller;

import com.asopoketool.model.TimerSetting;
import com.asopoketool.model.Tournament;
import com.asopoketool.service.TimerService;
import com.asopoketool.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tournament")
public class TimerController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TimerService timerService;

    @GetMapping("/{id}/timer")
    public String showTimer(@PathVariable Long id, Model model) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            return "redirect:/";
        }
        model.addAttribute("tournament", tournament);

        // Fetch round timer settings (using current round, fallback to 0 if not started)
        int round = tournament.getCurrentRound();
        TimerSetting setting = timerService.getTimerSetting(id, round);
        model.addAttribute("timerSetting", setting);

        return "player/timer-display";
    }
}
