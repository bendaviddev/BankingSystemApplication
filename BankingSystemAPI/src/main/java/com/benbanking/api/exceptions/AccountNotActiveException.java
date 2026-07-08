package com.benbanking.api.exceptions;

import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends ApiException {
    public AccountNotActiveException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
