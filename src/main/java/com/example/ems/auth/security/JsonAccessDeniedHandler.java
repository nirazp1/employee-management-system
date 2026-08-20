package com.example.ems.auth.security;

import com.example.ems.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    // Note: most of our 403s never actually land here. @PreAuthorize denials on controller
    // methods throw AccessDeniedException while a controller is executing, so those get picked
    // up by GlobalExceptionHandler's own @ExceptionHandler(AccessDeniedException.class) first,
    // same as any other exception a controller throws. This handler is the fallback for an
    // AccessDeniedException raised outside that path - e.g. straight from the security filter
    // chain, if we ever add role-restricted URL patterns via authorizeHttpRequests() instead
    // of method-level checks. Kept for defense-in-depth so that path also gets our JSON error
    // shape instead of Spring's default blank 403, even though nothing exercises it today.
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = ErrorResponse.of("FORBIDDEN",
                "You do not have permission to perform this action", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
