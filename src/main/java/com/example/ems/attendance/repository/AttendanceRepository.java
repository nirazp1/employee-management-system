package com.example.ems.attendance.repository;

import com.example.ems.attendance.entity.Attendance;
import com.example.ems.attendance.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByEmployee_IdAndDate(UUID employeeId, LocalDate date);

    Page<Attendance> findByEmployee_Id(UUID employeeId, Pageable pageable);

    Page<Attendance> findByEmployee_Department_Id(UUID departmentId, Pageable pageable);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    long countByDate(LocalDate date);
}
