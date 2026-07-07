package com.asopoketool.controller;

import com.asopoketool.model.PlayerAccount;
import com.asopoketool.model.PointHistory;
import com.asopoketool.service.AccountService;
import com.asopoketool.service.PointService;
import com.asopoketool.mapper.PointMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private PointMapper pointMapper;

    @GetMapping("/register")
    public String showRegisterForm(HttpServletRequest request, Model model) {
        if (Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/";
        }
        return "account/register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam String displayName,
                                  @RequestParam String password,
                                  @RequestParam String passwordConfirm,
                                  @RequestParam(required = false) String iconBase64,
                                  HttpServletResponse response,
                                  Model model) {
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("error", "パスワードと確認用パスワードが一致しません。");
            return "account/register";
        }

        try {
            String iconPath = null;
            if (iconBase64 != null && !iconBase64.trim().isEmpty()) {
                try {
                    String base64Data = iconBase64;
                    if (base64Data.contains(",")) {
                        base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                    }
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);

                    java.io.File dir = new java.io.File(System.getProperty("user.dir") + "/data/images");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    String filename = "user_icon_" + System.currentTimeMillis() + ".png";
                    java.io.File dest = new java.io.File(dir, filename);
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                        fos.write(decodedBytes);
                    }
                    iconPath = "/asopoketool/timer-bg-files/" + filename;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String token = accountService.register(displayName, password, iconPath);
            setAuthCookie(response, token);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "account/register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request) {
        if (Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/";
        }
        return "account/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String displayName,
                               @RequestParam String password,
                               HttpServletResponse response,
                               Model model) {
        try {
            String token = accountService.login(displayName, password);
            setAuthCookie(response, token);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "account/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("APT_TOKEN".equals(cookie.getName())) {
                    accountService.logout(cookie.getValue());
                    Cookie clearCookie = new Cookie("APT_TOKEN", null);
                    clearCookie.setPath("/");
                    clearCookie.setMaxAge(0);
                    response.addCookie(clearCookie);
                    break;
                }
            }
        }
        return "redirect:/";
    }

    @GetMapping("/mypage")
    public String showMyPage(HttpServletRequest request, Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        model.addAttribute("account", current);
        
        // Cumulative point & history
        model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
        List<PointHistory> histories = pointMapper.findHistoryByAccountId(current.getId());
        model.addAttribute("histories", histories);

        return "account/mypage";
    }

    @PostMapping("/update-name")
    public String updateDisplayName(HttpServletRequest request, @RequestParam String displayName, Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        try {
            accountService.updateDisplayName(current.getId(), displayName);
            current.setDisplayName(displayName); // reflect updated name in request context
            return "redirect:/account/mypage?success=name";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", current);
            model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
            model.addAttribute("histories", pointMapper.findHistoryByAccountId(current.getId()));
            return "account/mypage";
        }
    }

    @PostMapping("/update-password")
    public String updatePassword(HttpServletRequest request,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String newPasswordConfirm,
                                 Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        if (!newPassword.equals(newPasswordConfirm)) {
            model.addAttribute("error", "新しいパスワードと確認用が一致しません。");
            model.addAttribute("account", current);
            model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
            model.addAttribute("histories", pointMapper.findHistoryByAccountId(current.getId()));
            return "account/mypage";
        }

        try {
            accountService.updatePassword(current.getId(), currentPassword, newPassword);
            return "redirect:/account/mypage?success=password";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", current);
            model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
            model.addAttribute("histories", pointMapper.findHistoryByAccountId(current.getId()));
            return "account/mypage";
        }
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("APT_TOKEN", token);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 120); // 120 days persistent session
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // Uncomment if deploying on HTTPS
        response.addCookie(cookie);
    }

    @PostMapping("/update-icon")
    public String updateIconPath(HttpServletRequest request,
                                 @RequestParam(required = false) String iconBase64,
                                 Model model) {
        if (!Boolean.TRUE.equals(request.getAttribute("isLoggedIn"))) {
            return "redirect:/account/login";
        }

        PlayerAccount current = (PlayerAccount) request.getAttribute("currentAccount");
        String iconPath = null;
        if (iconBase64 != null && !iconBase64.trim().isEmpty()) {
            try {
                String base64Data = iconBase64;
                if (base64Data.contains(",")) {
                    base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                }
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);

                java.io.File dir = new java.io.File(System.getProperty("user.dir") + "/data/images");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String filename = "user_icon_" + System.currentTimeMillis() + ".png";
                java.io.File dest = new java.io.File(dir, filename);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    fos.write(decodedBytes);
                }
                iconPath = "/asopoketool/timer-bg-files/" + filename;
                accountService.updateIconPath(current.getId(), iconPath);
                current.setIconPath(iconPath);
                return "redirect:/account/mypage?success=icon";
            } catch (Exception e) {
                model.addAttribute("error", "アイコンの更新に失敗しました: " + e.getMessage());
            }
        }
        
        model.addAttribute("account", current);
        model.addAttribute("points", pointMapper.findByAccountId(current.getId()));
        model.addAttribute("histories", pointMapper.findHistoryByAccountId(current.getId()));
        return "account/mypage";
    }
}
