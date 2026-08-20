package com.example.ems.common;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExceptionHandlingIntegrationTest extends IntegrationTestBase {

    @Test
    void notFoundException_returnsConsistentErrorFormat() throws Exception {
        User admin = createUser("exc-admin@example.com", RoleName.ADMIN);
        String token = loginAndGetToken(admin.getEmail());
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/employees/{id}", missingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/employees/" + missingId));
    }

    @Test
    void validationError_returnsFieldLevelMessage() throws Exception {
        User hr = createUser("exc-hr@example.com", RoleName.HR);
        String token = loginAndGetToken(hr.getEmail());

        String invalidBody = """
                {"employeeNumber":"","firstName":"","lastName":"Doe","email":"not-an-email",
                 "hireDate":"2024-01-15","jobTitle":"Dev","salary":-100}
                """;

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void malformedJson_returnsBadRequest() throws Exception {
        User hr = createUser("exc-hr2@example.com", RoleName.HR);
        String token = loginAndGetToken(hr.getEmail());

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }
}
