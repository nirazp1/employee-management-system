package com.example.ems.employee;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.common.IntegrationTestBase;
import com.example.ems.department.entity.Department;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EmployeeIntegrationTest extends IntegrationTestBase {

    @Test
    void create_succeeds_forHrRole() throws Exception {
        User hr = createUser("hr@example.com", RoleName.HR);
        String token = loginAndGetToken(hr.getEmail());

        String body = """
                {"employeeNumber":"EMP-100","firstName":"Alice","lastName":"Smith","email":"alice@example.com",
                 "hireDate":"2024-01-15","jobTitle":"Developer","salary":75000}
                """;

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP-100"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void create_isForbidden_forEmployeeRole() throws Exception {
        User employeeUser = createUser("plainemployee@example.com", RoleName.EMPLOYEE);
        String token = loginAndGetToken(employeeUser.getEmail());

        String body = """
                {"employeeNumber":"EMP-101","firstName":"Bob","lastName":"Jones","email":"bob@example.com",
                 "hireDate":"2024-01-15","jobTitle":"Developer","salary":75000}
                """;

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void create_returnsConflict_whenEmployeeNumberDuplicated() throws Exception {
        User hr = createUser("hr2@example.com", RoleName.HR);
        String token = loginAndGetToken(hr.getEmail());

        String body = """
                {"employeeNumber":"EMP-200","firstName":"Carol","lastName":"White","email":"carol@example.com",
                 "hireDate":"2024-01-15","jobTitle":"Developer","salary":75000}
                """;
        mockMvc.perform(post("/api/v1/employees").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated());

        String duplicateBody = """
                {"employeeNumber":"EMP-200","firstName":"Dan","lastName":"Black","email":"dan@example.com",
                 "hireDate":"2024-01-15","jobTitle":"Developer","salary":75000}
                """;
        mockMvc.perform(post("/api/v1/employees").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(duplicateBody))
                .andExpect(status().isConflict());
    }

    @Test
    void findById_isForbidden_whenEmployeeRequestsAnotherEmployeesRecord() throws Exception {
        User userA = createUser("usera@example.com", RoleName.EMPLOYEE);
        User userB = createUser("userb@example.com", RoleName.EMPLOYEE);
        createEmployee(userA, null, EmployeeStatus.ACTIVE, "EMP-300");
        Employee employeeB = createEmployee(userB, null, EmployeeStatus.ACTIVE, "EMP-301");

        String tokenA = loginAndGetToken(userA.getEmail());

        mockMvc.perform(get("/api/v1/employees/{id}", employeeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void findById_succeeds_whenEmployeeRequestsOwnRecord() throws Exception {
        User userA = createUser("usera2@example.com", RoleName.EMPLOYEE);
        Employee employeeA = createEmployee(userA, null, EmployeeStatus.ACTIVE, "EMP-302");
        String tokenA = loginAndGetToken(userA.getEmail());

        mockMvc.perform(get("/api/v1/employees/{id}", employeeA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(employeeA.getId().toString()));
    }

    @Test
    void findAll_scopesToDepartment_forManagerRole() throws Exception {
        Department engineering = createDepartment("Engineering-" + System.nanoTime());
        Department sales = createDepartment("Sales-" + System.nanoTime());

        User managerUser = createUser("mgr@example.com", RoleName.MANAGER);
        createEmployee(managerUser, engineering, EmployeeStatus.ACTIVE, "EMP-400");

        User otherUser = createUser("otherdept@example.com", RoleName.EMPLOYEE);
        createEmployee(otherUser, sales, EmployeeStatus.ACTIVE, "EMP-401");

        String managerToken = loginAndGetToken(managerUser.getEmail());

        mockMvc.perform(get("/api/v1/employees").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].employeeNumber", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("EMP-401"))));
    }
}
