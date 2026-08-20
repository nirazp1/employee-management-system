package com.example.ems.leave.dto;

import com.example.ems.leave.entity.LeaveStatus;
import com.example.ems.leave.entity.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LeaveRequestResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveStatus status,
        UUID approvedById,
        String approvedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
