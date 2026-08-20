package com.example.ems.employee.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeUpdateRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotNull(message = "Hire date is required")
        @PastOrPresent(message = "Hire date cannot be in the future")
        LocalDate hireDate,

        @NotBlank(message = "Job title is required")
        @Size(max = 150, message = "Job title must not exceed 150 characters")
        String jobTitle,

        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Salary cannot be negative")
        BigDecimal salary,

        UUID departmentId
) {
}
