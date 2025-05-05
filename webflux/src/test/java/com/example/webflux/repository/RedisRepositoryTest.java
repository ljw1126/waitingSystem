package com.example.webflux.repository;

import com.example.webflux.EmbeddedRedis;
import com.example.webflux.service.QueueManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
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

        StepVerifier.create(redisRepository.addZSet(queue, userId, timestamp))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void addZSetWhenDuplicated() {
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSet(queue, userId, timestamp))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(redisRepository.addZSet(queue, userId, timestamp))
                .expectNext(false)
                .verifyComplete();
    }


    @Test
    void zRank() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSet(queue,1L, 100L)
                .then(redisRepository.zRank(queue,1L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankByMultiUser() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSet( queue,1L, 100L)
                .then(redisRepository.addZSet(queue,2L, 99L))
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

        Mono<Boolean> setup = redisRepository.addZSet(queue, 1L, 100L)
                .then(redisRepository.addZSet(queue, 2L, 101L))
                .then(redisRepository.addZSet(queue, 3L, 103L));

        Flux<ZSetOperations.TypedTuple<String>> result = setup.thenMany(redisRepository.popMin(queue, 4L)); // Mono -> Flux

        StepVerifier.create(result)
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("1"))
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("2"))
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("3"))
                .verifyComplete();
    }
}
