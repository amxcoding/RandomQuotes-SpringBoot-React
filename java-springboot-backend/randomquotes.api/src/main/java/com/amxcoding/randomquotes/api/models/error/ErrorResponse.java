package com.amxcoding.randomquotes.api.models.error;

import java.time.LocalDateTime;

public class ErrorResponse {

    private int statusCode;
    private String message;
    private String details;
    private LocalDateTime timestamp;

    public ErrorResponse(int statusCode, String message, String details) {

        this.statusCode = statusCode;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
