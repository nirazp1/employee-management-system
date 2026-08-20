package com.example.ems.employee.controller;

import com.example.ems.common.response.ApiResponse;
import com.example.ems.common.response.PagedResponse;
import com.example.ems.employee.dto.EmployeeCreateRequest;
import com.example.ems.employee.dto.EmployeeResponse;
import com.example.ems.employee.dto.EmployeeStatusUpdateRequest;
import com.example.ems.employee.dto.EmployeeUpdateRequest;
import com.example.ems.employee.entity.EmployeeStatus;
import com.example.ems.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Employees", description = "Employee management with search, filtering, and pagination")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Create an employee", description = "Requires ADMIN or HR role")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse response = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Employee created successfully"));
    }

    @GetMapping
    @Operation(summary = "List employees",
            description = "Paginated, sortable, filterable employee list. ADMIN/HR see all employees, "
                    + "MANAGER is scoped to their own department, EMPLOYEE is forbidden from listing.")
    public ResponseEntity<PagedResponse<EmployeeResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String jobTitle) {
        Page<EmployeeResponse> page = employeeService.findAll(pageable, search, departmentId, status, jobTitle);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by id",
            description = "ADMIN/HR unrestricted, MANAGER within their department, EMPLOYEE only their own record")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update an employee", description = "Requires ADMIN or HR role")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable UUID id,
                                                                  @Valid @RequestBody EmployeeUpdateRequest request) {
        EmployeeResponse response = employeeService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Employee updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Delete an employee", description = "Requires ADMIN or HR role")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Employee deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update an employee's status", description = "Requires ADMIN or HR role")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateStatus(@PathVariable UUID id,
                                                                        @Valid @RequestBody EmployeeStatusUpdateRequest request) {
        EmployeeResponse response = employeeService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Employee status updated successfully"));
    }
}
