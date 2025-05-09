package com.example.webflux.config;

import com.example.webflux.exception.WaitingQueueException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalRestControllerAdvice {
    @ExceptionHandler(WaitingQueueException.class)
    public Mono<ResponseEntity<ServerExceptionResponse>> handleWaitingQueueException(WaitingQueueException e) {
        return Mono.just(ResponseEntity.status(e.getHttpStatus()).body(new ServerExceptionResponse(e.getCode(), e.getReason())));
    }
}
