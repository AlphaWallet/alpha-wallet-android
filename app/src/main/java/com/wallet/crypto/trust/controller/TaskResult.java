package com.wallet.crypto.trust.controller;

/**
 * Created by marat on 10/15/17.
 */

public class TaskResult {
    private TaskStatus status;
    private String message;

    public TaskResult(TaskStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
