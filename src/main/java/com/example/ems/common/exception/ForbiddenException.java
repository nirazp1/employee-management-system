package com.example.ems.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", HttpStatus.FORBIDDEN, message);
    }
}
