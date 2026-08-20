package com.example.ems.payroll.controller;

import com.example.ems.common.response.ApiResponse;
import com.example.ems.common.response.PagedResponse;
import com.example.ems.payroll.dto.PayrollCreateRequest;
import com.example.ems.payroll.dto.PayrollResponse;
import com.example.ems.payroll.dto.PayrollUpdateRequest;
import com.example.ems.payroll.service.PayrollService;
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
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payroll", description = "Employee payroll management")
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/api/v1/payroll")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Create a payroll record", description = "Requires ADMIN or HR role; computes netSalary = baseSalary + bonuses - deductions")
    public ResponseEntity<ApiResponse<PayrollResponse>> create(@Valid @RequestBody PayrollCreateRequest request) {
        PayrollResponse response = payrollService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Payroll record created successfully"));
    }

    @GetMapping("/api/v1/payroll")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "List all payroll records", description = "Requires ADMIN or HR role")
    public ResponseEntity<PagedResponse<PayrollResponse>> findAll(Pageable pageable) {
        Page<PayrollResponse> page = payrollService.findAll(pageable);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/api/v1/payroll/{id}")
    @Operation(summary = "Get a payroll record by id", description = "ADMIN/HR unrestricted; employees may only view their own records")
    public ResponseEntity<ApiResponse<PayrollResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.findById(id)));
    }

    @PutMapping("/api/v1/payroll/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update a payroll record", description = "Requires ADMIN or HR role")
    public ResponseEntity<ApiResponse<PayrollResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody PayrollUpdateRequest request) {
        PayrollResponse response = payrollService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Payroll record updated successfully"));
    }

    @DeleteMapping("/api/v1/payroll/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Delete a payroll record", description = "Requires ADMIN or HR role")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        payrollService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Payroll record deleted successfully"));
    }

    @GetMapping("/api/v1/employees/{employeeId}/payroll")
    @Operation(summary = "Get payroll history for a specific employee", description = "ADMIN/HR unrestricted; employees may only view their own history")
    public ResponseEntity<PagedResponse<PayrollResponse>> findByEmployee(@PathVariable UUID employeeId,
                                                                           Pageable pageable) {
        Page<PayrollResponse> page = payrollService.findByEmployee(employeeId, pageable);
        return ResponseEntity.ok(PagedResponse.from(page));
    }
}
