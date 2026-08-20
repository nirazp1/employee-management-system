package com.example.ems.employee.repository;

import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByEmployeeNumberIgnoreCase(String employeeNumber);

    boolean existsByEmployeeNumberIgnoreCaseAndIdNot(String employeeNumber, UUID id);

    Optional<Employee> findByUser_Id(UUID userId);

    Optional<Employee> findByUser_EmailIgnoreCase(String email);

    long countByDepartment_Id(UUID departmentId);

    long countByStatus(EmployeeStatus status);

    @Query("SELECT d.name, COUNT(e) FROM Employee e JOIN e.department d GROUP BY d.name")
    List<Object[]> countGroupedByDepartment();
}
