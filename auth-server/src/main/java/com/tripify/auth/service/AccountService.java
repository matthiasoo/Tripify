package com.tripify.auth.service;

import com.tripify.auth.dto.ChangePasswordRequest;
import com.tripify.auth.dto.UpdateProfileRequest;
import com.tripify.auth.dto.UserResponse;
import com.tripify.auth.model.AppUser;
import com.tripify.auth.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse me(String email) {
        return toUserResponse(requireUser(email));
    }

    @Transactional
    public UserResponse updateProfile(String currentEmail, UpdateProfileRequest request) {
        AppUser user = requireUser(currentEmail);
        String email = normalizeEmail(request.email());

        userRepository.findByEmailIgnoreCase(email)
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Użytkownik o tym adresie email już istnieje.");
                });

        user.setName(request.name().trim());
        user.setEmail(email);

        return toUserResponse(user);
    }

    @Transactional
    public void changePassword(String currentEmail, ChangePasswordRequest request) {
        AppUser user = requireUser(currentEmail);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Obecne hasło jest nieprawidłowe.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private AppUser requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new SecurityException("Brak autoryzacji."));
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
