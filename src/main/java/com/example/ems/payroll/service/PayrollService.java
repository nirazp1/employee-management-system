package com.example.ems.payroll.service;

import com.example.ems.auth.security.SecurityUtils;
import com.example.ems.common.exception.ForbiddenException;
import com.example.ems.common.exception.InvalidOperationException;
import com.example.ems.common.exception.ResourceNotFoundException;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.service.EmployeeService;
import com.example.ems.payroll.dto.PayrollCreateRequest;
import com.example.ems.payroll.dto.PayrollResponse;
import com.example.ems.payroll.dto.PayrollUpdateRequest;
import com.example.ems.payroll.entity.Payroll;
import com.example.ems.payroll.entity.PayrollStatus;
import com.example.ems.payroll.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository payrollRepository;
    private final EmployeeService employeeService;

    @Transactional
    public PayrollResponse create(PayrollCreateRequest request) {
        Employee employee = employeeService.getEntity(request.employeeId());

        if (request.payPeriodEnd().isBefore(request.payPeriodStart())) {
            throw new InvalidOperationException("Pay period end date cannot be before the start date");
        }

        BigDecimal bonuses = request.bonuses() != null ? request.bonuses() : BigDecimal.ZERO;
        BigDecimal deductions = request.deductions() != null ? request.deductions() : BigDecimal.ZERO;
        BigDecimal netSalary = calculateNetSalary(request.baseSalary(), bonuses, deductions);

        Payroll payroll = Payroll.builder()
                .employee(employee)
                .payPeriodStart(request.payPeriodStart())
                .payPeriodEnd(request.payPeriodEnd())
                .baseSalary(request.baseSalary())
                .bonuses(bonuses)
                .deductions(deductions)
                .netSalary(netSalary)
                .paymentDate(request.paymentDate())
                .status(PayrollStatus.PENDING)
                .build();

        Payroll saved = payrollRepository.save(payroll);
        log.info("Payroll record created for employee {}: period {} - {}", employee.getId(),
                request.payPeriodStart(), request.payPeriodEnd());
        return toResponse(saved);
    }

    public Page<PayrollResponse> findAll(Pageable pageable) {
        assertPrivileged();
        return payrollRepository.findAll(pageable).map(this::toResponse);
    }

    public PayrollResponse findById(UUID id) {
        Payroll payroll = getEntity(id);
        assertCanView(payroll.getEmployee());
        return toResponse(payroll);
    }

    public Page<PayrollResponse> findByEmployee(UUID employeeId, Pageable pageable) {
        Employee employee = employeeService.getEntity(employeeId);
        assertCanView(employee);
        return payrollRepository.findByEmployee_Id(employeeId, pageable).map(this::toResponse);
    }

    @Transactional
    public PayrollResponse update(UUID id, PayrollUpdateRequest request) {
        assertPrivileged();
        Payroll payroll = getEntity(id);

        if (request.payPeriodEnd().isBefore(request.payPeriodStart())) {
            throw new InvalidOperationException("Pay period end date cannot be before the start date");
        }

        BigDecimal bonuses = request.bonuses() != null ? request.bonuses() : BigDecimal.ZERO;
        BigDecimal deductions = request.deductions() != null ? request.deductions() : BigDecimal.ZERO;
        BigDecimal netSalary = calculateNetSalary(request.baseSalary(), bonuses, deductions);

        payroll.setPayPeriodStart(request.payPeriodStart());
        payroll.setPayPeriodEnd(request.payPeriodEnd());
        payroll.setBaseSalary(request.baseSalary());
        payroll.setBonuses(bonuses);
        payroll.setDeductions(deductions);
        payroll.setNetSalary(netSalary);
        payroll.setPaymentDate(request.paymentDate());
        payroll.setStatus(request.status());

        Payroll saved = payrollRepository.save(payroll);
        log.info("Payroll record {} updated", id);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        assertPrivileged();
        Payroll payroll = getEntity(id);
        payrollRepository.delete(payroll);
        log.info("Payroll record {} deleted", id);
    }

    // Kept this as its own method instead of inlining the math in create()/update() because
    // both call it and I wanted the "no negative net salary" guardrail enforced in exactly
    // one place - deductions should never be able to push net pay negative, and duplicating
    // this check felt like an easy way to eventually miss it.
    private BigDecimal calculateNetSalary(BigDecimal baseSalary, BigDecimal bonuses, BigDecimal deductions) {
        BigDecimal netSalary = baseSalary.add(bonuses).subtract(deductions);
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Deductions cannot result in a negative net salary");
        }
        return netSalary;
    }

    private Payroll getEntity(UUID id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Payroll record", id));
    }

    private void assertPrivileged() {
        if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("HR")) {
            throw new ForbiddenException("Only ADMIN or HR can manage payroll");
        }
    }

    // No MANAGER carve-out here on purpose, unlike attendance/leave - salary data is sensitive
    // enough that "manages the department" shouldn't automatically mean "sees everyone's pay".
    // Only HR/ADMIN get the broad view; everyone else only ever sees their own record.
    private void assertCanView(Employee target) {
        if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("HR")) {
            return;
        }
        Employee self = employeeService.getCurrentEmployee();
        if (self.getId().equals(target.getId())) {
            return;
        }
        throw new ForbiddenException("You can only view your own payroll information");
    }

    private PayrollResponse toResponse(Payroll payroll) {
        Employee employee = payroll.getEmployee();
        return new PayrollResponse(payroll.getId(), employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(), payroll.getPayPeriodStart(),
                payroll.getPayPeriodEnd(), payroll.getBaseSalary(), payroll.getBonuses(), payroll.getDeductions(),
                payroll.getNetSalary(), payroll.getPaymentDate(), payroll.getStatus(), payroll.getCreatedAt(),
                payroll.getUpdatedAt());
    }
}
