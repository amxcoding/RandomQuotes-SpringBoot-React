package com.amxcoding.randomquotes.application.exceptions.services;

import com.amxcoding.randomquotes.application.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class QuoteFetchOrchestratorException extends ApiException {
    public QuoteFetchOrchestratorException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public QuoteFetchOrchestratorException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
