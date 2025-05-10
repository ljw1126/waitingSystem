package com.example.webflux.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public class WaitingQueueException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String code;
    private final String reason;
}
