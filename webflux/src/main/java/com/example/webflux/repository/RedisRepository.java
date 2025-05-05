package com.example.webflux.repository;

import reactor.core.publisher.Mono;

public interface RedisRepository {
    Mono<Boolean> addZSet(Long userId, Long timestamp);

    Mono<Long> zRank(Long userId);
}
