package com.example.webflux.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {
    public static final String WAIT_QUEUE_KEY = "wait:queue";
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Override
    public Mono<Boolean> addZSet(Long userId, Long timestamp) {
        return reactiveRedisTemplate.opsForZSet()
                .add(WAIT_QUEUE_KEY, userId.toString(), timestamp);
    }

    @Override
    public Mono<Long> zRank(Long userId) {
        return reactiveRedisTemplate.opsForZSet()
                .rank(WAIT_QUEUE_KEY, userId.toString());
    }
}
