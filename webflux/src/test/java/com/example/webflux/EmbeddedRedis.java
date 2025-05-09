package com.example.webflux;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

@TestConfiguration
public class EmbeddedRedis {
    private final RedisServer redisServer;

    public EmbeddedRedis(@Value("${spring.data.redis.port}") int port) throws IOException {
        this.redisServer = new RedisServer(port);
    }

    @PostConstruct
    public void start() throws IOException {
        this.redisServer.start();
    }

    @PreDestroy
    public void cleanup() throws IOException {
        this.redisServer.stop();
    }
}
