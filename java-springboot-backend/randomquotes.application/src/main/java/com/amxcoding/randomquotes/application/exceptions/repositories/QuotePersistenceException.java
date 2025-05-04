package com.amxcoding.randomquotes.application.exceptions.repositories;

import com.amxcoding.randomquotes.application.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class QuotePersistenceException extends ApiException {
    public QuotePersistenceException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public QuotePersistenceException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
