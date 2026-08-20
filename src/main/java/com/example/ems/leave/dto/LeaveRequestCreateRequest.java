package com.example.ems.leave.dto;

import com.example.ems.leave.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveRequestCreateRequest(

        @NotNull(message = "Leave type is required")
        LeaveType leaveType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        String reason
) {
}
