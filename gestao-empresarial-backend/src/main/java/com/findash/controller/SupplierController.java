package com.findash.controller;

import com.findash.dto.supplier.CreateSupplierRequestDTO;
import com.findash.dto.supplier.SupplierResponseDTO;
import com.findash.dto.supplier.UpdateSupplierRequestDTO;
import com.findash.security.CompanyContextHolder;
import com.findash.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<SupplierResponseDTO> create(@Valid @RequestBody CreateSupplierRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(companyId, request));
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponseDTO>> list() {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(supplierService.list(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> getById(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(supplierService.getById(companyId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(supplierService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        supplierService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
