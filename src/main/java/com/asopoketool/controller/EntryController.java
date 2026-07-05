package com.asopoketool.controller;

import com.asopoketool.model.Entry;
import com.asopoketool.model.Tournament;
import com.asopoketool.service.EntryService;
import com.asopoketool.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

@Controller
@RequestMapping("/checkin")
public class EntryController {

    @Autowired
    private EntryService entryService;

    @Autowired
    private TournamentService tournamentService;

    @GetMapping("/qr/{entryId}")
    public String showQrCode(@PathVariable Long entryId, Model model) {
        Entry entry = entryService.getEntryById(entryId);
        if (entry == null) {
            return "redirect:/";
        }
        model.addAttribute("entry", entry);

        Tournament tournament = tournamentService.getTournamentById(entry.getTournamentId());
        model.addAttribute("tournament", tournament);

        return "player/qr-code";
    }

    @GetMapping(value = "/qr/{entryId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public void getQrImage(@PathVariable Long entryId, HttpServletResponse response) {
        try {
            byte[] imageBytes = entryService.generateQRCodeBytes(entryId);
            response.setContentType("image/png");
            try (OutputStream out = response.getOutputStream()) {
                out.write(imageBytes);
                out.flush();
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
