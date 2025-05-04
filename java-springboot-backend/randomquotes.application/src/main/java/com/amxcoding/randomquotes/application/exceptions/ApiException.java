package com.amxcoding.randomquotes.application.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

public abstract class ApiException extends Exception {

    private final HttpStatus status;
    private final List<String> details;

    protected ApiException(String message, HttpStatus status) {
        super(message); // Exception message
        this.status = status;
        this.details = Collections.emptyList();
    }

    protected ApiException(String message, HttpStatus status, Throwable cause) {
        super(message, cause); // Exception message
        this.status = status;
        this.details = Collections.emptyList();
    }

    protected ApiException(String message, HttpStatus status, List<String> details) {
        super(message);
        this.status = status;
        this.details = details != null ? List.copyOf(details) : Collections.emptyList();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetails() {
        return details;
    }
}

