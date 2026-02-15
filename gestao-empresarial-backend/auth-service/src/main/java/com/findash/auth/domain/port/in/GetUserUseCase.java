package com.findash.auth.domain.port.in;

import com.findash.auth.domain.model.User;
import java.util.UUID;

public interface GetUserUseCase {
    User getById(UUID id);
    User getByEmail(String email);
    User updateProfile(UUID id, String name);
}
