package com.tripify.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripify.backend.dto.*;
import com.tripify.backend.model.AppUser;
import com.tripify.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("Test Name", "register@example.com", "password123");
        UserResponse userResponse = new UserResponse(1L, "Test Name", "register@example.com");
        AuthResponse authResponse = new AuthResponse("mock-token", userResponse);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-token"))
                .andExpect(jsonPath("$.user.name").value("Test Name"))
                .andExpect(jsonPath("$.user.email").value("register@example.com"));
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("login@example.com", "password123");
        UserResponse userResponse = new UserResponse(1L, "Test Name", "login@example.com");
        AuthResponse authResponse = new AuthResponse("mock-token", userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token"))
                .andExpect(jsonPath("$.user.name").value("Test Name"));
    }

    @Test
    void me_Success() throws Exception {
        String authHeader = "Bearer token123";
        UserResponse userResponse = new UserResponse(1L, "Test Name", "login@example.com");

        when(authService.extractBearerToken(authHeader)).thenReturn("token123");
        when(authService.findUserByToken("token123")).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Name"))
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    void me_Unauthorized_WhenTokenInvalid() throws Exception {
        String authHeader = "Bearer token123";

        when(authService.extractBearerToken(authHeader)).thenReturn("token123");
        when(authService.findUserByToken("token123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", authHeader))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_Success() throws Exception {
        String authHeader = "Bearer token123";
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "updated@example.com");
        UserResponse userResponse = new UserResponse(1L, "Updated Name", "updated@example.com");

        when(authService.extractBearerToken(authHeader)).thenReturn("token123");
        when(authService.updateProfile(eq("token123"), any(UpdateProfileRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/auth/profile")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void changePassword_Success() throws Exception {
        String authHeader = "Bearer token123";
        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "newPassword123");

        when(authService.extractBearerToken(authHeader)).thenReturn("token123");
        doNothing().when(authService).changePassword(eq("token123"), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_Success() throws Exception {
        String authHeader = "Bearer token123";

        when(authService.extractBearerToken(authHeader)).thenReturn("token123");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        verify(authService, times(1)).logout("token123");
    }
}
