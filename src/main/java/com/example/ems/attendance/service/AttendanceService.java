package com.example.ems.attendance.service;

import com.example.ems.attendance.dto.AttendanceResponse;
import com.example.ems.attendance.entity.Attendance;
import com.example.ems.attendance.entity.AttendanceStatus;
import com.example.ems.attendance.repository.AttendanceRepository;
import com.example.ems.auth.security.SecurityUtils;
import com.example.ems.common.exception.ForbiddenException;
import com.example.ems.common.exception.InvalidOperationException;
import com.example.ems.common.exception.ResourceNotFoundException;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);
    private static final LocalTime LATE_THRESHOLD = LocalTime.of(9, 15);
    private static final Duration HALF_DAY_THRESHOLD = Duration.ofHours(4);

    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;

    @Transactional
    public AttendanceResponse checkIn() {
        // Pulling the employee from the security context (not a path param) so nobody can
        // check someone else in by guessing an employeeId in the request.
        Employee employee = employeeService.getCurrentEmployee();

        if (employee.getStatus() == EmployeeStatus.TERMINATED || employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new InvalidOperationException("Terminated or inactive employees cannot check in");
        }

        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByEmployee_IdAndDate(employee.getId(), today).isPresent()) {
            throw new InvalidOperationException("Employee has already checked in today");
        }

        // There's no formal "shift start" concept anywhere in the system yet, so I picked
        // 9:15 as a stand-in cutoff for LATE. It's the one arbitrary business rule here -
        // swap it for a per-department/shift config if that ever becomes a real requirement.
        LocalDateTime now = LocalDateTime.now();
        AttendanceStatus status = now.toLocalTime().isAfter(LATE_THRESHOLD) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .date(today)
                .checkIn(now)
                .status(status)
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee {} checked in at {}", employee.getId(), now);
        return toResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkOut() {
        Employee employee = employeeService.getCurrentEmployee();
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployee_IdAndDate(employee.getId(), today)
                .orElseThrow(() -> new InvalidOperationException("Employee must check in before checking out"));

        if (attendance.getCheckOut() != null) {
            throw new InvalidOperationException("Employee has already checked out today");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOut(now);

        // Same idea as the LATE threshold: if someone was on-site for less than 4 hours,
        // I'm overwriting whatever status check-in gave them with HALF_DAY, since a short
        // day matters more for payroll/reporting than whether they were technically late.
        if (Duration.between(attendance.getCheckIn(), now).compareTo(HALF_DAY_THRESHOLD) < 0) {
            attendance.setStatus(AttendanceStatus.HALF_DAY);
        }

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee {} checked out at {}", employee.getId(), now);
        return toResponse(saved);
    }

    public Page<AttendanceResponse> findAll(Pageable pageable) {
        if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("HR")) {
            return attendanceRepository.findAll(pageable).map(this::toResponse);
        }
        if (SecurityUtils.hasRole("MANAGER")) {
            Employee manager = employeeService.getCurrentEmployee();
            // A manager with no department assigned has no "team" to look at yet, so I return
            // an empty page instead of throwing - it's not really an error state for them.
            if (manager.getDepartment() == null) {
                return Page.empty(pageable);
            }
            return attendanceRepository.findByEmployee_Department_Id(manager.getDepartment().getId(), pageable)
                    .map(this::toResponse);
        }
        // Plain employees don't get a browsable "all attendance" view - they're expected to
        // use GET /employees/{id}/attendance for their own history instead.
        throw new ForbiddenException("You do not have permission to view all attendance records");
    }

    public AttendanceResponse findById(UUID id) {
        Attendance attendance = getEntity(id);
        assertCanView(attendance.getEmployee());
        return toResponse(attendance);
    }

    public Page<AttendanceResponse> findByEmployee(UUID employeeId, Pageable pageable) {
        Employee employee = employeeService.getEntity(employeeId);
        assertCanView(employee);
        return attendanceRepository.findByEmployee_Id(employeeId, pageable).map(this::toResponse);
    }

    private Attendance getEntity(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance record", id));
    }

    // Same role hierarchy everywhere: ADMIN/HR see everything, MANAGER is boxed into their
    // own department, EMPLOYEE only ever sees themselves. Any single record lookup - by id
    // or via the per-employee endpoint - funnels through here.
    private void assertCanView(Employee target) {
        if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("HR")) {
            return;
        }
        if (SecurityUtils.hasRole("MANAGER")) {
            Employee manager = employeeService.getCurrentEmployee();
            if (manager.getDepartment() != null && target.getDepartment() != null
                    && manager.getDepartment().getId().equals(target.getDepartment().getId())) {
                return;
            }
        }
        Employee self = employeeService.getCurrentEmployee();
        if (self.getId().equals(target.getId())) {
            return;
        }
        throw new ForbiddenException("You do not have permission to access this attendance record");
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        Employee employee = attendance.getEmployee();
        return new AttendanceResponse(attendance.getId(), employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(), attendance.getDate(),
                attendance.getCheckIn(), attendance.getCheckOut(), attendance.getStatus(),
                attendance.getCreatedAt(), attendance.getUpdatedAt());
    }
}
