package com.amxcoding.randomquotes.api.models.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "error")
public class ErrorResponse {

    @JacksonXmlProperty(localName = "statuscode")
    private int statusCode;
    @JacksonXmlProperty(localName = "message")
    private String message;
    @JacksonXmlProperty(localName = "details")
    private String details;
    @JacksonXmlProperty(localName = "timestamp")
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
