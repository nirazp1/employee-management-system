package com.example.ems.employee.service;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.auth.repository.UserRepository;
import com.example.ems.common.TestSecurityContext;
import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.ForbiddenException;
import com.example.ems.department.entity.Department;
import com.example.ems.department.repository.DepartmentRepository;
import com.example.ems.employee.dto.EmployeeCreateRequest;
import com.example.ems.employee.dto.EmployeeResponse;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @AfterEach
    void tearDown() {
        TestSecurityContext.clear();
    }

    @Test
    void create_throwsDuplicateResourceException_whenEmployeeNumberAlreadyExists() {
        EmployeeCreateRequest request = new EmployeeCreateRequest("EMP-001", "Jane", "Doe", "jane@example.com",
                null, null, LocalDate.now(), "Engineer", BigDecimal.valueOf(50000), null, null);
        when(employeeRepository.existsByEmployeeNumberIgnoreCase("EMP-001")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void create_throwsDuplicateResourceException_whenEmailAlreadyExists() {
        EmployeeCreateRequest request = new EmployeeCreateRequest("EMP-002", "Jane", "Doe", "jane@example.com",
                null, null, LocalDate.now(), "Engineer", BigDecimal.valueOf(50000), null, null);
        when(employeeRepository.existsByEmployeeNumberIgnoreCase("EMP-002")).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void create_savesActiveEmployee_whenDataIsValid() {
        EmployeeCreateRequest request = new EmployeeCreateRequest("EMP-003", "Jane", "Doe", "jane@example.com",
                "555-1234", LocalDate.of(1990, 1, 1), LocalDate.now(), "Engineer", BigDecimal.valueOf(50000), null, null);
        when(employeeRepository.existsByEmployeeNumberIgnoreCase("EMP-003")).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(UUID.randomUUID());
            return employee;
        });

        EmployeeResponse response = employeeService.create(request);

        assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(response.employeeNumber()).isEqualTo("EMP-003");
    }

    @Test
    void findAll_throwsForbiddenException_forPlainEmployeeRole() {
        TestSecurityContext.authenticateAs(TestSecurityContext.userWithRoles("employee@example.com", RoleName.EMPLOYEE));
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> employeeService.findAll(pageable, null, null, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findById_throwsForbiddenException_whenEmployeeViewsAnotherEmployeesRecord() {
        User currentUser = TestSecurityContext.userWithRoles("self@example.com", RoleName.EMPLOYEE);
        TestSecurityContext.authenticateAs(currentUser);

        Employee self = Employee.builder().id(UUID.randomUUID()).build();
        Employee target = Employee.builder().id(UUID.randomUUID()).build();

        when(employeeRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(employeeRepository.findByUser_EmailIgnoreCase("self@example.com")).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> employeeService.findById(target.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findById_succeeds_whenEmployeeViewsOwnRecord() {
        User currentUser = TestSecurityContext.userWithRoles("self@example.com", RoleName.EMPLOYEE);
        TestSecurityContext.authenticateAs(currentUser);

        Employee self = Employee.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe")
                .status(EmployeeStatus.ACTIVE).build();

        when(employeeRepository.findById(self.getId())).thenReturn(Optional.of(self));
        when(employeeRepository.findByUser_EmailIgnoreCase("self@example.com")).thenReturn(Optional.of(self));

        EmployeeResponse response = employeeService.findById(self.getId());

        assertThat(response.id()).isEqualTo(self.getId());
    }

    @Test
    void findAll_scopesToManagersDepartment() {
        User currentUser = TestSecurityContext.userWithRoles("manager@example.com", RoleName.MANAGER);
        TestSecurityContext.authenticateAs(currentUser);

        Department department = Department.builder().id(UUID.randomUUID()).name("Engineering").build();
        Employee manager = Employee.builder().id(UUID.randomUUID()).department(department).build();

        when(employeeRepository.findByUser_EmailIgnoreCase("manager@example.com")).thenReturn(Optional.of(manager));
        when(employeeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        employeeService.findAll(PageRequest.of(0, 20), null, null, null, null);

        verify(employeeRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }
}
