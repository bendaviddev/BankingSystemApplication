package com.benbanking.api.exceptions;

import org.springframework.http.HttpStatus;

/** Generic 400 for validation-style failures not covered by a more specific exception. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
