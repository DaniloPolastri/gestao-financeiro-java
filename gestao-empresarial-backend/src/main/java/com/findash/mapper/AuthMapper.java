package com.findash.mapper;

import com.findash.dto.AuthResponseDTO;
import com.findash.dto.UserResponseDTO;
import com.findash.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    UserResponseDTO toUserResponse(User user);

    default AuthResponseDTO toAuthResponse(String accessToken, String refreshToken, User user) {
        return new AuthResponseDTO(accessToken, refreshToken, toUserResponse(user));
    }
}
