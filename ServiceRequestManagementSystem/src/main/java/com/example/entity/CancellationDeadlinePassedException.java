package com.example.entity;

import java.time.LocalDateTime;

public class CancellationDeadlinePassedException extends RuntimeException {

    public CancellationDeadlinePassedException(ExecutionMode executionMode, LocalDateTime deadline) {
        super("A scheduled " + executionMode + " service can be cancelled at the latest at " + deadline);
    }
}
