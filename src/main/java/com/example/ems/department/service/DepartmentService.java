package com.example.ems.department.service;

import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.ResourceNotFoundException;
import com.example.ems.department.dto.DepartmentCreateRequest;
import com.example.ems.department.dto.DepartmentResponse;
import com.example.ems.department.dto.DepartmentUpdateRequest;
import com.example.ems.department.entity.Department;
import com.example.ems.department.repository.DepartmentRepository;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A department named '" + request.name() + "' already exists");
        }

        Department department = Department.builder()
                .name(request.name())
                .description(request.description())
                .manager(resolveManager(request.managerId()))
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Department created: {}", saved.getName());
        return toResponse(saved);
    }

    public Page<DepartmentResponse> findAll(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(this::toResponse);
    }

    public DepartmentResponse findById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public DepartmentResponse update(UUID id, DepartmentUpdateRequest request) {
        Department department = getEntity(id);

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("A department named '" + request.name() + "' already exists");
        }

        department.setName(request.name());
        department.setDescription(request.description());
        department.setManager(resolveManager(request.managerId()));

        Department saved = departmentRepository.save(department);
        log.info("Department updated: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Department department = getEntity(id);
        departmentRepository.delete(department);
        log.info("Department deleted: {}", id);
    }

    public Department getEntity(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    // Intentionally not checking that this employee holds the MANAGER role - "who runs this
    // department" felt like it should just be "any employee HR/Admin points at", not something
    // gated on their login role. The role system controls what a manager can *do*, this is
    // just a label on the department.
    private Employee resolveManager(UUID managerId) {
        if (managerId == null) {
            return null;
        }
        return employeeRepository.findById(managerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee (manager)", managerId));
    }

    private DepartmentResponse toResponse(Department department) {
        DepartmentResponse.ManagerSummary managerSummary = null;
        Employee manager = department.getManager();
        if (manager != null) {
            managerSummary = new DepartmentResponse.ManagerSummary(
                    manager.getId(), manager.getFirstName() + " " + manager.getLastName(), manager.getJobTitle());
        }
        return new DepartmentResponse(department.getId(), department.getName(), department.getDescription(),
                managerSummary, department.getCreatedAt(), department.getUpdatedAt());
    }
}
