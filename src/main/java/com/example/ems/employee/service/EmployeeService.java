package com.example.ems.employee.service;

import com.example.ems.auth.entity.User;
import com.example.ems.auth.repository.UserRepository;
import com.example.ems.auth.security.SecurityUtils;
import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.ForbiddenException;
import com.example.ems.common.exception.ResourceNotFoundException;
import com.example.ems.department.entity.Department;
import com.example.ems.department.repository.DepartmentRepository;
import com.example.ems.employee.dto.EmployeeCreateRequest;
import com.example.ems.employee.dto.EmployeeResponse;
import com.example.ems.employee.dto.EmployeeStatusUpdateRequest;
import com.example.ems.employee.dto.EmployeeUpdateRequest;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.repository.EmployeeRepository;
import com.example.ems.employee.repository.EmployeeSpecifications;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmployeeNumberIgnoreCase(request.employeeNumber())) {
            throw new DuplicateResourceException("An employee with number '" + request.employeeNumber() + "' already exists");
        }
        if (employeeRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An employee with email '" + request.email() + "' already exists");
        }

        Employee employee = Employee.builder()
                .employeeNumber(request.employeeNumber())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .dateOfBirth(request.dateOfBirth())
                .hireDate(request.hireDate())
                .jobTitle(request.jobTitle())
                .salary(request.salary())
                .status(EmployeeStatus.ACTIVE)
                .department(resolveDepartment(request.departmentId()))
                .user(resolveLinkedUser(request.userEmail()))
                .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created: {}", saved.getEmployeeNumber());
        return toResponse(saved);
    }

    public Page<EmployeeResponse> findAll(Pageable pageable, String search, UUID departmentId,
                                           EmployeeStatus status, String jobTitle) {
        UUID effectiveDepartmentId = departmentId;

        if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("HR")) {
            if (!SecurityUtils.hasRole("MANAGER")) {
                throw new ForbiddenException("You do not have permission to list employees");
            }
            // A manager can technically pass a departmentId query param for someone else's
            // department, but I just overwrite it with their own here rather than validating
            // and rejecting it - simpler than adding a second error path for the same outcome.
            Department managerDepartment = getCurrentEmployee().getDepartment();
            if (managerDepartment == null) {
                return Page.empty(pageable);
            }
            effectiveDepartmentId = managerDepartment.getId();
        }

        Specification<Employee> spec = EmployeeSpecifications.filterBy(search, effectiveDepartmentId, status, jobTitle);
        return employeeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public EmployeeResponse findById(UUID id) {
        Employee employee = getEntity(id);
        assertCanView(employee);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse update(UUID id, EmployeeUpdateRequest request) {
        Employee employee = getEntity(id);

        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("An employee with email '" + request.email() + "' already exists");
        }

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setHireDate(request.hireDate());
        employee.setJobTitle(request.jobTitle());
        employee.setSalary(request.salary());
        employee.setDepartment(resolveDepartment(request.departmentId()));

        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public EmployeeResponse updateStatus(UUID id, EmployeeStatusUpdateRequest request) {
        Employee employee = getEntity(id);
        employee.setStatus(request.status());
        Employee saved = employeeRepository.save(employee);
        log.info("Employee {} status changed to {}", saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Employee employee = getEntity(id);
        employeeRepository.delete(employee);
        log.info("Employee deleted: {}", id);
    }

    public Employee getEntity(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
    }

    public Employee getCurrentEmployee() {
        return employeeRepository.findByUser_EmailIgnoreCase(SecurityUtils.currentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No employee profile is linked to the current user"));
    }

    public void assertCanView(Employee target) {
        if (SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("HR")) {
            return;
        }
        if (SecurityUtils.hasRole("MANAGER")) {
            Employee manager = getCurrentEmployee();
            if (manager.getDepartment() != null && target.getDepartment() != null
                    && manager.getDepartment().getId().equals(target.getDepartment().getId())) {
                return;
            }
        }
        Employee self = getCurrentEmployee();
        if (self.getId().equals(target.getId())) {
            return;
        }
        throw new ForbiddenException("You do not have permission to access this employee's data");
    }

    private Department resolveDepartment(UUID departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", departmentId));
    }

    // There's no dedicated "link this login to that employee" endpoint - I decided the
    // simplest onboarding flow is: the person registers themselves first (gets a User row),
    // then HR creates their Employee record and passes that same email back in userEmail to
    // wire the two together. The existence check below stops one login account from
    // accidentally (or maliciously) getting attached to two different employee records.
    private User resolveLinkedUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return null;
        }
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> ResourceNotFoundException.of("User account", userEmail));
        employeeRepository.findByUser_Id(user.getId()).ifPresent(existing -> {
            throw new DuplicateResourceException("User account '" + userEmail + "' is already linked to another employee");
        });
        return user;
    }

    private EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse.DepartmentSummary departmentSummary = null;
        Department department = employee.getDepartment();
        if (department != null) {
            departmentSummary = new EmployeeResponse.DepartmentSummary(department.getId(), department.getName());
        }
        return new EmployeeResponse(employee.getId(), employee.getEmployeeNumber(), employee.getFirstName(),
                employee.getLastName(), employee.getEmail(), employee.getPhone(), employee.getDateOfBirth(),
                employee.getHireDate(), employee.getJobTitle(), employee.getSalary(), employee.getStatus(),
                departmentSummary, employee.getCreatedAt(), employee.getUpdatedAt());
    }
}
