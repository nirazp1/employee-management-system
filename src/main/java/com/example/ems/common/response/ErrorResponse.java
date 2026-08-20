package com.example.ems.common.response;

import java.time.LocalDateTime;

public record ErrorResponse(boolean success, ErrorDetail error, LocalDateTime timestamp, String path) {

    public record ErrorDetail(String code, String message) {
    }

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(false, new ErrorDetail(code, message), LocalDateTime.now(), path);
    }
}
