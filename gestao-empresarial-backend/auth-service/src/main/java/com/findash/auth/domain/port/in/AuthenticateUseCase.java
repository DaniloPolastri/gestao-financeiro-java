package com.findash.auth.domain.port.in;

import java.util.UUID;

public interface AuthenticateUseCase {
    record AuthResult(String accessToken, String refreshToken, UUID userId,
                      String name, String email) {}
    AuthResult authenticate(String email, String password);
    AuthResult refreshToken(String refreshToken);
    void logout(String refreshToken);
}
