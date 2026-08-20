package com.example.ems.payroll.service;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.common.TestSecurityContext;
import com.example.ems.common.exception.ForbiddenException;
import com.example.ems.common.exception.InvalidOperationException;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.service.EmployeeService;
import com.example.ems.payroll.dto.PayrollCreateRequest;
import com.example.ems.payroll.dto.PayrollResponse;
import com.example.ems.payroll.entity.Payroll;
import com.example.ems.payroll.repository.PayrollRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private PayrollService payrollService;

    @AfterEach
    void tearDown() {
        TestSecurityContext.clear();
    }

    @Test
    void create_throwsInvalidOperationException_whenPayPeriodEndBeforeStart() {
        Employee employee = Employee.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe").build();
        when(employeeService.getEntity(employee.getId())).thenReturn(employee);

        PayrollCreateRequest request = new PayrollCreateRequest(employee.getId(),
                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 1),
                BigDecimal.valueOf(5000), null, null, null);

        assertThatThrownBy(() -> payrollService.create(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Pay period end date");
    }

    @Test
    void create_throwsInvalidOperationException_whenDeductionsExceedEarnings() {
        Employee employee = Employee.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe").build();
        when(employeeService.getEntity(employee.getId())).thenReturn(employee);

        PayrollCreateRequest request = new PayrollCreateRequest(employee.getId(),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
                BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(5000), null);

        assertThatThrownBy(() -> payrollService.create(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("negative net salary");
    }

    @Test
    void create_calculatesNetSalaryCorrectly() {
        Employee employee = Employee.builder().id(UUID.randomUUID()).firstName("Jane").lastName("Doe").build();
        when(employeeService.getEntity(employee.getId())).thenReturn(employee);
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(invocation -> {
            Payroll payroll = invocation.getArgument(0);
            payroll.setId(UUID.randomUUID());
            return payroll;
        });

        PayrollCreateRequest request = new PayrollCreateRequest(employee.getId(),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500), BigDecimal.valueOf(200), null);

        PayrollResponse response = payrollService.create(request);

        assertThat(response.netSalary()).isEqualByComparingTo(BigDecimal.valueOf(5300));
    }

    @Test
    void findAll_throwsForbiddenException_forEmployeeRole() {
        TestSecurityContext.authenticateAs(TestSecurityContext.userWithRoles("employee@example.com", RoleName.EMPLOYEE));

        assertThatThrownBy(() -> payrollService.findAll(PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findById_throwsForbiddenException_whenEmployeeViewsAnothersPayroll() {
        User currentUser = TestSecurityContext.userWithRoles("self@example.com", RoleName.EMPLOYEE);
        TestSecurityContext.authenticateAs(currentUser);

        Employee self = Employee.builder().id(UUID.randomUUID()).build();
        Employee other = Employee.builder().id(UUID.randomUUID()).build();
        Payroll payroll = Payroll.builder().id(UUID.randomUUID()).employee(other).build();

        when(payrollRepository.findById(payroll.getId())).thenReturn(Optional.of(payroll));
        when(employeeService.getCurrentEmployee()).thenReturn(self);

        assertThatThrownBy(() -> payrollService.findById(payroll.getId()))
                .isInstanceOf(ForbiddenException.class);
    }
}
