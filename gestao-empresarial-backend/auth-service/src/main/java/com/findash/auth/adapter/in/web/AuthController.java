package com.findash.auth.adapter.in.web;

import com.findash.auth.adapter.in.web.dto.request.*;
import com.findash.auth.adapter.in.web.dto.response.AuthResponse;
import com.findash.auth.adapter.in.web.dto.response.AuthResponse.UserResponse;
import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.in.AuthenticateUseCase;
import com.findash.auth.domain.port.in.AuthenticateUseCase.AuthResult;
import com.findash.auth.domain.port.in.RegisterUserUseCase;
import com.findash.shared.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUseCase;
    private final AuthenticateUseCase authUseCase;

    public AuthController(RegisterUserUseCase registerUseCase, AuthenticateUseCase authUseCase) {
        this.registerUseCase = registerUseCase;
        this.authUseCase = authUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUseCase.register(request.name(), request.email(), request.password());
        AuthResult result = authUseCase.authenticate(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authUseCase.authenticate(request.email(), request.password());
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResult result = authUseCase.refreshToken(request.refreshToken());
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authUseCase.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(new UserResponse(user.userId(), null, user.email()));
    }

    private AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(
            result.accessToken(),
            result.refreshToken(),
            new UserResponse(result.userId(), result.name(), result.email())
        );
    }
}
