package com.amxcoding.randomquotes.application.exceptions.providers;

import com.amxcoding.randomquotes.application.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class QuoteProviderException extends ApiException {
    public QuoteProviderException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
