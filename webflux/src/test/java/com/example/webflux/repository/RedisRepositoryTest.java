package com.example.webflux.repository;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
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
        String queue = "default";
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();

        StepVerifier.create(redisRepository.addZSet(userId, timestamp))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void addZSetWhenDuplicated() {
        String queue = "default";
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();

        StepVerifier.create(redisRepository.addZSet(userId, timestamp))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(redisRepository.addZSet(userId, timestamp))
                .expectNext(false)
                .verifyComplete();
    }


    @Test
    void zRank() {
        StepVerifier.create(redisRepository.addZSet(1L, 100L)
                .then(redisRepository.zRank(1L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankByMultiUser() {
        StepVerifier.create(redisRepository.addZSet( 1L, 100L)
                .then(redisRepository.addZSet(2L, 99L))
                .then(redisRepository.zRank( 2L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankByNoneUserId() {
        StepVerifier.create(redisRepository.zRank(99L))
                .expectComplete()
                .verify();
    }
}
