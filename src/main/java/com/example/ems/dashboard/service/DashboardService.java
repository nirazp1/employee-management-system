package com.example.ems.dashboard.service;

import com.example.ems.attendance.entity.AttendanceStatus;
import com.example.ems.attendance.repository.AttendanceRepository;
import com.example.ems.dashboard.dto.DashboardSummaryResponse;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.repository.EmployeeRepository;
import com.example.ems.leave.entity.LeaveStatus;
import com.example.ems.leave.repository.LeaveRequestRepository;
import com.example.ems.payroll.entity.PayrollStatus;
import com.example.ems.payroll.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;

    public DashboardSummaryResponse getSummary() {
        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long inactiveEmployees = employeeRepository.countByStatus(EmployeeStatus.INACTIVE);
        long onLeaveEmployees = employeeRepository.countByStatus(EmployeeStatus.ON_LEAVE);

        Map<String, Long> employeesByDepartment = new LinkedHashMap<>();
        for (Object[] row : employeeRepository.countGroupedByDepartment()) {
            employeesByDepartment.put((String) row[0], (Long) row[1]);
        }

        // There's no ABSENT attendance record ever written anywhere in the system - a missing
        // check-in just means no row exists for that day, not a row with status=ABSENT. So I
        // can't count absences directly; instead I infer it as "active employees minus however
        // many actually checked in today". The Math.max(0, ...) guards against a negative
        // number if somehow more records exist than active employees (e.g. someone checked in
        // then got marked inactive same day).
        LocalDate today = LocalDate.now();
        long present = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.PRESENT);
        long late = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.LATE);
        long halfDay = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.HALF_DAY);
        long totalRecorded = attendanceRepository.countByDate(today);
        long absent = Math.max(0, activeEmployees - totalRecorded);

        DashboardSummaryResponse.AttendanceSummary attendanceSummary =
                new DashboardSummaryResponse.AttendanceSummary(present, late, halfDay, absent, totalRecorded);

        long pendingLeaveRequests = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);

        DashboardSummaryResponse.PayrollTotals payrollTotals = new DashboardSummaryResponse.PayrollTotals(
                payrollRepository.sumNetSalaryByStatus(PayrollStatus.PENDING),
                payrollRepository.sumNetSalaryByStatus(PayrollStatus.PROCESSED),
                payrollRepository.sumNetSalaryByStatus(PayrollStatus.PAID));

        return new DashboardSummaryResponse(totalEmployees, activeEmployees, inactiveEmployees,
                employeesByDepartment, onLeaveEmployees, attendanceSummary, pendingLeaveRequests, payrollTotals);
    }
}
