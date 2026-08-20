package com.example.ems.employee.dto;

import com.example.ems.employee.entity.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        LocalDate hireDate,
        String jobTitle,
        BigDecimal salary,
        EmployeeStatus status,
        DepartmentSummary department,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record DepartmentSummary(UUID id, String name) {
    }
}
