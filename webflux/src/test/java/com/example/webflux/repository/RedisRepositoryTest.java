package com.example.webflux.repository;

import com.example.webflux.EmbeddedRedis;
import com.example.webflux.service.QueueManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test")
public class RedisRepositoryTest {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private RedisRepositoryImpl redisRepository;

    @BeforeEach
    void setUp() {
        redisRepository = new RedisRepositoryImpl(reactiveRedisTemplate);

        ReactiveRedisConnection reactiveConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        reactiveConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void addZSet() {
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, timestamp))
                .expectNext(true)
                .verifyComplete();
    }

    @DisplayName("대기열 큐에 등록된 사용자의 경우 false를 반환한다")
    @Test
    void addZSetWhenDuplicated() {
        Long userId = 1L;
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, Instant.now().getEpochSecond()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, Instant.now().getEpochSecond()))
                .expectNext(false)
                .verifyComplete();
    }


    @Test
    void zRank() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue,1L, 100L)
                .then(redisRepository.zRank(queue,1L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankByMultiUser() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent( queue,1L, 100L)
                .then(redisRepository.addZSetIfAbsent(queue,2L, 99L))
                .then(redisRepository.zRank( queue,2L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankByNoneUserId() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.zRank(queue,99L))
                .expectComplete()
                .verify();
    }

    @Test
    void popMin() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        Mono<Boolean> setup = redisRepository.addZSetIfAbsent(queue, 1L, 100L)
                .then(redisRepository.addZSetIfAbsent(queue, 2L, 101L))
                .then(redisRepository.addZSetIfAbsent(queue, 3L, 103L));

        Flux<ZSetOperations.TypedTuple<String>> result = setup.thenMany(redisRepository.popMin(queue, 4L)); // Mono -> Flux

        StepVerifier.create(result)
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("1"))
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("2"))
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("3"))
                .verifyComplete();
    }

    @Test
    void scanWhenEmpty() {
        String pattern = "wait:*";
        Long count = 100L;

        StepVerifier.create(redisRepository.scan(pattern, count).collectList())
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void scanWhenExistQueueData() {
        String queue = QueueManager.WAITING_QUEUE.getKey();
        String pattern = "wait:*";
        Long count = 100L;

        Mono<Boolean> setup = redisRepository.addZSetIfAbsent(queue, 1L, 100L)
                .then(redisRepository.addZSetIfAbsent(queue, 2L, 101L))
                .then(redisRepository.addZSetIfAbsent(queue, 3L, 103L));

        StepVerifier.create(setup.thenMany(redisRepository.scan(pattern, count).collectList()))
                .expectNextMatches(list -> list.contains(queue)) // wait:queue
                .verifyComplete();
    }

    @Test
    void luaScript() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsentAndRank(queue, 1L, 100L)
                .then(redisRepository.addZSetIfAbsentAndRank(queue, 2L, 102L))
                .then(redisRepository.addZSetIfAbsentAndRank(queue, 3L, 102L))
                .then(redisRepository.addZSetIfAbsentAndRank(queue, 4L, 103L)))
                .expectNext(3L)
                .verifyComplete();
    }
}
