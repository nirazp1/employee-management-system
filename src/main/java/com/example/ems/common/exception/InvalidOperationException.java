package com.example.ems.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends ApiException {

    public InvalidOperationException(String message) {
        super("INVALID_OPERATION", HttpStatus.BAD_REQUEST, message);
    }
}
