package com.findash.controller;

import com.findash.dto.client.CreateClientRequestDTO;
import com.findash.dto.client.ClientResponseDTO;
import com.findash.dto.client.UpdateClientRequestDTO;
import com.findash.security.CompanyContextHolder;
import com.findash.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponseDTO> create(@Valid @RequestBody CreateClientRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(companyId, request));
    }

    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> list() {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(clientService.list(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(clientService.getById(companyId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClientRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(clientService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        clientService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
