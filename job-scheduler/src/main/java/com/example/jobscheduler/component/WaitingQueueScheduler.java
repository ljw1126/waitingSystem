package com.example.jobscheduler.component;

import static com.example.common.QueueManager.*;

import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.zset.Tuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueueScheduler {
    private final ReactiveRedisRepository redisRepository;

    @Value("${scheduler.enabled}")
    private boolean scheduling = false;

    @Value("${scheduler.max-allow-user-count}")
    private long maxAllUserCount = 0;

    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    private void allowWaitingQueueUser() {
        if(!scheduling) {
            log.info("passed scheduling...");
            return;
        }

        log.info("process scheduling...");
        redisRepository.scan("wait:*", 100L)
                .flatMap(queue -> allow(queue, maxAllUserCount).map(allowedCount -> Tuple.of(queue.getBytes(), allowedCount.doubleValue())))
                .doOnNext(tuple -> log.info("Tried %d and allowed %d members of %s queue".formatted(maxAllUserCount, tuple.getScore().longValue(), new String(tuple.getValue()))))
                .subscribe();
    }

    private Mono<Long> allow(String queue, Long count) {
        return redisRepository.popMin(queue, count)
                .flatMap(member -> redisRepository.addZSetIfAbsent(PROCEED_QUEUE.getKey(), Long.parseLong(Objects.requireNonNull(member.getValue())), Instant.now().getEpochSecond()))
                .count();
    }

}
