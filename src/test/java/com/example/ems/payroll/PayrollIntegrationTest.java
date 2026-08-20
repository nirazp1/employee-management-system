package com.example.ems.payroll;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.common.IntegrationTestBase;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayrollIntegrationTest extends IntegrationTestBase {

    @Test
    void create_calculatesNetSalary_andEmployeeCanViewOwnRecord() throws Exception {
        User employeeUser = createUser("payroll-emp@example.com", RoleName.EMPLOYEE);
        Employee employee = createEmployee(employeeUser, null, EmployeeStatus.ACTIVE, "EMP-700");

        User hrUser = createUser("payroll-hr@example.com", RoleName.HR);
        String hrToken = loginAndGetToken(hrUser.getEmail());

        String body = """
                {"employeeId":"%s","payPeriodStart":"2026-01-01","payPeriodEnd":"2026-01-31",
                 "baseSalary":5000,"bonuses":300,"deductions":100}
                """.formatted(employee.getId());

        mockMvc.perform(post("/api/v1/payroll").header("Authorization", "Bearer " + hrToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.netSalary").value(5200.0));

        String employeeToken = loginAndGetToken(employeeUser.getEmail());
        mockMvc.perform(get("/api/v1/employees/{id}/payroll", employee.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].netSalary").value(5200.0));
    }

    @Test
    void create_isForbidden_forEmployeeRole() throws Exception {
        User employeeUser = createUser("payroll-emp2@example.com", RoleName.EMPLOYEE);
        Employee employee = createEmployee(employeeUser, null, EmployeeStatus.ACTIVE, "EMP-701");
        String token = loginAndGetToken(employeeUser.getEmail());

        String body = """
                {"employeeId":"%s","payPeriodStart":"2026-01-01","payPeriodEnd":"2026-01-31","baseSalary":5000}
                """.formatted(employee.getId());

        mockMvc.perform(post("/api/v1/payroll").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_isRejected_whenDeductionsCauseNegativeNetSalary() throws Exception {
        User employeeUser = createUser("payroll-emp3@example.com", RoleName.EMPLOYEE);
        Employee employee = createEmployee(employeeUser, null, EmployeeStatus.ACTIVE, "EMP-702");

        User hrUser = createUser("payroll-hr2@example.com", RoleName.HR);
        String hrToken = loginAndGetToken(hrUser.getEmail());

        String body = """
                {"employeeId":"%s","payPeriodStart":"2026-01-01","payPeriodEnd":"2026-01-31",
                 "baseSalary":1000,"deductions":5000}
                """.formatted(employee.getId());

        mockMvc.perform(post("/api/v1/payroll").header("Authorization", "Bearer " + hrToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_OPERATION"));
    }
}
