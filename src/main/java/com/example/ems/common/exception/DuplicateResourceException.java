package com.example.ems.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", HttpStatus.CONFLICT, message);
    }
}
