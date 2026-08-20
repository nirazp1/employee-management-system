package com.example.ems.attendance.dto;

import com.example.ems.attendance.entity.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        LocalDate date,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        AttendanceStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
