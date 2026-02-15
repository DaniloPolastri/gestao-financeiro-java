package com.findash.auth.domain.service;

import com.findash.auth.domain.model.RefreshToken;
import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.in.AuthenticateUseCase;
import com.findash.auth.domain.port.out.*;
import com.findash.shared.exception.ResourceNotFoundException;
import com.findash.shared.security.JwtTokenProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AuthService implements AuthenticateUseCase {

    private final UserRepositoryPort userRepo;
    private final UserRoleRepositoryPort userRoleRepo;
    private final RefreshTokenRepositoryPort refreshTokenRepo;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long refreshTokenExpirationMs;

    public AuthService(UserRepositoryPort userRepo, UserRoleRepositoryPort userRoleRepo,
                       RefreshTokenRepositoryPort refreshTokenRepo,
                       PasswordEncoderPort passwordEncoder, JwtTokenProvider tokenProvider,
                       long refreshTokenExpirationMs) {
        this.userRepo = userRepo;
        this.userRoleRepo = userRoleRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public AuthResult authenticate(String email, String password) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Senha invalida");
        }

        List<String> roles = userRoleRepo.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().name())
                .toList();

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshTokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                null, user.getId(), refreshTokenStr,
                Instant.now().plusMillis(refreshTokenExpirationMs),
                false, Instant.now()
        );
        refreshTokenRepo.save(refreshToken);

        return new AuthResult(accessToken, refreshTokenStr, user.getId(),
                              user.getName(), user.getEmail());
    }

    @Override
    public AuthResult refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(refreshTokenStr)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", refreshTokenStr));

        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token invalido ou expirado");
        }

        User user = userRepo.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", refreshToken.getUserId()));

        List<String> roles = userRoleRepo.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().name())
                .toList();

        // Revoke old, create new
        refreshToken.setRevoked(true);
        refreshTokenRepo.save(refreshToken);

        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String newRefreshTokenStr = UUID.randomUUID().toString();

        RefreshToken newRefreshToken = new RefreshToken(
                null, user.getId(), newRefreshTokenStr,
                Instant.now().plusMillis(refreshTokenExpirationMs),
                false, Instant.now()
        );
        refreshTokenRepo.save(newRefreshToken);

        return new AuthResult(newAccessToken, newRefreshTokenStr, user.getId(),
                              user.getName(), user.getEmail());
    }

    @Override
    public void logout(String refreshTokenStr) {
        refreshTokenRepo.findByToken(refreshTokenStr)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepo.save(token);
                });
    }
}
