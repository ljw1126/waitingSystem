package com.example.jobscheduler;

import com.example.common.QueueManager;
import com.example.jobscheduler.component.ReactiveRedisRepository;
import java.time.Instant;
import java.util.List;
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
class ReactiveRedisRepositoryTest {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private ReactiveRedisRepository redisRepository;

    @BeforeEach
    void setUp() {
        redisRepository = new ReactiveRedisRepository(reactiveRedisTemplate);

        ReactiveRedisConnection reactiveConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        reactiveConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void scan() {
        String pattern = "wait:*";
        Long count = 100L;

        StepVerifier.create(redisRepository.scan(pattern, count).collectList())
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }
    
    @Test
    void popMin() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        Mono<Boolean> setup = redisRepository.addZSetIfAbsent(queue, 1L, 200L)
                .then(redisRepository.addZSetIfAbsent(queue, 2L, 201L))
                .then(redisRepository.addZSetIfAbsent(queue, 3L, 203L));

        Flux<ZSetOperations.TypedTuple<String>> result = setup.thenMany(redisRepository.popMin(queue, 4L));

        StepVerifier.create(result)
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("1"))
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("2"))
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("3"))
                .verifyComplete();
    }
    
    @Test
    void addZSetIfAbsent() {
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, timestamp))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void duplicatedAddZSetIfAbsent() {
        Long userId = 1L;
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, 100L)
                        .then(redisRepository.addZSetIfAbsent(queue, userId, 101L)))
                .expectNext(false)
                .verifyComplete();
    }
}
