package com.example.ems.department.controller;

import com.example.ems.common.response.ApiResponse;
import com.example.ems.common.response.PagedResponse;
import com.example.ems.department.dto.DepartmentCreateRequest;
import com.example.ems.department.dto.DepartmentResponse;
import com.example.ems.department.dto.DepartmentUpdateRequest;
import com.example.ems.department.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Departments", description = "Department management")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Create a department", description = "Requires ADMIN or HR role. Department name must be unique.")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentResponse response = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Department created successfully"));
    }

    @GetMapping
    @Operation(summary = "List departments", description = "Paginated list of departments, available to any authenticated user")
    public ResponseEntity<PagedResponse<DepartmentResponse>> findAll(Pageable pageable) {
        Page<DepartmentResponse> page = departmentService.findAll(pageable);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a department by id")
    public ResponseEntity<ApiResponse<DepartmentResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update a department", description = "Requires ADMIN or HR role")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(@PathVariable UUID id,
                                                                    @Valid @RequestBody DepartmentUpdateRequest request) {
        DepartmentResponse response = departmentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Department updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Delete a department", description = "Requires ADMIN or HR role")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Department deleted successfully"));
    }
}
