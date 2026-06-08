package com.tripify.backend.service;

import com.tripify.backend.dto.*;
import com.tripify.backend.model.AppUser;
import com.tripify.backend.model.AuthToken;
import com.tripify.backend.repository.AppUserRepository;
import com.tripify.backend.repository.AuthTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AuthTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        AppUser savedUser = new AppUser("test@example.com", "Test User", "encodedPassword");
        ReflectionTestUtils.setField(savedUser, "id", 1L);
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("Test User", response.user().name());
        assertEquals("test@example.com", response.user().email());
        assertEquals(1L, response.user().id());
        verify(tokenRepository, times(1)).save(any(AuthToken.class));
    }

    @Test
    void register_ThrowsException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AppUser user = new AppUser("test@example.com", "Test User", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("Test User", response.user().name());
        verify(tokenRepository, times(1)).save(any(AuthToken.class));
    }

    @Test
    void login_ThrowsException_WhenUserNotFound() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_ThrowsException_WhenPasswordIncorrect() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        AppUser user = new AppUser("test@example.com", "Test User", "encodedPassword");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void findUserByToken_Success() {
        AppUser user = new AppUser("test@example.com", "Test User", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", 1L);
        AuthToken token = new AuthToken("token123", user, Instant.now().plus(1, ChronoUnit.DAYS));

        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(token));

        Optional<UserResponse> response = authService.findUserByToken("token123");

        assertTrue(response.isPresent());
        assertEquals("Test User", response.get().name());
    }

    @Test
    void findUserByToken_ExpiredToken() {
        AppUser user = new AppUser("test@example.com", "Test User", "encodedPassword");
        AuthToken token = new AuthToken("token123", user, Instant.now().minus(1, ChronoUnit.DAYS));

        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(token));

        Optional<UserResponse> response = authService.findUserByToken("token123");

        assertFalse(response.isPresent());
    }

    @Test
    void updateProfile_Success() {
        AppUser user = new AppUser("test@example.com", "Test User", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", 1L);
        AuthToken token = new AuthToken("token123", user, Instant.now().plus(1, ChronoUnit.DAYS));

        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(token));
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "new@example.com");
        UserResponse response = authService.updateProfile("token123", request);

        assertEquals("New Name", response.name());
        assertEquals("new@example.com", response.email());
        assertEquals("New Name", user.getName());
        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void changePassword_Success() {
        AppUser user = new AppUser("test@example.com", "Test User", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", 1L);
        AuthToken token = new AuthToken("token123", user, Instant.now().plus(1, ChronoUnit.DAYS));

        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("currentPass", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newEncodedPassword");

        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "newPass");
        authService.changePassword("token123", request);

        assertEquals("newEncodedPassword", user.getPasswordHash());
    }

    @Test
    void logout_Success() {
        authService.logout("token123");
        verify(tokenRepository, times(1)).deleteByToken("token123");
    }

    @Test
    void extractBearerToken() {
        assertEquals("tokenXYZ", authService.extractBearerToken("Bearer tokenXYZ"));
        assertNull(authService.extractBearerToken("tokenXYZ"));
        assertNull(authService.extractBearerToken(null));
    }
}
