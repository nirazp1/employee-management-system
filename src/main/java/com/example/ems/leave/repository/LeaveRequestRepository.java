package com.example.ems.leave.repository;

import com.example.ems.leave.entity.LeaveRequest;
import com.example.ems.leave.entity.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Page<LeaveRequest> findByEmployee_Id(UUID employeeId, Pageable pageable);

    Page<LeaveRequest> findByEmployee_Department_Id(UUID departmentId, Pageable pageable);

    long countByStatus(LeaveStatus status);

    // Classic "do two date ranges overlap" check: two ranges overlap unless one ends before
    // the other starts, so I just negate that and got this two-condition form. Restricting
    // to PENDING/APPROVED on purpose - REJECTED/CANCELLED requests shouldn't block new ones.
    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.employee.id = :employeeId
              AND lr.status IN ('PENDING', 'APPROVED')
              AND lr.startDate <= :endDate
              AND lr.endDate >= :startDate
            """)
    List<LeaveRequest> findOverlapping(@Param("employeeId") UUID employeeId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
