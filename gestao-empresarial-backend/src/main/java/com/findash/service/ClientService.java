package com.findash.service;

import com.findash.dto.client.CreateClientRequestDTO;
import com.findash.dto.client.ClientResponseDTO;
import com.findash.dto.client.UpdateClientRequestDTO;
import java.util.List;
import java.util.UUID;

public interface ClientService {
    ClientResponseDTO create(UUID companyId, CreateClientRequestDTO request);
    List<ClientResponseDTO> list(UUID companyId);
    ClientResponseDTO getById(UUID companyId, UUID clientId);
    ClientResponseDTO update(UUID companyId, UUID clientId, UpdateClientRequestDTO request);
    void delete(UUID companyId, UUID clientId);
}
