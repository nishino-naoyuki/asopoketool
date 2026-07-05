package com.asopoketool.controller;

import com.asopoketool.mapper.PointMapper;
import com.asopoketool.model.PlayerCumulativePoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/ranking")
public class RankingController {

    @Autowired
    private PointMapper pointMapper;

    @GetMapping
    public String showCumulativeRanking(Model model) {
        List<PlayerCumulativePoint> ranking = pointMapper.findAllOrderByPoint();
        model.addAttribute("ranking", ranking);
        return "player/cumulative-ranking";
    }
}
