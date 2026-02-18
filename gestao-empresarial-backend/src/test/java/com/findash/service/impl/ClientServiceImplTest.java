package com.findash.service.impl;

import com.findash.dto.client.CreateClientRequestDTO;
import com.findash.dto.client.ClientResponseDTO;
import com.findash.dto.client.UpdateClientRequestDTO;
import com.findash.entity.Client;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.ClientMapper;
import com.findash.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ClientMapper clientMapper;

    private ClientServiceImpl clientService;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        clientService = new ClientServiceImpl(clientRepository, clientMapper);
        companyId = UUID.randomUUID();
    }

    @Test
    void createClient_success() {
        var request = new CreateClientRequestDTO("Cliente A", "11222333000181", "a@test.com", "11999990000");
        var client = new Client(companyId, "Cliente A");
        client.setId(UUID.randomUUID());
        var response = new ClientResponseDTO(client.getId(), "Cliente A", "11222333000181", "a@test.com", "11999990000", true, null, null);

        when(clientRepository.existsByCompanyIdAndNameIgnoreCaseAndActiveTrue(companyId, "Cliente A")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientMapper.toResponse(any(Client.class))).thenReturn(response);

        ClientResponseDTO result = clientService.create(companyId, request);
        assertNotNull(result);
        assertEquals("Cliente A", result.name());
    }

    @Test
    void createClient_duplicateActiveName_throws() {
        var request = new CreateClientRequestDTO("Cliente A", null, null, null);
        when(clientRepository.existsByCompanyIdAndNameIgnoreCaseAndActiveTrue(companyId, "Cliente A")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> clientService.create(companyId, request));
    }

    @Test
    void createClient_sameNameAsSoftDeleted_succeeds() {
        var request = new CreateClientRequestDTO("Deletado", null, null, null);
        var client = new Client(companyId, "Deletado");
        client.setId(UUID.randomUUID());
        var response = new ClientResponseDTO(client.getId(), "Deletado", null, null, null, true, null, null);

        when(clientRepository.existsByCompanyIdAndNameIgnoreCaseAndActiveTrue(companyId, "Deletado")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientMapper.toResponse(any(Client.class))).thenReturn(response);

        assertDoesNotThrow(() -> clientService.create(companyId, request));
    }

    @Test
    void listClients_returnsAll() {
        var c1 = new Client(companyId, "A");
        when(clientRepository.findByCompanyIdAndActiveTrue(companyId)).thenReturn(List.of(c1));
        when(clientMapper.toResponse(c1)).thenReturn(new ClientResponseDTO(UUID.randomUUID(), "A", null, null, null, true, null, null));

        List<ClientResponseDTO> result = clientService.list(companyId);
        assertEquals(1, result.size());
    }

    @Test
    void getClient_notFound_throws() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findByIdAndCompanyId(clientId, companyId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> clientService.getById(companyId, clientId));
    }

    @Test
    void deleteClient_setsInactive() {
        UUID clientId = UUID.randomUUID();
        var client = new Client(companyId, "A");
        client.setId(clientId);
        when(clientRepository.findByIdAndCompanyId(clientId, companyId)).thenReturn(Optional.of(client));

        clientService.delete(companyId, clientId);
        assertFalse(client.isActive());
        verify(clientRepository).save(client);
    }
}
