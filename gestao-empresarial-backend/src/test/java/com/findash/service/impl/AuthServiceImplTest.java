package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.CompanyMember;
import com.findash.entity.MemberStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private CompanyMemberRepository companyMemberRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthMapper mapper;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, userRoleRepository,
                refreshTokenRepository, companyMemberRepository, tokenProvider, mapper, 2592000000L);
    }

    @Test
    void login_withInvalidEmail_throwsException() {
        when(userRepository.findByEmail("bad@email.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> authService.login(new LoginRequestDTO("bad@email.com", "password")));
    }

    @Test
    void register_withExistingEmail_throwsDuplicateException() {
        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> authService.register(new RegisterRequestDTO("Test", "existing@email.com", "password")));
    }

    @Test
    void register_withNewEmail_createsUser() {
        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userRoleRepository.findByUserId(any())).thenReturn(List.of());
        when(tokenProvider.generateAccessToken(any(), anyString(), anyList())).thenReturn("jwt");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toAuthResponse(anyString(), anyString(), any()))
                .thenReturn(new AuthResponseDTO("jwt", "refresh",
                        new UserResponseDTO(UUID.randomUUID(), "Test", "new@email.com")));
        when(companyMemberRepository.findByInvitedEmailAndStatus("new@email.com", MemberStatus.INVITED))
                .thenReturn(List.of());

        AuthResponseDTO result = authService.register(
                new RegisterRequestDTO("Test", "new@email.com", "password"));

        assertNotNull(result);
        assertEquals("new@email.com", result.user().email());
        verify(userRepository).save(any());
    }

    @Test
    void register_withPendingInvites_resolvesInvites() {
        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userRoleRepository.findByUserId(any())).thenReturn(List.of());
        when(tokenProvider.generateAccessToken(any(), anyString(), anyList())).thenReturn("jwt");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toAuthResponse(anyString(), anyString(), any()))
                .thenReturn(new AuthResponseDTO("jwt", "refresh",
                        new UserResponseDTO(UUID.randomUUID(), "New", "new@email.com")));

        CompanyMember pendingInvite = new CompanyMember();
        pendingInvite.setCompanyId(UUID.randomUUID());
        pendingInvite.setInvitedEmail("new@email.com");
        pendingInvite.setStatus(MemberStatus.INVITED);
        when(companyMemberRepository.findByInvitedEmailAndStatus("new@email.com", MemberStatus.INVITED))
                .thenReturn(List.of(pendingInvite));
        when(companyMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.register(new RegisterRequestDTO("New", "new@email.com", "password"));

        verify(companyMemberRepository).save(argThat(m ->
                m.getStatus() == MemberStatus.ACTIVE && m.getUserId() != null));
        verify(userRoleRepository, atLeast(1)).save(any());
    }
}
