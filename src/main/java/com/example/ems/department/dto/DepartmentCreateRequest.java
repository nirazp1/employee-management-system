package com.example.ems.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DepartmentCreateRequest(

        @NotBlank(message = "Department name is required")
        @Size(max = 150, message = "Department name must not exceed 150 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        UUID managerId
) {
}
