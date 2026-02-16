package com.findash.mapper;

import com.findash.dto.client.ClientResponseDTO;
import com.findash.entity.Client;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    default ClientResponseDTO toResponse(Client client) {
        return new ClientResponseDTO(
            client.getId(),
            client.getName(),
            client.getDocument(),
            client.getEmail(),
            client.getPhone(),
            client.isActive(),
            client.getCreatedAt(),
            client.getUpdatedAt()
        );
    }
}
