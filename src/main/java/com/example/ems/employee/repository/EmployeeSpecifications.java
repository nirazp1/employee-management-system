package com.example.ems.employee.repository;

import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    // Built this as a Specification instead of a pile of derived-query methods (findByStatus,
    // findByDepartment, findByStatusAndDepartment, ...) because the search endpoint supports
    // any combination of these filters at once - a query-method approach would need one method
    // per combination. Each filter only gets added to the predicate list if it was actually
    // supplied, so an all-null call just returns everything.
    public static Specification<Employee> filterBy(String search, UUID departmentId, EmployeeStatus status,
                                                     String jobTitle) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                // One free-text box searching across name/email/employee number felt more
                // useful to whoever's using this than making them pick which field to search.
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("employeeNumber")), pattern)
                ));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (jobTitle != null && !jobTitle.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("jobTitle")), "%" + jobTitle.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
