package com.example.ems.leave;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.common.IntegrationTestBase;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.entity.EmployeeStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaveRequestIntegrationTest extends IntegrationTestBase {

    @Test
    void create_isRejected_whenEndDateBeforeStartDate() throws Exception {
        User user = createUser("leave-user@example.com", RoleName.EMPLOYEE);
        createEmployee(user, null, EmployeeStatus.ACTIVE, "EMP-600");
        String token = loginAndGetToken(user.getEmail());

        String body = """
                {"leaveType":"VACATION","startDate":"2026-05-10","endDate":"2026-05-05","reason":"Trip"}
                """;

        mockMvc.perform(post("/api/v1/leave-requests").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_OPERATION"));
    }

    @Test
    void create_isRejected_whenOverlappingExistingRequest() throws Exception {
        User user = createUser("leave-user2@example.com", RoleName.EMPLOYEE);
        createEmployee(user, null, EmployeeStatus.ACTIVE, "EMP-601");
        String token = loginAndGetToken(user.getEmail());

        String firstBody = """
                {"leaveType":"VACATION","startDate":"2026-06-01","endDate":"2026-06-10","reason":"Trip"}
                """;
        mockMvc.perform(post("/api/v1/leave-requests").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(firstBody))
                .andExpect(status().isCreated());

        String overlappingBody = """
                {"leaveType":"SICK","startDate":"2026-06-05","endDate":"2026-06-08","reason":"Flu"}
                """;
        mockMvc.perform(post("/api/v1/leave-requests").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(overlappingBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("overlaps")));
    }

    @Test
    void fullApprovalWorkflow_succeeds() throws Exception {
        User employeeUser = createUser("leave-user3@example.com", RoleName.EMPLOYEE);
        Employee employee = createEmployee(employeeUser, null, EmployeeStatus.ACTIVE, "EMP-602");
        String employeeToken = loginAndGetToken(employeeUser.getEmail());

        String body = """
                {"leaveType":"PERSONAL","startDate":"2026-07-01","endDate":"2026-07-03","reason":"Personal matters"}
                """;
        String createResponse = mockMvc.perform(post("/api/v1/leave-requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse).get("data");
        UUID leaveRequestId = UUID.fromString(created.get("id").asText());

        User hrUser = createUser("leave-hr@example.com", RoleName.HR);
        String hrToken = loginAndGetToken(hrUser.getEmail());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/leave-requests/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void employeeCannotApprove_ownLeaveRequest() throws Exception {
        User employeeUser = createUser("leave-user4@example.com", RoleName.EMPLOYEE);
        createEmployee(employeeUser, null, EmployeeStatus.ACTIVE, "EMP-603");
        String employeeToken = loginAndGetToken(employeeUser.getEmail());

        String body = """
                {"leaveType":"SICK","startDate":"2026-08-01","endDate":"2026-08-02","reason":"Flu"}
                """;
        String createResponse = mockMvc.perform(post("/api/v1/leave-requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse).get("data");
        UUID leaveRequestId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/leave-requests/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }
}
