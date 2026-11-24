package com.asopoketool.controller;

import com.asopoketool.mapper.PointMapper;
import com.asopoketool.model.PlayerAccount;
import com.asopoketool.model.PlayerCumulativePoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/ranking")
public class RankingController {

    @Autowired
    private PointMapper pointMapper;

    @GetMapping
    public String showCumulativeRanking(HttpServletRequest request, Model model) {
        List<PlayerCumulativePoint> ranking = pointMapper.findAllAccountsWithPointsOrderByPoint();

        // Assign standard competition ranks (e.g. 1, 2, 2, 4)
        int prevPoint = -1;
        int currentRank = 0;
        for (int i = 0; i < ranking.size(); i++) {
            PlayerCumulativePoint item = ranking.get(i);
            if (i == 0 || item.getTotalPoint() != prevPoint) {
                currentRank = i + 1;
                prevPoint = item.getTotalPoint();
            }
            item.setRank(currentRank);
        }

        // Check logged-in user's rank and gap to next higher rank
        PlayerAccount currentAccount = (PlayerAccount) request.getAttribute("currentAccount");
        PlayerCumulativePoint myRankEntry = null;
        Integer pointsToNextRank = null;
        Integer nextRankNumber = null;

        if (currentAccount != null) {
            for (int i = 0; i < ranking.size(); i++) {
                PlayerCumulativePoint item = ranking.get(i);
                if (currentAccount.getId().equals(item.getAccountId())) {
                    myRankEntry = item;
                    // Find nearest player with strictly higher points
                    for (int j = i - 1; j >= 0; j--) {
                        if (ranking.get(j).getTotalPoint() > item.getTotalPoint()) {
                            pointsToNextRank = ranking.get(j).getTotalPoint() - item.getTotalPoint();
                            nextRankNumber = ranking.get(j).getRank();
                            break;
                        }
                    }
                    break;
                }
            }
        }

        model.addAttribute("ranking", ranking);
        model.addAttribute("top3", ranking.size() > 3 ? ranking.subList(0, 3) : ranking);
        model.addAttribute("restRanking", ranking.size() > 3 ? ranking.subList(3, ranking.size()) : java.util.Collections.emptyList());
        model.addAttribute("myRankEntry", myRankEntry);
        model.addAttribute("pointsToNextRank", pointsToNextRank);
        model.addAttribute("nextRankNumber", nextRankNumber);
        model.addAttribute("currentYear", LocalDate.now().getYear());

        return "player/cumulative-ranking";
    }
}
