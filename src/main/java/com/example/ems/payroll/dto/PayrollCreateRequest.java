package com.example.ems.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayrollCreateRequest(

        @NotNull(message = "Employee id is required")
        UUID employeeId,

        @NotNull(message = "Pay period start date is required")
        LocalDate payPeriodStart,

        @NotNull(message = "Pay period end date is required")
        LocalDate payPeriodEnd,

        @NotNull(message = "Base salary is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Base salary cannot be negative")
        BigDecimal baseSalary,

        @DecimalMin(value = "0.0", inclusive = true, message = "Bonuses cannot be negative")
        BigDecimal bonuses,

        @DecimalMin(value = "0.0", inclusive = true, message = "Deductions cannot be negative")
        BigDecimal deductions,

        LocalDate paymentDate
) {
}
