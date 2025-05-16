package com.example.webflux.service;

import static com.example.common.QueueManager.*;
import static com.example.webflux.exception.QueueErrorCode.*;

import com.example.webflux.repository.RedisRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {
    private final RedisRepository redisRepository;

    @Value("${scheduler.enabled}")
    private boolean scheduling = false;

    @Value("${scheduler.max-allow-user-count}")
    private long maxAllUserCount = 0;

    public Mono<Long> enqueueWaitingQueue(Long userId) {
        long unixTimestamp = Instant.now().getEpochSecond();
        String queue = WAITING_QUEUE.getKey();
        return redisRepository.addZSetIfAbsent(queue, userId, unixTimestamp)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(ALREADY_RESISTER_USER.build()))
                .flatMap(i -> rank(queue, userId));
    }

    public Mono<Long> allow(Long count) {
        return redisRepository.popMin(WAITING_QUEUE.getKey(), count)
                .flatMap(member -> redisRepository.addZSetIfAbsent(PROCEED_QUEUE.getKey(), Long.parseLong(Objects.requireNonNull(member.getValue())), Instant.now().getEpochSecond()))
                .count();
    }

    public Mono<Boolean> isAllowedByToken(Long userId, String token) {
        return this.createToken(userId)
                .filter(other -> other.equalsIgnoreCase(token))
                .map(i -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Long> checked(Long userId) {
        return enqueueWaitingQueue(userId)
                .onErrorResume(e -> rank(WAITING_QUEUE.getKey(), userId));
    }

    public Mono<Long> rank(String queue, Long userId) {
        return redisRepository.zRank(queue, userId)
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 ? rank + 1 : rank);
    }

    public Mono<String> generateToken(Long userId) {
        return rank(PROCEED_QUEUE.getKey(), userId)
                .filter(i -> i >= 0)
                .switchIfEmpty(Mono.error(NONE_EXIST_USER.build()))
                .flatMap(i -> createToken(userId));
    }

    private Mono<String> createToken(Long userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = "user-queue-%d".formatted(userId);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for(byte aByte : encodedHash) {
                hexString.append(String.format("%02x", aByte));
            }
            return Mono.just(hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
