package com.benbanking.api.exceptions;

import org.springframework.http.HttpStatus;

/** Base type for domain exceptions that carry an HTTP status for GlobalExceptionHandler. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
