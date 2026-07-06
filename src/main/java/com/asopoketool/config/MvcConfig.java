package com.asopoketool.config;

import com.asopoketool.interceptor.AccountInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private AccountInterceptor accountInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Player Account auto-login interceptor
        registry.addInterceptor(accountInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/audio/**", "/images/**", "/admin/**");

        // Session Token generation interceptor (For guest tracking)
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String sessionToken = null;
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("SESSION_TOKEN".equals(cookie.getName())) {
                            sessionToken = cookie.getValue();
                            break;
                        }
                    }
                }

                if (sessionToken == null) {
                    sessionToken = UUID.randomUUID().toString();
                    Cookie cookie = new Cookie("SESSION_TOKEN", sessionToken);
                    cookie.setPath("/");
                    cookie.setMaxAge(60 * 60 * 24 * 365); // 1 year
                    cookie.setHttpOnly(true);
                    response.addCookie(cookie);
                }

                request.setAttribute("sessionToken", sessionToken);
                return true;
            }
        })
        .addPathPatterns("/**")
        .excludePathPatterns("/css/**", "/js/**", "/audio/**", "/images/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/audio/**").addResourceLocations("classpath:/static/audio/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
        // External BGM file location
        registry.addResourceHandler("/bgm-files/**").addResourceLocations("file:/opt/asopoketool/bgm/");
        // External Timer Background image location
        registry.addResourceHandler("/timer-bg-files/**").addResourceLocations("file:/opt/asopoketool/images/");
    }
}
