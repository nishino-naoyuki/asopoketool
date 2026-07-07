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
import java.util.Map;
import java.util.HashMap;

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

    @Autowired
    private com.asopoketool.service.BracketService bracketService;

    @Autowired
    private com.asopoketool.mapper.TournamentRoundMapper roundMapper;

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

    @PostMapping("/{id}/result/{matchId}/ajax")
    @ResponseBody
    public Map<String, Object> registerResultAjax(@PathVariable Long id,
                                                  @PathVariable Long matchId,
                                                  @RequestParam(required = false) Long winnerEntryId) {
        Map<String, Object> response = new HashMap<>();
        try {
            MatchResult existing = matchResultMapper.findByMatchId(matchId);
            if (existing != null) {
                response.put("success", false);
                response.put("message", "既に結果が登録されているため、変更できません。");
                return response;
            }

            MatchResult result = MatchResult.builder()
                    .matchId(matchId)
                    .winnerEntryId(winnerEntryId)
                    .registeredBy("PLAYER")
                    .build();
            matchResultMapper.insert(result);

            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/{id}/bracket/match/{matchId}/ajax")
    @ResponseBody
    public Map<String, Object> registerBracketResultAjax(@PathVariable Long id,
                                                         @PathVariable Long matchId,
                                                         @RequestParam Long winnerEntryId) {
        Map<String, Object> response = new HashMap<>();
        try {
            com.asopoketool.model.BracketMatch match = bracketService.getBracketsByTournament(id)
                    .stream()
                    .flatMap(b -> b.getMatches().stream())
                    .filter(m -> m.getId().equals(matchId))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                response.put("success", false);
                response.put("message", "マッチが見つかりません。");
                return response;
            }

            if (match.getWinnerEntryId() != null) {
                response.put("success", false);
                response.put("message", "既に結果が登録されているため、変更できません。");
                return response;
            }

            com.asopoketool.model.TournamentRound tr = roundMapper.findByTournamentAndRound(id, 100 + match.getRoundNumber());
            if (tr != null && "FINISHED".equals(tr.getStatus())) {
                response.put("success", false);
                response.put("message", "該当ラウンドの結果は既に確定しているため、変更できません。");
                return response;
            }

            bracketService.registerBracketResult(matchId, winnerEntryId);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
