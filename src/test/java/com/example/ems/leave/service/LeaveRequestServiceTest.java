package com.example.ems.leave.service;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.auth.repository.UserRepository;
import com.example.ems.common.TestSecurityContext;
import com.example.ems.common.exception.ForbiddenException;
import com.example.ems.common.exception.InvalidOperationException;
import com.example.ems.department.entity.Department;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.repository.EmployeeRepository;
import com.example.ems.employee.service.EmployeeService;
import com.example.ems.leave.dto.LeaveRequestCreateRequest;
import com.example.ems.leave.dto.LeaveRequestResponse;
import com.example.ems.leave.entity.LeaveRequest;
import com.example.ems.leave.entity.LeaveStatus;
import com.example.ems.leave.entity.LeaveType;
import com.example.ems.leave.repository.LeaveRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    @AfterEach
    void tearDown() {
        TestSecurityContext.clear();
    }

    @Test
    void create_throwsInvalidOperationException_whenEndDateBeforeStartDate() {
        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(LeaveType.VACATION,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 5), "Trip");

        assertThatThrownBy(() -> leaveRequestService.create(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("End date cannot be before start date");

        verifyNoInteractions(leaveRequestRepository);
    }

    @Test
    void create_throwsInvalidOperationException_whenRequestOverlapsExisting() {
        Employee employee = Employee.builder().id(UUID.randomUUID()).build();
        when(employeeService.getCurrentEmployee()).thenReturn(employee);

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(LeaveType.SICK,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 5), "Flu");

        when(leaveRequestRepository.findOverlapping(employee.getId(), request.startDate(), request.endDate()))
                .thenReturn(List.of(new LeaveRequest()));

        assertThatThrownBy(() -> leaveRequestService.create(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void create_savesPendingLeaveRequest_whenValidAndNonOverlapping() {
        Employee employee = Employee.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe").build();
        when(employeeService.getCurrentEmployee()).thenReturn(employee);

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(LeaveType.VACATION,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), "Trip");

        when(leaveRequestRepository.findOverlapping(any(), any(), any())).thenReturn(List.of());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest leaveRequest = invocation.getArgument(0);
            leaveRequest.setId(UUID.randomUUID());
            return leaveRequest;
        });

        LeaveRequestResponse response = leaveRequestService.create(request);

        assertThat(response.status()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    void approve_throwsInvalidOperationException_whenRequestIsNotPending() {
        User admin = TestSecurityContext.userWithRoles("admin@example.com", RoleName.ADMIN);
        TestSecurityContext.authenticateAs(admin);

        Employee employee = Employee.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe").build();
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .status(LeaveStatus.APPROVED)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();

        when(leaveRequestRepository.findById(leaveRequest.getId())).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestService.approve(leaveRequest.getId()))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void approve_setsEmployeeStatusToOnLeave_whenTodayFallsWithinRange() {
        User admin = TestSecurityContext.userWithRoles("admin@example.com", RoleName.ADMIN);
        TestSecurityContext.authenticateAs(admin);

        Employee employee = Employee.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe")
                .status(EmployeeStatus.ACTIVE).build();
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .status(LeaveStatus.PENDING)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        when(leaveRequestRepository.findById(leaveRequest.getId())).thenReturn(Optional.of(leaveRequest));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        leaveRequestService.approve(leaveRequest.getId());

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
        verify(employeeRepository).save(employee);
    }

    @Test
    void approve_throwsForbiddenException_whenManagerOutsideEmployeesDepartment() {
        User managerUser = TestSecurityContext.userWithRoles("manager@example.com", RoleName.MANAGER);
        TestSecurityContext.authenticateAs(managerUser);

        Department otherDepartment = Department.builder().id(UUID.randomUUID()).name("Sales").build();
        Employee target = Employee.builder().id(UUID.randomUUID()).department(otherDepartment).build();
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(UUID.randomUUID())
                .employee(target)
                .status(LeaveStatus.PENDING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();

        Department managerDepartment = Department.builder().id(UUID.randomUUID()).name("Engineering").build();
        Employee manager = Employee.builder().id(UUID.randomUUID()).department(managerDepartment).build();

        when(leaveRequestRepository.findById(leaveRequest.getId())).thenReturn(Optional.of(leaveRequest));
        when(employeeService.getCurrentEmployee()).thenReturn(manager);

        assertThatThrownBy(() -> leaveRequestService.approve(leaveRequest.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancel_throwsForbiddenException_whenNotOwnerAndNotPrivileged() {
        User otherEmployeeUser = TestSecurityContext.userWithRoles("other@example.com", RoleName.EMPLOYEE);
        TestSecurityContext.authenticateAs(otherEmployeeUser);

        Employee owner = Employee.builder().id(UUID.randomUUID()).build();
        Employee requester = Employee.builder().id(UUID.randomUUID()).build();
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(UUID.randomUUID())
                .employee(owner)
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(leaveRequest.getId())).thenReturn(Optional.of(leaveRequest));
        when(employeeService.getCurrentEmployee()).thenReturn(requester);

        assertThatThrownBy(() -> leaveRequestService.cancel(leaveRequest.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancel_throwsInvalidOperationException_whenRequestAlreadyApproved() {
        User admin = TestSecurityContext.userWithRoles("admin@example.com", RoleName.ADMIN);
        TestSecurityContext.authenticateAs(admin);

        Employee owner = Employee.builder().id(UUID.randomUUID()).build();
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(UUID.randomUUID())
                .employee(owner)
                .status(LeaveStatus.APPROVED)
                .build();

        when(leaveRequestRepository.findById(leaveRequest.getId())).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestService.cancel(leaveRequest.getId()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot be modified");
    }
}
