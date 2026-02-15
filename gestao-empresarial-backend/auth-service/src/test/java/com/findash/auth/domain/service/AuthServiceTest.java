package com.findash.auth.domain.service;

import com.findash.auth.domain.model.RefreshToken;
import com.findash.auth.domain.model.Role;
import com.findash.auth.domain.model.User;
import com.findash.auth.domain.model.UserRole;
import com.findash.auth.domain.port.in.AuthenticateUseCase.AuthResult;
import com.findash.auth.domain.port.out.*;
import com.findash.shared.exception.ResourceNotFoundException;
import com.findash.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepositoryPort userRepo;
    @Mock private UserRoleRepositoryPort userRoleRepo;
    @Mock private RefreshTokenRepositoryPort refreshTokenRepo;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepo, userRoleRepo, refreshTokenRepo,
                                       passwordEncoder, tokenProvider, 2592000000L);
    }

    @Test
    void authenticate_withValidCredentials_returnsAuthResult() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Test", "test@email.com", "encoded", true,
                             Instant.now(), Instant.now());
        UserRole role = new UserRole(UUID.randomUUID(), userId, UUID.randomUUID(),
                                     Role.ADMIN, Instant.now());

        when(userRepo.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(userRoleRepo.findByUserId(userId)).thenReturn(List.of(role));
        when(tokenProvider.generateAccessToken(eq(userId), eq("test@email.com"), anyList()))
                .thenReturn("jwt-token");
        when(refreshTokenRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResult result = authService.authenticate("test@email.com", "password");

        assertNotNull(result);
        assertEquals("jwt-token", result.accessToken());
        assertNotNull(result.refreshToken());
        assertEquals(userId, result.userId());
    }

    @Test
    void authenticate_withInvalidEmail_throwsException() {
        when(userRepo.findByEmail("bad@email.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> authService.authenticate("bad@email.com", "password"));
    }

    @Test
    void authenticate_withWrongPassword_throwsException() {
        User user = new User(UUID.randomUUID(), "Test", "test@email.com", "encoded",
                             true, Instant.now(), Instant.now());
        when(userRepo.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.authenticate("test@email.com", "wrong"));
    }
}
