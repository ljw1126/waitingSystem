package com.example.website.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.common.AllowedResponse;
import com.example.common.WaitingQueueRankResponse;
import com.example.website.service.WaitingQueueService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(controllers = HomeController.class)
class HomeControllerTest {

    private static final Long TEST_USER_ID = 123L;
    private static final String COOKIE_NAME = "user-queue-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaitingQueueService waitingQueueService;

    @DisplayName("토큰이 유효하면 메인 페이지로 이동한다")
    @Test
    void requestIndexWhenValidToken() throws Exception {
        String validToken = "valid-token";
        Cookie cookie = new Cookie(COOKIE_NAME, validToken);

        when(waitingQueueService.isAllowUser(any(), any()))
                .thenReturn(new AllowedResponse(true));

        mockMvc.perform(get("/index").param("userId", String.valueOf(TEST_USER_ID)).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @DisplayName("토큰이 유효하지 않으면 대기열 페이지로 이동한다")
    @Test
    void requestIndexWhenInvalidToken() throws Exception {
        Cookie invalidCookie = new Cookie(COOKIE_NAME, "invalid-token");

        when(waitingQueueService.isAllowUser(TEST_USER_ID, "invalid-token"))
                .thenReturn(new AllowedResponse(false));
        when(waitingQueueService.accessibleCheck(any()))
                .thenReturn(new WaitingQueueRankResponse(999L));

        mockMvc.perform(get("/index").param("userId", String.valueOf(TEST_USER_ID)).cookie(invalidCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("waiting-room"))
                .andExpect(model().attribute("rank", 999L));
    }

    @DisplayName("토큰이 없으면 대기열 페이지로 이동한다")
    @Test
    void requestIndexWithoutToken() throws Exception {
        when(waitingQueueService.accessibleCheck(any()))
                .thenReturn(new WaitingQueueRankResponse(999L));

        mockMvc.perform(get("/index").param("userId", String.valueOf(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(view().name("waiting-room"))
                .andExpect(model().attribute("rank", 999L));
    }
}
