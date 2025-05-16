package com.example.webflux.controller;

import static com.example.webflux.service.QueueManager.*;

import com.example.common.AllowedResponse;
import com.example.common.WaitingQueueRankResponse;
import com.example.webflux.controller.dto.AllowResultResponse;
import com.example.webflux.controller.dto.RankNumberResponse;
import com.example.webflux.controller.dto.WaitingQueueResponse;
import com.example.webflux.service.QueueService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class QueueController {
    private final QueueService queueService;

    @PostMapping("/waiting/queue")
    public Mono<WaitingQueueResponse> enqueueWaitingQueue(@RequestParam(name = "userId") Long userId) {
        return queueService.enqueueWaitingQueue(userId)
                .map(WaitingQueueResponse::new);
    }

    @PostMapping("/waiting/queue/allow")
    public Mono<AllowResultResponse> allow(@RequestParam(name = "count") Long count) {
        return queueService.allow(count)
            .map(allowedCount -> new AllowResultResponse(count, allowedCount));
    }

    @GetMapping("/queue/allowed")
    public Mono<AllowedResponse> isAllowed(@RequestParam("userId") Long userId, @RequestParam("token") String token) {
        return queueService.isAllowedByToken(userId, token)
                .map(AllowedResponse::new);
    }

    @GetMapping("/waiting/queue/rank")
    public Mono<RankNumberResponse> rank(@RequestParam("userId") Long userId) {
        return queueService.rank(WAITING_QUEUE.getKey(), userId)
                .map(RankNumberResponse::new);
    }

    @GetMapping("/waiting/queue/checked")
    public Mono<WaitingQueueRankResponse> checked(@RequestParam("userId") Long userId) {
        return queueService.checked(userId)
                .map(WaitingQueueRankResponse::new);
    }

    private static final String USER_QUEUE_TOKEN = "user-queue-token";

    @GetMapping("/touch")
    public Mono<String> touch(@RequestParam("userId") Long userId, ServerWebExchange exchange) {
        log.info("touch : {}", userId);

        return Mono.defer(() -> queueService.generateToken(userId))
            .map(token -> {
                  exchange.getResponse().addCookie(ResponseCookie.from(USER_QUEUE_TOKEN, token)
                          .maxAge(Duration.ofSeconds(300))
                          .path("/")
                          .build());
                  return token;
                });
    }
}
