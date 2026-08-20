package com.example.ems.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message);
    }
}
