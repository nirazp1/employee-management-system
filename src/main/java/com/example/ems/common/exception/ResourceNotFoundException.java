package com.example.ems.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }

    public static ResourceNotFoundException of(String resource, Object identifier) {
        return new ResourceNotFoundException(resource + " not found with identifier: " + identifier);
    }
}
