package com.example.ems.department;

import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.common.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DepartmentIntegrationTest extends IntegrationTestBase {

    @Test
    void create_succeeds_forAdminRole() throws Exception {
        User admin = createUser("dept-admin@example.com", RoleName.ADMIN);
        String token = loginAndGetToken(admin.getEmail());

        String body = """
                {"name":"Marketing-%s","description":"Handles marketing"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(org.hamcrest.Matchers.startsWith("Marketing-")));
    }

    @Test
    void create_isForbidden_forManagerRole() throws Exception {
        User manager = createUser("dept-mgr@example.com", RoleName.MANAGER);
        String token = loginAndGetToken(manager.getEmail());

        String body = """
                {"name":"Ops-%s"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_returnsConflict_whenNameAlreadyExists() throws Exception {
        User admin = createUser("dept-admin2@example.com", RoleName.ADMIN);
        String token = loginAndGetToken(admin.getEmail());
        String name = "Finance-" + UUID.randomUUID();

        String body = "{\"name\":\"" + name + "\"}";

        mockMvc.perform(post("/api/v1/departments").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/departments").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void findById_returnsNotFound_whenDepartmentDoesNotExist() throws Exception {
        User admin = createUser("dept-admin3@example.com", RoleName.ADMIN);
        String token = loginAndGetToken(admin.getEmail());

        mockMvc.perform(get("/api/v1/departments/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
