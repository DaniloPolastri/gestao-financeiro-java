package com.findash.auth.domain.port.in;

import com.findash.auth.domain.model.User;

public interface RegisterUserUseCase {
    User register(String name, String email, String password);
}
