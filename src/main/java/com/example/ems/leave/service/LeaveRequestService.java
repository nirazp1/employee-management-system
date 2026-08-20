package com.example.ems.leave.service;

import com.example.ems.auth.entity.User;
import com.example.ems.auth.repository.UserRepository;
import com.example.ems.auth.security.SecurityUtils;
import com.example.ems.common.exception.ForbiddenException;
import com.example.ems.common.exception.InvalidOperationException;
import com.example.ems.common.exception.ResourceNotFoundException;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.repository.EmployeeRepository;
import com.example.ems.employee.service.EmployeeService;
import com.example.ems.leave.dto.LeaveRequestCreateRequest;
import com.example.ems.leave.dto.LeaveRequestResponse;
import com.example.ems.leave.entity.LeaveRequest;
import com.example.ems.leave.entity.LeaveStatus;
import com.example.ems.leave.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveRequestService {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final UserRepository userRepository;

    @Transactional
    public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidOperationException("End date cannot be before start date");
        }

        Employee employee = employeeService.getCurrentEmployee();

        // Checking against PENDING and APPROVED requests only (see the repository query) -
        // a rejected or cancelled request shouldn't block someone from re-requesting the
        // same dates, so I deliberately don't overlap-check against those statuses.
        if (!leaveRequestRepository.findOverlapping(employee.getId(), request.startDate(), request.endDate()).isEmpty()) {
            throw new InvalidOperationException("This leave request overlaps with an existing pending or approved request");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.leaveType())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request created for employee {}: {} to {}", employee.getId(), request.startDate(), request.endDate());
        return toResponse(saved);
    }

    public Page<LeaveRequestResponse> findAll(Pageable pageable) {
        if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("HR")) {
            return leaveRequestRepository.findAll(pageable).map(this::toResponse);
        }
        if (SecurityUtils.hasRole("MANAGER")) {
            Employee manager = employeeService.getCurrentEmployee();
            if (manager.getDepartment() == null) {
                return Page.empty(pageable);
            }
            return leaveRequestRepository.findByEmployee_Department_Id(manager.getDepartment().getId(), pageable)
                    .map(this::toResponse);
        }
        // Unlike attendance, leave requests don't have a separate "my requests" endpoint,
        // so a plain employee hitting GET /leave-requests just gets their own list back
        // instead of a 403 - it's the only view they need this endpoint for anyway.
        Employee self = employeeService.getCurrentEmployee();
        return leaveRequestRepository.findByEmployee_Id(self.getId(), pageable).map(this::toResponse);
    }

    public LeaveRequestResponse findById(UUID id) {
        LeaveRequest leaveRequest = getEntity(id);
        assertCanView(leaveRequest.getEmployee());
        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveRequestResponse approve(UUID id) {
        LeaveRequest leaveRequest = getEntity(id);
        assertCanDecide(leaveRequest.getEmployee());

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidOperationException("Only pending leave requests can be approved");
        }

        User approver = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(approver);

        // Only flip the employee to ON_LEAVE if the leave is actually happening today -
        // approving a request for next month shouldn't make someone look on-leave right now.
        // (There's no scheduler to flip them back to ACTIVE when the leave ends yet - that'd
        // need a background job, which felt out of scope here.)
        LocalDate today = LocalDate.now();
        if (!today.isBefore(leaveRequest.getStartDate()) && !today.isAfter(leaveRequest.getEndDate())) {
            Employee employee = leaveRequest.getEmployee();
            employee.setStatus(EmployeeStatus.ON_LEAVE);
            employeeRepository.save(employee);
        }

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} approved by {}", id, approver.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse reject(UUID id) {
        LeaveRequest leaveRequest = getEntity(id);
        assertCanDecide(leaveRequest.getEmployee());

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidOperationException("Only pending leave requests can be rejected");
        }

        User approver = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(approver);

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} rejected by {}", id, approver.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse cancel(UUID id) {
        LeaveRequest leaveRequest = getEntity(id);

        boolean isPrivileged = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("HR");
        if (!isPrivileged) {
            Employee self = employeeService.getCurrentEmployee();
            if (!self.getId().equals(leaveRequest.getEmployee().getId())) {
                throw new ForbiddenException("You can only cancel your own leave requests");
            }
        }

        // Once something's APPROVED, the employee's status may already have been flipped to
        // ON_LEAVE and other people may be planning around it, so I don't let a self-service
        // cancel touch it - that needs HR/Admin to sort out manually, hence PENDING-only here.
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidOperationException("Only pending leave requests can be cancelled; approved requests cannot be modified");
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} cancelled", id);
        return toResponse(saved);
    }

    private LeaveRequest getEntity(UUID id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Leave request", id));
    }

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
        throw new ForbiddenException("You do not have permission to access this leave request");
    }

    // Deliberately no "is this my own request" branch here, unlike assertCanView - an
    // employee approving their own leave would defeat the whole point of an approval flow.
    private void assertCanDecide(Employee target) {
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
        throw new ForbiddenException("You do not have permission to approve or reject this leave request");
    }

    private LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        Employee employee = leaveRequest.getEmployee();
        User approver = leaveRequest.getApprovedBy();
        return new LeaveRequestResponse(
                leaveRequest.getId(),
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                leaveRequest.getLeaveType(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason(),
                leaveRequest.getStatus(),
                approver != null ? approver.getId() : null,
                approver != null ? approver.getFirstName() + " " + approver.getLastName() : null,
                leaveRequest.getCreatedAt(),
                leaveRequest.getUpdatedAt());
    }
}
