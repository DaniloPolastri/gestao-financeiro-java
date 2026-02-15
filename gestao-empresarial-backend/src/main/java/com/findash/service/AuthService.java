package com.findash.service;

import com.findash.dto.*;

public interface AuthService {
    AuthResponseDTO register(RegisterRequestDTO request);
    AuthResponseDTO login(LoginRequestDTO request);
    AuthResponseDTO refresh(RefreshRequestDTO request);
    void logout(RefreshRequestDTO request);
}
