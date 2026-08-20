package com.example.ems.dashboard.controller;

import com.example.ems.common.response.ApiResponse;
import com.example.ems.dashboard.dto.DashboardSummaryResponse;
import com.example.ems.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Administrative statistics and reporting")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Get organization-wide dashboard statistics",
            description = "Requires ADMIN or HR role. Returns employee counts, attendance, leave, and payroll totals.")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSummary()));
    }
}
