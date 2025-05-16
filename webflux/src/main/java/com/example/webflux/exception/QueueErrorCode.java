package com.example.webflux.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QueueErrorCode {
    ALREADY_RESISTER_USER(HttpStatus.CONFLICT, "WQ-0001", "Already registered in queue"),
    NONE_EXIST_USER(HttpStatus.BAD_REQUEST, "WQ-0002", "None exist in queue");

    private final HttpStatus httpStatus;
    private final String code;
    private final String reason;

    public WaitingQueueException build() {
        return new WaitingQueueException(httpStatus, code, reason);
    }

    public WaitingQueueException build(Object ...args) {
        return new WaitingQueueException(httpStatus, code, reason.formatted(args));
    }
}
