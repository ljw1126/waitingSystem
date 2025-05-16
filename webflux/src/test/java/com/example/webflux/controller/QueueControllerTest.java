package com.example.webflux.controller;

import static com.example.webflux.exception.QueueErrorCode.ALREADY_RESISTER_USER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.common.AllowedResponse;
import com.example.webflux.config.ServerExceptionResponse;
import com.example.webflux.controller.dto.AllowResultResponse;
import com.example.webflux.controller.dto.RankNumberResponse;
import com.example.webflux.controller.dto.WaitingQueueResponse;
import com.example.webflux.service.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(QueueController.class)
class QueueControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private QueueService queueService;

    @DisplayName("대기열 큐에 유저를 등록한다")
    @Test
    void enqueueWaitingQueue() {
        Long givenUserId = 1L;
        Long givenRank = 99L;

        when(queueService.enqueueWaitingQueue(givenUserId))
                .thenReturn(Mono.just(givenRank));

        webTestClient.post()
                .uri("/api/v1/waiting/queue?userId=" + givenUserId)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(WaitingQueueResponse.class)
                .value(response -> {
                    assertThat(response.getRank()).isEqualTo(givenRank);
                });
    }

    @DisplayName("이미 등록된 사용자인 경우 예외를 응답한다")
    @Test
    void enqueueWaitingQueueThrowException() {
        when(queueService.enqueueWaitingQueue(99L))
                .thenThrow(ALREADY_RESISTER_USER.build());

        webTestClient
            .post()
            .uri("/api/v1/waiting/queue?userId=99")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ServerExceptionResponse.class)
            .value(
                response -> {
                  assertThat(response.code()).isEqualTo(ALREADY_RESISTER_USER.getCode());
                  assertThat(response.reason()).isEqualTo(ALREADY_RESISTER_USER.getReason());
                });
    }

    @DisplayName("요청 카운트 수만큼 대기열에서 유저를 허용하고 허용한 유저 카운트를 응답한다")
    @Test
    void allow() {
        Long requestCount = 100L;
        Long allowedCount = 99L;

        when(queueService.allow(requestCount))
                .thenReturn(Mono.just(allowedCount));

        webTestClient.post()
                .uri("/api/v1/waiting/queue/allow?count=" + requestCount)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(AllowResultResponse.class)
                .value(response -> {
                    assertThat(response.getRequestCount()).isEqualTo(requestCount);
                    assertThat(response.getAllowedCount()).isEqualTo(allowedCount);
                });
    }

    @Test
    void isAllowed() {
        Long userId = 100L;
        String token = "2d5d9b49e5991835ad5080d8d68ad34f43edd862df267bc2fa82bba5eb31135f";

        when(queueService.isAllowedByToken(userId, token))
                .thenReturn(Mono.just(true));

        webTestClient.get()
                .uri(uriBuilder -> {
                    return uriBuilder.path("/api/v1/queue/allowed")
                            .queryParam("userId", userId)
                            .queryParam("token", token)
                            .build();
                })
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(AllowedResponse.class)
                .value(response -> assertThat(response.allowed()).isTrue());
    }

    @DisplayName("대기열 큐에서 유저의 순위를 응답한다")
    @Test
    void rank() {
        Long userId = 100L;
        Long rank = 2L;

        when(queueService.rank(anyString(), anyLong()))
                .thenReturn(Mono.just(rank));

        webTestClient.get()
                .uri("/api/v1/waiting/queue/rank?userId=" + userId)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(RankNumberResponse.class)
                .value(response -> {
                    assertThat(response.rank()).isEqualTo(rank);
                });
    }

    @Test
    void checked() {
        Long userId = 100L;
        Long expectedRank = 99L;

        when(queueService.checked(userId))
                .thenReturn(Mono.just(99L));

        webTestClient.get()
                .uri("/api/v1/waiting/queue/checked?userId=" + userId)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(WaitingQueueResponse.class)
                .value(response -> response.getRank().equals(expectedRank));
    }

    @DisplayName("허용된 사용자 아이디로 토큰 생성하여 응답한다")
    @Test
    void touch() {
        Long userId = 100L;
        String token = "2d5d9b49e5991835ad5080d8d68ad34f43edd862df267bc2fa82bba5eb31135f";

        when(queueService.generateToken(userId))
                .thenReturn(Mono.just(token));

        webTestClient.get()
                .uri("/api/v1/touch?userId=" + userId)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectCookie()
                .value("user-queue-token", s -> assertThat(s).isEqualToIgnoringCase(token));

    }
}
