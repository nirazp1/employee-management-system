package com.example.ems.payroll.repository;

import com.example.ems.payroll.entity.Payroll;
import com.example.ems.payroll.entity.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayrollRepository extends JpaRepository<Payroll, UUID> {

    Page<Payroll> findByEmployee_Id(UUID employeeId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.netSalary), 0) FROM Payroll p WHERE p.status = :status")
    BigDecimal sumNetSalaryByStatus(@Param("status") PayrollStatus status);
}
