package com.asopoketool.interceptor;

import com.asopoketool.model.PlayerAccount;
import com.asopoketool.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AccountInterceptor implements HandlerInterceptor {

    @Autowired
    private AccountService accountService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("APT_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            PlayerAccount account = accountService.findByToken(token);
            if (account != null) {
                request.setAttribute("currentAccount", account);
                request.setAttribute("isLoggedIn", true);
                return true;
            }
        }

        request.setAttribute("currentAccount", null);
        request.setAttribute("isLoggedIn", false);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            modelAndView.addObject("currentAccount", request.getAttribute("currentAccount"));
            modelAndView.addObject("isLoggedIn", request.getAttribute("isLoggedIn"));
        }
    }
}
