package com.myledger.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myledger.entity.Role;
import com.myledger.entity.User;
import com.myledger.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end auth + role checks against a real Postgres (Testcontainers).
 * Requires a running Docker daemon.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Skip the owner seed here; the test creates its own accounts.
        registry.add("myledger.bootstrap.owner-email", () -> "seed-owner@myledger.local");
        registry.add("myledger.bootstrap.owner-password", () -> "unused-seed-pw");
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper json;

    @BeforeEach
    void seedAccounts() {
        createIfAbsent("owner@test.local", "owner-pw", Role.ROLE_OWNER);
        createIfAbsent("contractor@test.local", "contractor-pw", Role.ROLE_CONTRACTOR);
    }

    private void createIfAbsent(String email, String pw, Role role) {
        if (!users.existsByEmailIgnoreCase(email)) {
            users.save(new User(email, passwordEncoder.encode(pw), role));
        }
    }

    private String login(String email, String pw) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginBody(email, pw))))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private record LoginBody(String email, String password) {
    }

    @Test
    void login_succeeds_and_me_returns_identity() throws Exception {
        String token = login("owner@test.local", "owner-pw");
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void bad_credentials_return_401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginBody("owner@test.local", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_without_token_returns_401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void contractor_is_forbidden_from_owner_endpoints() throws Exception {
        String token = login("contractor@test.local", "contractor-pw");
        // /api/expenses is an owner-only surface; a contractor must be rejected at the gate (403),
        // never reaching a handler.
        mvc.perform(get("/api/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
