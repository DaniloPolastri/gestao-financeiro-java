package com.findash.auth.domain.service;

import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.out.PasswordEncoderPort;
import com.findash.auth.domain.port.out.UserRepositoryPort;
import com.findash.shared.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock private UserRepositoryPort userRepo;
    @Mock private PasswordEncoderPort passwordEncoder;

    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationService(userRepo, passwordEncoder);
    }

    @Test
    void register_withNewEmail_createsUser() {
        when(userRepo.existsByEmail("new@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = service.register("Test User", "new@email.com", "password");

        assertNotNull(result);
        assertEquals("Test User", result.getName());
        assertEquals("new@email.com", result.getEmail());
        assertEquals("encoded", result.getPassword());
        assertTrue(result.isActive());
    }

    @Test
    void register_withExistingEmail_throwsDuplicateException() {
        when(userRepo.existsByEmail("existing@email.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> service.register("Test", "existing@email.com", "password"));
    }
}
