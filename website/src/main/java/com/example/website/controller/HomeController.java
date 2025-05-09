package com.example.website.controller;

import com.example.common.AllowedResponse;
import com.example.common.QueueStatusResponse;
import com.example.website.service.WaitingQueueService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    private final WaitingQueueService waitingQueueService;

    @GetMapping("/index")
    public String home(@ModelAttribute(name = "userId") Long userId, Model model, HttpServletRequest request) {
        String token = getToken(request);
        if(!token.isEmpty() && isValidToken(userId, token)) {
            return "index";
        }

        QueueStatusResponse response = waitingQueueService.accessibleCheck(userId);
        model.addAttribute("rank", response.rank());
        return "waiting-room";
    }

    private String getToken(HttpServletRequest request) {
        final String COOKIE_NAME = "user-queue-token";
        Cookie[] cookies = request.getCookies();
        String token = "";
        if (cookies != null) {
            Optional<Cookie> cookie = Arrays.stream(cookies).filter(i -> i.getName().equalsIgnoreCase(COOKIE_NAME)).findFirst();
            token = cookie.orElse(new Cookie("user-queue-token", "")).getValue();
        }

        return token;
    }

    private boolean isValidToken(Long userId, String token) {
        AllowedResponse allowUser = waitingQueueService.isAllowUser(userId, token);
        return allowUser != null && allowUser.allowed();
    }
}
