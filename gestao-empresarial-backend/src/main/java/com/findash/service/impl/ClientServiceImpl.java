package com.findash.service.impl;

import com.findash.dto.client.CreateClientRequestDTO;
import com.findash.dto.client.ClientResponseDTO;
import com.findash.dto.client.UpdateClientRequestDTO;
import com.findash.entity.Client;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.ClientMapper;
import com.findash.repository.AccountRepository;
import com.findash.repository.ClientRepository;
import com.findash.service.ClientService;
import com.findash.util.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final AccountRepository accountRepository;

    public ClientServiceImpl(ClientRepository clientRepository, ClientMapper clientMapper,
                             AccountRepository accountRepository) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.accountRepository = accountRepository;
    }

    @Override
    public ClientResponseDTO create(UUID companyId, CreateClientRequestDTO request) {
        if (clientRepository.existsByCompanyIdAndNameIgnoreCaseAndActiveTrue(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um cliente com este nome");
        }

        String normalizedDoc = request.document() != null ? CnpjValidator.normalize(request.document()) : null;
        if (normalizedDoc != null && !normalizedDoc.isBlank()
                && clientRepository.existsByCompanyIdAndDocumentAndActiveTrue(companyId, normalizedDoc)) {
            throw new DuplicateResourceException("Ja existe um cliente com este documento");
        }

        Client client = new Client(companyId, request.name().trim());
        client.setDocument(normalizedDoc != null && !normalizedDoc.isBlank() ? normalizedDoc : null);
        client.setEmail(request.email());
        client.setPhone(request.phone());

        client = clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponseDTO> list(UUID companyId) {
        return clientRepository.findByCompanyIdAndActiveTrue(companyId)
                .stream()
                .map(clientMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDTO getById(UUID companyId, UUID clientId) {
        Client client = findOrThrow(companyId, clientId);
        return clientMapper.toResponse(client);
    }

    @Override
    public ClientResponseDTO update(UUID companyId, UUID clientId, UpdateClientRequestDTO request) {
        Client client = findOrThrow(companyId, clientId);

        if (!client.getName().equalsIgnoreCase(request.name())
                && clientRepository.existsByCompanyIdAndNameIgnoreCaseAndActiveTrue(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um cliente com este nome");
        }

        client.setName(request.name().trim());
        String normalizedDoc = request.document() != null ? CnpjValidator.normalize(request.document()) : null;
        client.setDocument(normalizedDoc != null && !normalizedDoc.isBlank() ? normalizedDoc : null);
        client.setEmail(request.email());
        client.setPhone(request.phone());

        client = clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    @Override
    public void delete(UUID companyId, UUID clientId) {
        Client client = findOrThrow(companyId, clientId);

        if (accountRepository.existsByClientId(clientId)) {
            throw new BusinessRuleException("Nao e possivel excluir este cliente pois existem lancamentos vinculados a ele.");
        }

        clientRepository.delete(client);
    }

    private Client findOrThrow(UUID companyId, UUID clientId) {
        return clientRepository.findByIdAndCompanyId(clientId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", clientId));
    }
}
