package com.example.ems.payroll.dto;

import com.example.ems.payroll.entity.PayrollStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PayrollResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        LocalDate payPeriodStart,
        LocalDate payPeriodEnd,
        BigDecimal baseSalary,
        BigDecimal bonuses,
        BigDecimal deductions,
        BigDecimal netSalary,
        LocalDate paymentDate,
        PayrollStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
