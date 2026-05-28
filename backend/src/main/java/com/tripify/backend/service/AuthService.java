package com.tripify.backend.service;

import com.tripify.backend.dto.AuthResponse;
import com.tripify.backend.dto.LoginRequest;
import com.tripify.backend.dto.RegisterRequest;
import com.tripify.backend.dto.UserResponse;
import com.tripify.backend.model.AppUser;
import com.tripify.backend.model.AuthToken;
import com.tripify.backend.repository.AppUserRepository;
import com.tripify.backend.repository.AuthTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final int TOKEN_DAYS_TO_LIVE = 7;

    private final AppUserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AppUserRepository userRepository,
            AuthTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        AppUser user = userRepository.save(new AppUser(
                email,
                request.name().trim(),
                passwordEncoder.encode(request.password())
        ));

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return createAuthResponse(user);
    }

    public Optional<UserResponse> findUserByToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(authToken -> !authToken.isExpired())
                .map(AuthToken::getUser)
                .map(this::toUserResponse);
    }

    @Transactional
    public void logout(String token) {
        tokenRepository.deleteByToken(token);
    }

    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }

    private AuthResponse createAuthResponse(AppUser user) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(TOKEN_DAYS_TO_LIVE, ChronoUnit.DAYS);
        tokenRepository.save(new AuthToken(token, user, expiresAt));
        return new AuthResponse(token, toUserResponse(user));
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
