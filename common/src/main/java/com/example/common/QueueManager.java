package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QueueManager {
    WAITING_QUEUE("wait:queue"),
    PROCEED_QUEUE("proceed:queue");

    private final String key;
}
