package com.example.ems.leave.controller;

import com.example.ems.common.response.ApiResponse;
import com.example.ems.common.response.PagedResponse;
import com.example.ems.leave.dto.LeaveRequestCreateRequest;
import com.example.ems.leave.dto.LeaveRequestResponse;
import com.example.ems.leave.service.LeaveRequestService;
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
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave Requests", description = "Employee leave request submission and approval workflow")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @Operation(summary = "Submit a leave request", description = "Employees submit requests for themselves; dates must be valid and non-overlapping")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> create(@Valid @RequestBody LeaveRequestCreateRequest request) {
        LeaveRequestResponse response = leaveRequestService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Leave request submitted successfully"));
    }

    @GetMapping
    @Operation(summary = "List leave requests",
            description = "ADMIN/HR see all requests, MANAGER sees their team's requests, EMPLOYEE sees only their own")
    public ResponseEntity<PagedResponse<LeaveRequestResponse>> findAll(Pageable pageable) {
        Page<LeaveRequestResponse> page = leaveRequestService.findAll(pageable);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a leave request by id")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(leaveRequestService.findById(id)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Approve a pending leave request", description = "Requires ADMIN, HR, or the requester's MANAGER")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(leaveRequestService.approve(id), "Leave request approved"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Reject a pending leave request", description = "Requires ADMIN, HR, or the requester's MANAGER")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(leaveRequestService.reject(id), "Leave request rejected"));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending leave request", description = "Employees may cancel their own pending requests; ADMIN/HR may cancel any pending request")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(leaveRequestService.cancel(id), "Leave request cancelled"));
    }
}
