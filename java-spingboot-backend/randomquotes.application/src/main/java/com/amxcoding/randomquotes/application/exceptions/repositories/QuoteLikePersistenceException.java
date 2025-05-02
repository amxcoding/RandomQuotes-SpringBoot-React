package com.amxcoding.randomquotes.application.exceptions.repositories;

import com.amxcoding.randomquotes.application.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class QuoteLikePersistenceException extends ApiException {
    public QuoteLikePersistenceException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
