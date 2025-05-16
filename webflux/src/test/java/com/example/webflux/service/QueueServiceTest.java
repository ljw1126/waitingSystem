package com.example.webflux.service;

import static com.example.common.QueueManager.*;

import com.example.webflux.EmbeddedRedis;
import com.example.webflux.exception.WaitingQueueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test")
class QueueServiceTest {
    @Autowired
    private QueueService queueService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @BeforeEach
    void setUp() {
        ReactiveRedisConnection reactiveConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        reactiveConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void enqueueWaitingQueue() {
        StepVerifier.create(queueService.enqueueWaitingQueue(1L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(queueService.enqueueWaitingQueue(2L))
                .expectNext(2L)
                .verifyComplete();
    }

    @DisplayName("이미 등록된 유저가 재시도하는 경우 예외를 던진다")
    @Test
    void alreadyEnqueueWaitingQueue() {
        StepVerifier.create(queueService.enqueueWaitingQueue(1L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(queueService.enqueueWaitingQueue(1L))
                .expectError(WaitingQueueException.class)
                .verify();
    }

    @DisplayName("대기열 큐에 유저가 없는 경우 0을 반환한다")
    @Test
    void emptyAllowUser() {
        StepVerifier.create(queueService.allow(100L))
                .expectNext(0L)
                .verifyComplete();
    }

    @DisplayName("대기열 큐에 허용한 유저 수 만큼 카운팅을 반환한다")
    @Test
    void allowUser() {
        Mono<Long> setup = queueService.enqueueWaitingQueue(1L)
                .then(queueService.enqueueWaitingQueue(2L))
                .then(queueService.enqueueWaitingQueue(3L))
                .then(queueService.enqueueWaitingQueue(4L))
                .then(queueService.enqueueWaitingQueue(5L));

        StepVerifier.create(setup.then(queueService.allow(3L)))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void isNotAllowedByToken() {
        Long userId = 100L;
        String token = "empty";

        StepVerifier.create(queueService.isAllowedByToken(userId, token))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isAllowedByToken() {
        Long userId = 100L;
        String token = "2d5d9b49e5991835ad5080d8d68ad34f43edd862df267bc2fa82bba5eb31135f";

        StepVerifier.create(queueService.isAllowedByToken(userId, token))
                .expectNext(true)
                .verifyComplete();
    }

    @DisplayName("대기열에 없는 유저인 경우 대기열에 추가된 후 순위 랭킹을 반환한다")
    @Test
    void checkedWhenEmptyUserId() {
        Long userId = 102L;

        StepVerifier.create(queueService.enqueueWaitingQueue(100L)
                        .then(queueService.enqueueWaitingQueue(101L))
                        .then(queueService.checked(userId))
                ).expectNext(3L)
                .verifyComplete();
    }

    @DisplayName("대기열에 있는 유저의 경우 대기열 순위 랭킹을 반환한다")
    @Test
    void checkedWhenExistWaitingQueue() {
        Long userId = 102L;

        StepVerifier.create(queueService.enqueueWaitingQueue(100L)
                        .then(queueService.enqueueWaitingQueue(101L))
                        .then(queueService.enqueueWaitingQueue(userId))
                        .then(queueService.checked(userId))
                ).expectNext(3L)
                .verifyComplete();
    }

    @Test
    void generateTokenWhenNoneExistUser() {
        Long userId = 100L;

        StepVerifier.create(queueService.generateToken(userId))
            .expectErrorMatches(
                throwable ->
                    throwable instanceof WaitingQueueException
                        && ((WaitingQueueException) throwable).getHttpStatus().is4xxClientError())
            .verify();
    }

    @Test
    void successGenerateToken() {
        Long userId = 100L;
        String expectedToken = "2d5d9b49e5991835ad5080d8d68ad34f43edd862df267bc2fa82bba5eb31135f";

        StepVerifier.create(queueService.enqueueWaitingQueue(userId)
                .then(queueService.allow(1L))
                .then(queueService.generateToken(userId)))
                .expectNext(expectedToken)
                .verifyComplete();
    }

    @DisplayName("대기열에 없는 유저의 경우 -1을 반환한다")
    @Test
    void rankWhenNoneRegisterUserId() {
        String queue = WAITING_QUEUE.getKey();
        Long userId = 99L;

        StepVerifier.create(queueService.rank(queue, userId))
                .expectNext(-1L)
                .verifyComplete();
    }

    @DisplayName("대기열에 등록된 유저의 경우 순위를 반환한다")
    @Test
    void rank() {
        String queue = WAITING_QUEUE.getKey();
        Long userId = 101L;

        StepVerifier.create(queueService.enqueueWaitingQueue(100L)
                        .then(queueService.enqueueWaitingQueue(userId))
                        .then(queueService.enqueueWaitingQueue(102L))
                        .then(queueService.rank(queue, userId)))
                .expectNext(2L)
                .verifyComplete();
    }
}
