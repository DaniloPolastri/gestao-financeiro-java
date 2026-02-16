package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.MemberStatus;
import com.findash.entity.RefreshToken;
import com.findash.entity.Role;
import com.findash.entity.User;
import com.findash.entity.UserRole;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.AuthMapper;
import com.findash.repository.CompanyMemberRepository;
import com.findash.repository.RefreshTokenRepository;
import com.findash.repository.UserRepository;
import com.findash.repository.UserRoleRepository;
import com.findash.security.JwtTokenProvider;
import com.findash.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthMapper mapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final long refreshTokenExpirationMs;

    public AuthServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           CompanyMemberRepository companyMemberRepository,
                           JwtTokenProvider tokenProvider,
                           AuthMapper mapper,
                           @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.tokenProvider = tokenProvider;
        this.mapper = mapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email ja cadastrado: " + request.email());
        }

        User user = new User(request.name(), request.email(),
                              passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        // Resolve pending invites
        var pendingInvites = companyMemberRepository
                .findByInvitedEmailAndStatus(request.email(), MemberStatus.INVITED);
        for (var invite : pendingInvites) {
            invite.setUserId(user.getId());
            invite.setStatus(MemberStatus.ACTIVE);
            invite.setJoinedAt(Instant.now());
            companyMemberRepository.save(invite);

            UserRole role = new UserRole();
            role.setUserId(user.getId());
            role.setCompanyId(invite.getCompanyId());
            role.setRole(Role.EDITOR); // default role for invited users
            userRoleRepository.save(role);
        }

        return createAuthResponse(user);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Senha invalida");
        }

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDTO refresh(RefreshRequestDTO request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", request.refreshToken()));

        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token invalido ou expirado");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", refreshToken.getUserId()));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(RefreshRequestDTO request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponseDTO createAuthResponse(User user) {
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().name())
                .toList();

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshTokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshTokenRepository.save(refreshToken);

        return mapper.toAuthResponse(accessToken, refreshTokenStr, user);
    }
}
