package com.example.ems.attendance.controller;

import com.example.ems.attendance.dto.AttendanceResponse;
import com.example.ems.attendance.service.AttendanceService;
import com.example.ems.common.response.ApiResponse;
import com.example.ems.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance", description = "Employee attendance check-in/check-out and history")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/api/v1/attendance/check-in")
    @Operation(summary = "Check in for today", description = "Employees check in for their own attendance; blocked for terminated/inactive employees or duplicate check-ins")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn() {
        AttendanceResponse response = attendanceService.checkIn();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Checked in successfully"));
    }

    @PostMapping("/api/v1/attendance/check-out")
    @Operation(summary = "Check out for today", description = "Requires an existing check-in for today; blocked for duplicate check-outs")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut() {
        AttendanceResponse response = attendanceService.checkOut();
        return ResponseEntity.ok(ApiResponse.ok(response, "Checked out successfully"));
    }

    @GetMapping("/api/v1/attendance")
    @Operation(summary = "List attendance records",
            description = "ADMIN/HR see all records, MANAGER is scoped to their department")
    public ResponseEntity<PagedResponse<AttendanceResponse>> findAll(Pageable pageable) {
        Page<AttendanceResponse> page = attendanceService.findAll(pageable);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/api/v1/attendance/{id}")
    @Operation(summary = "Get an attendance record by id")
    public ResponseEntity<ApiResponse<AttendanceResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.findById(id)));
    }

    @GetMapping("/api/v1/employees/{employeeId}/attendance")
    @Operation(summary = "Get attendance history for a specific employee")
    public ResponseEntity<PagedResponse<AttendanceResponse>> findByEmployee(@PathVariable UUID employeeId,
                                                                              Pageable pageable) {
        Page<AttendanceResponse> page = attendanceService.findByEmployee(employeeId, pageable);
        return ResponseEntity.ok(PagedResponse.from(page));
    }
}
