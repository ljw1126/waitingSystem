package com.example.webflux.service;

import static com.example.webflux.exception.QueueErrorCode.*;
import static com.example.webflux.service.QueueManager.*;

import com.example.webflux.repository.RedisRepository;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final RedisRepository redisRepository;

    public Mono<Long> enqueueWaitingQueue(Long userId) {
        long unixTimestamp = Instant.now().getEpochSecond();
        String queue = WAITING_QUEUE.getKey();
        return redisRepository.addZSetIfAbsent(queue, userId, unixTimestamp)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(ALREADY_RESISTER_USER.build()))
                .flatMap(i -> redisRepository.zRank(queue, userId))
                .map(i -> i >= 0 ? i + 1 : i);
    }

    public Mono<Long> allow(Long count) {
        return redisRepository.popMin(WAITING_QUEUE.getKey(), count)
                .flatMap(member -> redisRepository.addZSetIfAbsent(PROCEED_QUEUE.getKey(), Long.parseLong(Objects.requireNonNull(member.getValue())), Instant.now().getEpochSecond()))
                .count();
    }

    public Mono<Boolean> isAllowed(Long userId) {
        return redisRepository.zRank(PROCEED_QUEUE.getKey(), userId)
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0);
    }

    public Mono<Long> checked(Long userId) {
        return isAllowed(userId)
                .filter(Boolean::booleanValue)
                .flatMap(allowed -> Mono.just(0L))
                .switchIfEmpty(enqueueWaitingQueue(userId)
                                .onErrorResume(ex -> redisRepository.zRank(WAITING_QUEUE.getKey(), userId).map(i -> i >= 0 ? i + 1 : i))
                );
    }
}
