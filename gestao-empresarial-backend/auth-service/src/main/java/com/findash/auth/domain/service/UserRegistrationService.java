package com.findash.auth.domain.service;

import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.in.RegisterUserUseCase;
import com.findash.auth.domain.port.out.PasswordEncoderPort;
import com.findash.auth.domain.port.out.UserRepositoryPort;
import com.findash.shared.exception.DuplicateResourceException;

import java.time.Instant;

public class UserRegistrationService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepo;
    private final PasswordEncoderPort passwordEncoder;

    public UserRegistrationService(UserRepositoryPort userRepo, PasswordEncoderPort passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String name, String email, String password) {
        if (userRepo.existsByEmail(email)) {
            throw new DuplicateResourceException("Email ja cadastrado: " + email);
        }

        User user = new User(null, name, email, passwordEncoder.encode(password),
                              true, Instant.now(), Instant.now());
        return userRepo.save(user);
    }
}
