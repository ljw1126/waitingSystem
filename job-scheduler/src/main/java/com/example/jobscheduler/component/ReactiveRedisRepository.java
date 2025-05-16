package com.example.jobscheduler.component;

import java.nio.ByteBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReactiveZSetCommands;
import org.springframework.data.redis.connection.zset.Tuple;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ReactiveRedisRepository {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public Flux<String> scan(String pattern, Long count) {
        return reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(count)
                .build());
    }

    public Flux<ZSetOperations.TypedTuple<String>> popMin(String queue, Long count) {
        return reactiveRedisTemplate.opsForZSet().popMin(queue, count);
    }

    public Mono<Boolean> addZSetIfAbsent(String queue, Long userId, Long timestamp) {
        ReactiveZSetCommands.ZAddCommand zAddCommand = ReactiveZSetCommands.ZAddCommand.tuple(Tuple.of(userId.toString().getBytes(), timestamp.doubleValue()))
                .nx().to(ByteBuffer.wrap(queue.getBytes()));

        return reactiveRedisTemplate.getConnectionFactory()
                .getReactiveConnection()
                .zSetCommands()
                .zAdd(Mono.just(zAddCommand))
                .next()
                .map(response -> response.getOutput() != null && response.getOutput().intValue() == 1);
    }

}
