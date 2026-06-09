package com.tripify.auth.service;

import com.tripify.auth.dto.RegisterRequest;
import com.tripify.auth.model.AppUser;
import com.tripify.auth.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Użytkownik o tym adresie email już istnieje.");
        }

        return userRepository.save(new AppUser(
                email,
                request.name().trim(),
                passwordEncoder.encode(request.password())
        ));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
