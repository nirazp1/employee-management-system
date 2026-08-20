package com.example.ems.department.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        String description,
        ManagerSummary manager,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ManagerSummary(UUID id, String fullName, String jobTitle) {
    }
}
