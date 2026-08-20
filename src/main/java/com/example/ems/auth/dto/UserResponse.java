package com.example.ems.auth.dto;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Set<String> roles,
        UUID employeeId
) {
}
