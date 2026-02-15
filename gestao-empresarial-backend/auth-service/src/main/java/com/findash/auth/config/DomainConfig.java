package com.findash.auth.config;

import com.findash.auth.domain.port.out.*;
import com.findash.auth.domain.service.AuthService;
import com.findash.auth.domain.service.UserRegistrationService;
import com.findash.shared.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public AuthService authService(UserRepositoryPort userRepo,
                                    UserRoleRepositoryPort userRoleRepo,
                                    RefreshTokenRepositoryPort refreshTokenRepo,
                                    PasswordEncoderPort passwordEncoder,
                                    JwtTokenProvider tokenProvider,
                                    @Value("${app.jwt.refresh-token-expiration-ms}") long refreshExpMs) {
        return new AuthService(userRepo, userRoleRepo, refreshTokenRepo,
                               passwordEncoder, tokenProvider, refreshExpMs);
    }

    @Bean
    public UserRegistrationService userRegistrationService(UserRepositoryPort userRepo,
                                                            PasswordEncoderPort passwordEncoder) {
        return new UserRegistrationService(userRepo, passwordEncoder);
    }
}
