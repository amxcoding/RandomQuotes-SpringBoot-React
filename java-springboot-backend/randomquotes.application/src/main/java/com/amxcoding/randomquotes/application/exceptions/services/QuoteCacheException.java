package com.amxcoding.randomquotes.application.exceptions.services;

import com.amxcoding.randomquotes.application.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class QuoteCacheException extends ApiException {
    public QuoteCacheException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public QuoteCacheException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
