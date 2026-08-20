package com.example.ems.auth;

import com.example.ems.common.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void register_thenLogin_thenFetchProfile_succeeds() throws Exception {
        String registerBody = """
                {"email":"newuser@example.com","password":"Password123!","firstName":"New","lastName":"User"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles[0]").value("EMPLOYEE"));

        String loginBody = """
                {"email":"newuser@example.com","password":"Password123!"}
                """;

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"));
    }

    @Test
    void register_returnsConflict_whenEmailAlreadyRegistered() throws Exception {
        String body = """
                {"email":"dup@example.com","password":"Password123!","firstName":"A","lastName":"B"}
                """;

        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void login_returnsUnauthorized_whenPasswordIsWrong() throws Exception {
        String registerBody = """
                {"email":"wrongpass@example.com","password":"Password123!","firstName":"A","lastName":"B"}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {"email":"wrongpass@example.com","password":"WrongPassword!"}
                """;
        mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void me_returnsUnauthorized_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void register_returnsValidationError_whenPasswordTooShort() throws Exception {
        String body = """
                {"email":"short@example.com","password":"123","firstName":"A","lastName":"B"}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
