package com.example.website.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.common.AllowedResponse;
import com.example.common.QueueStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class WaitingQueueServiceTest {

    @Mock
    ExchangeFunction exchangeFunction;

    WaitingQueueService waitingQueueService;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .baseUrl("http://localhost:9090")
                .build();

        waitingQueueService = new WaitingQueueService(webClient);
    }

    @DisplayName("대기열 등록 후 순위를 반환한다")
    @Test
    void accessibleCheck() {
        Long userId = 1L;
        Long expectedRank = 123L;

        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("""
                            {
                                "accessible" : false,
                                "rank" : 123
                            }
                        """)
                .build();

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(clientResponse));

        QueueStatusResponse result = waitingQueueService.accessibleCheck(userId);

        assertThat(result.accessible()).isFalse();
        assertThat(result.rank()).isEqualTo(expectedRank);
    }

    @DisplayName("토큰 유효성 검사 결과를 응답한다")
    @Test
    void allow() {
        Long userId = 1L;
        String token = "valid-token";

        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("""
                            {   
                                "allowed" : true 
                            }
                        """).build();

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(clientResponse));

        AllowedResponse result = waitingQueueService.isAllowUser(userId, token);

        assertThat(result.allowed()).isTrue();
    }
}
