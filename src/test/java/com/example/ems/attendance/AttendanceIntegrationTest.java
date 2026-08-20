package com.example.ems.attendance;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.common.IntegrationTestBase;
import com.example.ems.employee.entity.EmployeeStatus;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttendanceIntegrationTest extends IntegrationTestBase {

    @Test
    void checkIn_thenCheckOut_succeeds() throws Exception {
        User user = createUser("att-user@example.com", RoleName.EMPLOYEE);
        createEmployee(user, null, EmployeeStatus.ACTIVE, "EMP-500");
        String token = loginAndGetToken(user.getEmail());

        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.checkIn").exists());

        mockMvc.perform(post("/api/v1/attendance/check-out").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkOut").exists());
    }

    @Test
    void checkIn_isRejected_whenAlreadyCheckedInToday() throws Exception {
        User user = createUser("att-user2@example.com", RoleName.EMPLOYEE);
        createEmployee(user, null, EmployeeStatus.ACTIVE, "EMP-501");
        String token = loginAndGetToken(user.getEmail());

        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_OPERATION"));
    }

    @Test
    void checkOut_isRejected_whenNoCheckInExists() throws Exception {
        User user = createUser("att-user3@example.com", RoleName.EMPLOYEE);
        createEmployee(user, null, EmployeeStatus.ACTIVE, "EMP-502");
        String token = loginAndGetToken(user.getEmail());

        mockMvc.perform(post("/api/v1/attendance/check-out").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_OPERATION"));
    }

    @Test
    void checkIn_isRejected_forTerminatedEmployee() throws Exception {
        User user = createUser("att-user4@example.com", RoleName.EMPLOYEE);
        createEmployee(user, null, EmployeeStatus.TERMINATED, "EMP-503");
        String token = loginAndGetToken(user.getEmail());

        mockMvc.perform(post("/api/v1/attendance/check-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("Terminated")));
    }
}
