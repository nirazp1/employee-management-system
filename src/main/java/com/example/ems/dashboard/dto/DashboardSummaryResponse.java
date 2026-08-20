package com.example.ems.dashboard.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        Map<String, Long> employeesByDepartment,
        long employeesOnLeave,
        AttendanceSummary todaysAttendance,
        long pendingLeaveRequests,
        PayrollTotals payrollTotals
) {
    public record AttendanceSummary(long present, long late, long halfDay, long absent, long totalRecorded) {
    }

    public record PayrollTotals(BigDecimal pending, BigDecimal processed, BigDecimal paid) {
    }
}
