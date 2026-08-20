package com.example.ems.auth.security;

import com.example.ems.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    // This exists purely because @RestControllerAdvice (GlobalExceptionHandler) can only catch
    // exceptions thrown while a controller method is running - a missing/invalid token gets
    // rejected in the security filter chain, before the request ever reaches a controller. So
    // I'm hand-writing the same { success:false, error:{...} } JSON shape here to keep every
    // error response consistent, instead of letting Spring Security fall back to its default
    // (a bare 401 with no body).
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = ErrorResponse.of("UNAUTHENTICATED",
                "Authentication is required to access this resource", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
