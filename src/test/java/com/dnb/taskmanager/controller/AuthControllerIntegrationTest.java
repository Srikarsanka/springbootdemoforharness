package com.dnb.taskmanager.controller;

import com.dnb.taskmanager.dto.request.LoginRequest;
import com.dnb.taskmanager.dto.request.RegisterRequest;
import com.dnb.taskmanager.dto.response.AuthResponse;
import com.dnb.taskmanager.dto.response.UserResponse;
import com.dnb.taskmanager.model.enums.UserRole;
import com.dnb.taskmanager.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Test
    @DisplayName("POST /api/auth/register - Success")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com", "New", "User", "SecurePass123!", UserRole.DEVELOPER);

        UserResponse userResponse = new UserResponse(
                "user-1", "newuser@example.com", "New", "User", UserRole.DEVELOPER, Instant.now());
        AuthResponse authResponse = new AuthResponse("mock-jwt-token", 86400000L, userResponse);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.user.email").value("newuser@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation Failure on Invalid Email")
    void register_InvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "not-an-email", "New", "User", "SecurePass123!", UserRole.DEVELOPER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Success")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");

        UserResponse userResponse = new UserResponse(
                "user-1", "user@example.com", "First", "Last", UserRole.DEVELOPER, Instant.now());
        AuthResponse authResponse = new AuthResponse("login-jwt-token", 86400000L, userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-jwt-token"))
                .andExpect(jsonPath("$.user.id").value("user-1"));
    }
}

