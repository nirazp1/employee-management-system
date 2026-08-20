package com.example.ems.employee.dto;

import com.example.ems.employee.entity.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record EmployeeStatusUpdateRequest(

        @NotNull(message = "Status is required")
        EmployeeStatus status
) {
}
